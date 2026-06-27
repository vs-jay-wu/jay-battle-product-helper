package com.viewsonic.classswift.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.Locale

object DeviceInfoUtils {
    @SuppressLint("DefaultLocale")
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val model = Build.MODEL.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        return if (model.startsWith(manufacturer)) model else "$manufacturer - $model"
    }

    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
}