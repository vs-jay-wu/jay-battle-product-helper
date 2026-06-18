package com.viewsonic.classswift.api

import android.content.Context
import android.graphics.Bitmap
import android.os.RemoteException
import com.viewsonic.vsapicompat.VSContext
import com.viewsonic.vsapicompat.VSPictureManager
import com.viewsonic.vsapicompat.VSServiceManagerCompat
import com.viewsonic.vsapicompat.VSSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Wraps every VSApi entry point with a narrow try/catch so non-IFP devices
 * (or IFPs where VSApi is unavailable) fall back gracefully instead of crashing.
 *
 * Returning `null` (or `false` from `installApp`) signals to the caller that
 * VSApi did not succeed; the caller is responsible for providing a fallback path
 * (e.g. PackageInstaller, MediaProjection, fingerprint-based model name lookup).
 *
 * The catch list is intentionally narrow — `Throwable` is NOT swallowed, so
 * legitimate bugs still surface.
 */
class VSApiGateway(private val applicationContext: Context) {
    @Volatile
    private var screenshotCapability: Boolean? = null
    private val screenshotCapabilityLock = Any()

    /**
     * Install an APK silently via VSApi. Requires `MANAGE_USERS` permission
     * (granted on AOSP IFP via platform signature; ungranted elsewhere).
     *
     * @return `true` if VSApi reported success, `false` if VSApi reported failure
     *         but did not throw, `null` if VSApi was unavailable / threw a
     *         catchable error.
     */
    fun installApp(apkPath: String): Boolean? = runWithVSApi("installApp") {
        val service = VSServiceManagerCompat.getService(
            applicationContext,
            VSContext.VS_SYSTEM_SERVICE
        ) as VSSystemManager
        service.installApp(apkPath)
    }

    /**
     * Read the VSApi-provided precise model name. On non-IFP devices this returns null.
     */
    fun getPreciseModelName(): String? = runWithVSApi("getPreciseModelName") {
        val service = VSServiceManagerCompat.getService(
            applicationContext,
            VSContext.VS_SYSTEM_SERVICE
        ) as VSSystemManager
        service.preciseModelName.takeIf { it.isNotEmpty() }
    }

    /**
     * Capture the current screen via VSApi (no MediaProjection consent required).
     * Returns null if VSApi is unavailable; caller should fall back to MediaProjection.
     */
    fun captureScreen(): Bitmap? = runWithVSApi("captureScreen") {
        val service = VSServiceManagerCompat.getService(
            applicationContext,
            VSContext.VS_PICTURE_SERVICE
        ) as VSPictureManager
        service.screenshot
    }

    /**
     * Probe whether VSApi screen capture actually works on this process.
     *
     * Some IFP models expose `VS_PICTURE_SERVICE` but return no screenshot. The
     * full-screen probe is expensive, so cache the result for the process lifetime.
     */
    suspend fun canCaptureScreen(): Boolean {
        screenshotCapability?.let { return it }
        return withContext(Dispatchers.IO) {
            resolveScreenshotCapability()
        }
    }

    /**
     * Cheap probe: does the VSApi picture service exist on this device?
     */
    fun isPictureServiceAvailable(): Boolean = runWithVSApi("isPictureServiceAvailable") {
        VSServiceManagerCompat.getService(
            applicationContext,
            VSContext.VS_PICTURE_SERVICE
        ) != null
    } ?: false

    private fun resolveScreenshotCapability(): Boolean {
        screenshotCapability?.let { return it }
        return synchronized(screenshotCapabilityLock) {
            screenshotCapability?.let { return@synchronized it }
            val canCapture = isPictureServiceAvailable() && probeCaptureScreen()
            screenshotCapability = canCapture
            canCapture
        }
    }

    private fun probeCaptureScreen(): Boolean {
        val probeBitmap = captureScreen() ?: run {
            Timber.tag(VS_TAG).d("[canCaptureScreen] captureScreen probe returned null")
            return false
        }
        Timber.tag(VS_TAG).d("[canCaptureScreen] captureScreen probe succeeded")
        probeBitmap.recycle()
        return true
    }

    /**
     * Wraps the explicit catch list once for every call.
     * Any other throwable propagates so real bugs aren't silently swallowed.
     */
    private inline fun <T> runWithVSApi(tag: String, block: () -> T): T? {
        return try {
            block()
        } catch (e: NoClassDefFoundError) {
            Timber.tag(VS_TAG).w("[$tag] VSApi unavailable (NoClassDefFoundError): ${e.message}")
            null
        } catch (e: NoSuchMethodError) {
            Timber.tag(VS_TAG).w("[$tag] VSApi method missing (NoSuchMethodError): ${e.message}")
            null
        } catch (e: RemoteException) {
            Timber.tag(VS_TAG).w(e, "[$tag] VSApi binder failed")
            null
        } catch (e: SecurityException) {
            Timber.tag(VS_TAG).w(e, "[$tag] VSApi permission denied")
            null
        } catch (e: LinkageError) {
            Timber.tag(VS_TAG).w(e, "[$tag] VSApi link error")
            null
        } catch (e: OutOfMemoryError) {
            // 4K IFP screenshots can be ~32 MB; let callers fall back instead of crashing.
            Timber.tag(VS_TAG).w(e, "[$tag] VSApi out of memory")
            null
        }
    }

    companion object {
        private const val VS_TAG = "VSApiGateway"
    }
}
