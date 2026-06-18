package com.viewsonic.classswift.testing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import timber.log.Timber

/**
 * 測試用 BroadcastReceiver（僅 stag build）。
 *
 * 接收 adb broadcast：
 *   adb shell am broadcast -a com.viewsonic.classswift.TEST_SET_FOCUSABLE \
 *     --ez remove_focusable <true|false>
 *
 * remove_focusable=true  → 移除 FLAG_NOT_FOCUSABLE，讓 UIAutomator2 看見 CS elements
 * remove_focusable=false → 還原 FLAG_NOT_FOCUSABLE（正常狀態）
 */
class TestFocusableReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return

        val removeFocusable = intent.getBooleanExtra(EXTRA_REMOVE_FOCUSABLE, true)
        Timber.d("[TestFocusableReceiver] remove_focusable=$removeFocusable")

        Handler(Looper.getMainLooper()).post {
            CSWindowManager.setFocusable(focusable = removeFocusable)
        }
    }

    companion object {
        const val ACTION = "com.viewsonic.classswift.TEST_SET_FOCUSABLE"
        const val EXTRA_REMOVE_FOCUSABLE = "remove_focusable"
    }
}
