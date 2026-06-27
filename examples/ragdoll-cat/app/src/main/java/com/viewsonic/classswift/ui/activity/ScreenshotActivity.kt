package com.viewsonic.classswift.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import android.view.Display
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.viewsonic.classswift.api.VSApiGateway
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.service.ClassSwiftService
import com.viewsonic.classswift.ui.customview.CropImageView
import com.viewsonic.classswift.ui.window.CropImageWindow
import com.viewsonic.classswift.ui.window.MvbCropImageWindow
import com.viewsonic.classswift.utils.DisplayUtils
import com.viewsonic.classswift.utils.ImageUtil
import com.viewsonic.classswift.utils.extension.doRecycle
import com.viewsonic.classswift.utils.extension.extraOrDefault
import com.viewsonic.classswift.utils.extension.extraOrNull
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Screenshot capture flow with two paths:
 *
 *   - VSApi path (AOSP IFP) — preferred. No MediaProjection consent required;
 *     `VSPictureManager.screenshot` returns the current screen as a Bitmap.
 *   - MediaProjection path (fallback) — same flow as the legacy EDLA build:
 *     request user consent, set up a `VirtualDisplay` + `ImageReader`, capture
 *     a frame at crop time.
 *
 * Path selection happens in `onCreate` via a cached `VSApiGateway` capability
 * check. If VSApi is available we skip the MediaProjection setup entirely
 * (better UX — no consent dialog).
 */
class ScreenshotActivity : AppCompatActivity() {
    enum class Result {
        NONE,
        SUCCESSFUL,
        CANCELED,
        FAILED
    }

    private val mediaProjectionManager: MediaProjectionManager by inject()
    private val csWindowManager: CSWindowManager by inject()
    private val vsApiGateway: VSApiGateway by inject()
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider by inject()

    private val screenshotSavedPath: String by extraOrDefault(ARG_SCREENSHOT_SAVED_PATH, "")
    private val resultReceiver: ResultReceiver? by extraOrNull(ARG_RESULT_RECEIVER)

    private var classSwiftService: ClassSwiftService? = null
    private var isResultSent = false
    private var isServiceBound: Boolean = false

    // MediaProjection-only state (initialized only on the fallback path).
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var mWidth = 0
    private var mHeight = 0
    private var density = 0
    private var display: Display? = null
    private var cropProcessingJob: Job? = null

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Timber.d("[B][MediaProjection.Callback - onStop] : ")
            releaseMediaProjectionResources()
        }
    }

    private val mediaProjectionResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Timber.d("[B][mediaProjectionResult] : result = $result")
            when (result.resultCode) {
                RESULT_OK -> {
                    result.data?.let { dataIntent ->
                        try {
                            classSwiftService?.startMediaProjectionForegroundServiceCompat()
                            getMediaProjection(result.resultCode, dataIntent)
                            lifecycleScope.launch {
                                startCroppingViaMediaProjection()
                            }
                            return@registerForActivityResult
                        } catch (e: Exception) {
                            Timber.d("[B][mediaProjectionResult] : error = $e")
                        }
                    }
                }
                else -> {
                    sendResult(Result.CANCELED)
                }
            }
            sendResult(Result.FAILED)
        }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as ClassSwiftService.LocalBinder
            classSwiftService = binder.getService()
            Timber.d("[B][onServiceConnected] : ")
            isServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.d("[B][onServiceDisconnected] : ")
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("[B][onCreate] : screenshotSavedPath = $screenshotSavedPath")

        lifecycleScope.launch {
            val useVSApi = vsApiGateway.canCaptureScreen()
            Timber.d("[B][onCreate] : useVSApi = $useVSApi")

            Timber.d("[B][onCreate] : waitUntilServiceBound start")
            withContext(Dispatchers.IO) {
                waitUntilServiceBound()
            }
            if (useVSApi) {
                showCropWindowForCurrentMode(useVSApi = true)
            } else {
                if (isNeedToCheckMediaProjectionPermission()) {
                    Timber.d("[B][onCreate] : Need to request MediaProjection permission")
                    requestMediaProjectionPermission()
                } else {
                    Timber.d("[B][onCreate] : Don't need to request permission, reuse cached MediaProjection")
                    classSwiftService?.let { service ->
                        service.mediaProjectionData?.let { (resultCode, dataIntent) ->
                            service.startMediaProjectionForegroundServiceCompat()
                            getMediaProjection(resultCode, dataIntent)
                            startCroppingViaMediaProjection()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("[B][onDestroy] : isResultSent = $isResultSent")
        if (!isResultSent) {
            mediaProjection?.stop()
            sendResult(Result.CANCELED)
        }
    }

    private fun getMediaProjection(resultCode: Int, dataIntent: Intent) {
        Timber.d("[B][mediaProjectionResult] : resultCode = $resultCode, dataIntent = $dataIntent")
        mediaProjectionManager.getMediaProjection(resultCode, dataIntent)?.let { mp ->
            Timber.d("[B][mediaProjectionResult] : mediaProjection got")
            classSwiftService?.let { service ->
                service.mediaProjectionData = resultCode to dataIntent
            }
            this.mediaProjection = mp
        }
    }

    private fun sendResult(result: Result, imageUri: Uri? = null) {
        Timber.d("[B][sendResult] : result = $result, imageUri = $imageUri")
        val bundleData = bundleOf(
            BUNDLE_KEY_RESULT to result
        )
        imageUri?.let {
            bundleData.putParcelable(BUNDLE_KEY_IMAGE_URI, imageUri)
        }
        resultReceiver?.send(RESULT_OK, bundleData)
        classSwiftService?.startForegroundWithDefault()
        when (myViewBoardConnectionStateProvider.isBound()) {
            true -> csWindowManager.removeWindow(WindowTag.MVB_CROP_IMAGE)
            false -> csWindowManager.removeWindow(WindowTag.CROP_IMAGE)
        }
        isResultSent = true
        finish()
    }

    private suspend fun waitUntilServiceBound() {
        bindService(ClassSwiftService.getStartIntent(), connection, BIND_AUTO_CREATE)
        while (!isServiceBound) {
            Timber.d("[B][waitUntilServiceBound] : Wait for service connection is established.")
            delay(SERVICE_CONNECTION_CHECK_PERIOD_MILLIS)
        }
    }

    // ---------- MediaProjection fallback path ----------

    private fun isNeedToCheckMediaProjectionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            true
        } else {
            classSwiftService?.mediaProjectionData == null
        }
    }

    private fun requestMediaProjectionPermission() {
        if (!isNeedToCheckMediaProjectionPermission()) {
            return
        }
        val screenCaptureIntent = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+ requires choosing single-app vs full-screen capture.
                // createConfigForDefaultDisplay pins the choice.
                mediaProjectionManager.createScreenCaptureIntent(
                    MediaProjectionConfig.createConfigForDefaultDisplay()
                )
            }
            else -> {
                mediaProjectionManager.createScreenCaptureIntent()
            }
        }
        mediaProjectionResult.launch(screenCaptureIntent)
    }

    private suspend fun startCroppingViaMediaProjection() {
        Timber.d("[B][startCroppingViaMediaProjection] : ")
        val mp = mediaProjection
        if (mp == null) {
            Timber.e("[B][startCroppingViaMediaProjection] : mediaProjection is null")
            sendResult(Result.FAILED)
            return
        }
        mp.registerCallback(mediaProjectionCallback, null)

        density = Resources.getSystem().displayMetrics.densityDpi
        display = DisplayUtils.getDisplay(applicationContext)
        mWidth = DisplayUtils.getScreenSize().first
        mHeight = DisplayUtils.getScreenSize().second
        if (display?.isValid != true || mWidth == 0 || mHeight == 0 || density == 0) {
            Timber.e("Display variables initialization failed")
            sendResult(Result.FAILED)
            return
        }

        val isVirtualDisplayReady = waitUntilVirtualDisplayReady(mp)
        if (!isVirtualDisplayReady) {
            Timber.e("VirtualDisplay initialization failed")
            sendResult(Result.FAILED)
            return
        }

        showCropWindowForCurrentMode(useVSApi = false)
    }

    private suspend fun waitUntilVirtualDisplayReady(mp: MediaProjection): Boolean = suspendCoroutine { continuation ->
        try {
            val reader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 1)
            imageReader = reader

            virtualDisplay = mp.createVirtualDisplay(
                "ScreenCapture",
                mWidth,
                mHeight,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                object : VirtualDisplay.Callback() {
                    override fun onStopped() {
                        Timber.d("[B][VirtualDisplay- onStopped] : ")
                        virtualDisplay?.release()
                    }

                    override fun onPaused() {
                        Timber.d("[B][VirtualDisplay- onPaused] : ")
                    }

                    override fun onResumed() {
                        Timber.d("[B][VirtualDisplay- onResumed] : ")
                        continuation.resume(true)
                    }
                },
                null
            )
        } catch (e: Exception) {
            Timber.d("[B][waitUntilVirtualDisplayReady] : e = $e")
            e.printStackTrace()
            continuation.resume(false)
        }
    }

    private fun releaseMediaProjectionResources() {
        virtualDisplay?.release()
        imageReader?.close()
        virtualDisplay = null
        imageReader = null
    }

    // ---------- Crop window (shared between both paths) ----------

    private fun showCropWindowForCurrentMode(useVSApi: Boolean) {
        when (myViewBoardConnectionStateProvider.isBound()) {
            true -> showMvbCropImageWindow(useVSApi)
            false -> showCropImageWindow(useVSApi)
        }
    }

    private fun showMvbCropImageWindow(useVSApi: Boolean) {
        csWindowManager.createWindow(
            MvbCropImageWindow(applicationContext), Gravity.CENTER, isOutOfScreen = false
        )
        val mvbCropImageWindow = csWindowManager.getWindow(WindowTag.MVB_CROP_IMAGE)?.customWindow as MvbCropImageWindow
        mvbCropImageWindow.setCropImagePath(screenshotSavedPath)
        mvbCropImageWindow.setOnCropRangeListener(makeCropRangeListener(WindowTag.MVB_CROP_IMAGE, useVSApi))
        mvbCropImageWindow.cancelButtonListener = {
            Timber.d("[B][showMvbCropImageWindow] : Cancel")
            mediaProjection?.stop()
            sendResult(Result.CANCELED)
        }
    }

    private fun showCropImageWindow(useVSApi: Boolean) {
        csWindowManager.createWindow(
            CropImageWindow(applicationContext), Gravity.CENTER, isOutOfScreen = false
        )
        val cropWindow = csWindowManager.getWindow(WindowTag.CROP_IMAGE)?.customWindow as CropImageWindow
        cropWindow.setCropImagePath(screenshotSavedPath)
        cropWindow.setOnCropRangeListener(makeCropRangeListener(WindowTag.CROP_IMAGE, useVSApi))
        cropWindow.cancelButtonListener = {
            Timber.d("[B][showCropImageWindow] : Cancel")
            mediaProjection?.stop()
            sendResult(Result.CANCELED)
        }
    }

    private fun makeCropRangeListener(
        windowTag: WindowTag,
        useVSApi: Boolean
    ) = object : CropImageView.OnCropRangeListener {
        override fun cropRange(x: Int, y: Int, width: Int, height: Int) {
            Timber.d("[B][cropRange] : [${this@ScreenshotActivity}] width = $width, height = $height")
            if (cropProcessingJob?.isActive == true) {
                Timber.d("[B][cropRange] : Is processing")
                return
            }
            cropProcessingJob = lifecycleScope.launch {
                try {
                    if (useVSApi) {
                        captureAndCropViaVSApi(windowTag, x, y, width, height)
                    } else {
                        captureAndCropViaMediaProjection(windowTag, x, y, width, height)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "[B][cropRange] : exception")
                }
            }
        }
    }

    private suspend fun captureAndCropViaVSApi(
        windowTag: WindowTag,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        // Binder roundtrip + full-screen bitmap copy/crop must not block the main thread.
        val bitmap = withContext(Dispatchers.IO) { vsApiGateway.captureScreen() }
        if (bitmap == null) {
            Timber.w("[B][captureAndCropViaVSApi] : VSApi captureScreen returned null")
            csWindowManager.removeWindow(windowTag)
            sendResult(Result.FAILED)
            return
        }
        val isCropSuccessful = withContext(Dispatchers.IO) {
            ImageUtil.cropImageAndResizeToStorage(
                screenshotSavedPath, bitmap, x, y, width, height
            )
        }
        bitmap.doRecycle()
        csWindowManager.removeWindow(windowTag)
        if (isCropSuccessful) {
            sendResult(Result.SUCCESSFUL, screenshotSavedPath.toUri())
        } else {
            sendResult(Result.FAILED)
        }
    }

    private suspend fun captureAndCropViaMediaProjection(
        windowTag: WindowTag,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        val reader = imageReader
        if (reader == null) {
            Timber.e("[B][captureAndCropViaMediaProjection] : imageReader is null")
            csWindowManager.removeWindow(windowTag)
            mediaProjection?.stop()
            sendResult(Result.FAILED)
            return
        }
        // Avoid races where the crop mask / tap effect hasn't fully cleared.
        withContext(Dispatchers.IO) { delay(230) }

        val image = reader.acquireNextImage() ?: run {
            csWindowManager.removeWindow(windowTag)
            mediaProjection?.stop()
            sendResult(Result.FAILED)
            return
        }
        val plane = image.planes[0]
        val buffer = plane.buffer
        val bitmap: Bitmap = createBitmap(image.width, image.height)
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        val isCropSuccessful = ImageUtil.cropImageAndResizeToStorage(
            screenshotSavedPath, bitmap, x, y, width, height
        )
        csWindowManager.removeWindow(windowTag)
        mediaProjection?.stop()
        bitmap.doRecycle()
        image.close()
        if (isCropSuccessful) {
            sendResult(Result.SUCCESSFUL, screenshotSavedPath.toUri())
        } else {
            sendResult(Result.FAILED)
        }
    }

    companion object {
        // Arguments for activity
        private const val ARG_SCREENSHOT_SAVED_PATH = "arg_screenshot_saved_path"
        private const val ARG_RESULT_RECEIVER = "arg_result_receiver"

        // Return Data
        const val BUNDLE_KEY_RESULT = "bundle_key_result"
        const val BUNDLE_KEY_IMAGE_URI = "bundle_key_image_uri"

        private const val DEFAULT_SCREENSHOT_FILE_NAME = "temp_screenshot.jpg"
        private const val SERVICE_CONNECTION_CHECK_PERIOD_MILLIS = 50L

        fun createIntent(
            context: Context,
            resultReceiver: ResultReceiver,
            screenshotSavedPath: String = context.cacheDir.absolutePath + File.separator + DEFAULT_SCREENSHOT_FILE_NAME,
        ): Intent {
            return Intent(context, ScreenshotActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(ARG_SCREENSHOT_SAVED_PATH, screenshotSavedPath)
                putExtra(ARG_RESULT_RECEIVER, resultReceiver)
            }
        }
    }
}
