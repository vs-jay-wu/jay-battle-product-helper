package com.viewsonic.classswift.utils

import android.os.Build
import com.viewsonic.classswift.api.VSApiGateway
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

object SystemInfoUtils {
    private val vsApiGateway: VSApiGateway by inject(VSApiGateway::class.java)

    /**
     * Resolve the device's model name with VSApi-first fallback chain:
     *   1. VSApi `preciseModelName` (works on AOSP IFP).
     *   2. `Build.FINGERPRINT` second segment if it starts with "IFP" (AOSP IFP fallback).
     *   3. `Build.MODEL` (everything else).
     */
    fun getModelName(): String {
        val vsModelName = vsApiGateway.getPreciseModelName().orEmpty()
        // FINGERPRINT example: ViewSonic/IFP8652-2/IFP8652-2:13/TQ1A.230205.002/20241113:user/release-keys
        val modelNameFromFingerprint = Build.FINGERPRINT.split("/").getOrNull(1)
            ?.takeIf { name -> name.startsWith("IFP") }
            .orEmpty()

        Timber.d("[B][getModelName] : VS Model Name = $vsModelName")
        Timber.d("[B][getModelName] : Build.FINGERPRINT = ${Build.FINGERPRINT}")
        Timber.d("[B][getModelName] : Model Name from FINGERPRINT = $modelNameFromFingerprint")
        Timber.d("[B][getModelName] : Build.MODEL = ${Build.MODEL}")

        return when {
            vsModelName.isNotEmpty() -> vsModelName
            modelNameFromFingerprint.isNotEmpty() -> modelNameFromFingerprint
            else -> Build.MODEL
        }
    }
}
