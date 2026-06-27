package com.viewsonic.classswift.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.core.view.WindowInsetsCompat
import org.koin.java.KoinJavaComponent.inject
import kotlin.math.sqrt

object DisplayUtils {
    private val windowManager: WindowManager by inject(WindowManager::class.java)

    /**
     * return: <Width, Height, Diagonal> in inches
     */
    fun getDisplayInInches(): Triple<Float, Float, Float> {
        val deviceDensity = getDensity()
        val deviceDpi = deviceDensity * 160f
        val deviceSize = getScreenSize()
        val widthInInches = deviceSize.first.toFloat() / deviceDpi
        val heightInInches = deviceSize.second.toFloat() / deviceDpi
        val diagonalInInches = sqrt(widthInInches * widthInInches + heightInInches * heightInInches)
        return Triple(widthInInches, heightInInches, diagonalInInches)
    }

    fun getDensity(): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            windowManager.currentWindowMetrics.density
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.density
        }
    }

    fun getDisplay(context: Context): Display =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val displayManager = context.getSystemService(DisplayManager::class.java)
            displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
            windowManager.defaultDisplay
        }

    fun getScreenSize(
        isIgnoreNavigationBars: Boolean = false,
        isIgnoreStatusBars: Boolean = false
    ): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above
            windowManager.currentWindowMetrics.bounds
            val metrics = windowManager.currentWindowMetrics
            val windowInsets = metrics.windowInsets
            var typeMask = 0
            if (isIgnoreNavigationBars) {
                typeMask = typeMask or WindowInsets.Type.navigationBars()
            }
            if (isIgnoreStatusBars) {
                typeMask = typeMask or WindowInsets.Type.statusBars()
            }
            val insets = windowInsets.getInsetsIgnoringVisibility(typeMask)

            val insetsWidth = insets.right + insets.left
            val insetsHeight = insets.top + insets.bottom

            // Real screen size excluding system decorations
            val width = metrics.bounds.width() - insetsWidth
            val height = metrics.bounds.height() - insetsHeight
            width to height
        } else {
            // Below Android 11
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels to displayMetrics.heightPixels
        }
    }

    fun getStatusBarHeight(context: Context, isSkippedIfStatusBarInvisibleAboveAndroid11: Boolean = false): Int {
        var statusBarHeight = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
            val windowInsets: WindowInsets = windowMetrics.windowInsets
            val isStatusBarVisible = windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())
            if (!isSkippedIfStatusBarInvisibleAboveAndroid11 || isStatusBarVisible) {
                windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).let {
                    statusBarHeight = it.top
                }
            }
        } else {
            // 對於較舊的 Android 版本，使用 Resources 方法
            val resourceId =
                context.applicationContext.resources.getIdentifier(
                    "status_bar_height",
                    "dimen",
                    "android"
                )
            if (resourceId > 0) {
                statusBarHeight =
                    context.applicationContext.resources.getDimensionPixelSize(resourceId)
            }
        }
        return statusBarHeight
    }

    fun getNavigationBarHeight(context: Context): Int {
        var navigationBarHeight = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
            val windowInsets: WindowInsets = windowMetrics.windowInsets

            windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).let {
                navigationBarHeight = it.bottom
            }
        } else {
            // 對於較舊的 Android 版本，使用 Resources 方法
            val navResourceId =
                context.applicationContext.resources.getIdentifier(
                    "navigation_bar_height",
                    "dimen",
                    "android"
                )
            if (navResourceId > 0) {
                navigationBarHeight =
                    context.applicationContext.resources.getDimensionPixelSize(navResourceId)
            }
        }
        return navigationBarHeight
    }

    /**
     * return <locationX, locationY>
     */
    fun getViewTopCenterLocationOnDisplay(view: View): Pair<Int, Int> {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        location[0] += view.measuredWidth / 2
        return location[0] to location[1]
    }

}