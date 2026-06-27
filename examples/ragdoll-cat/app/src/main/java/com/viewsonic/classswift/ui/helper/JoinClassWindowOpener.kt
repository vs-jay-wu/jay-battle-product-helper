package com.viewsonic.classswift.ui.helper

import com.viewsonic.classswift.manager.MvbToolbarStateManager
import com.viewsonic.classswift.ui.window.JoinClassWindow
import com.viewsonic.classswift.utils.DisplayUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import org.koin.java.KoinJavaComponent.get

/**
 * Opens the Join Class window at the correct initial location based on the
 * current mVB toolbar position (VSFT-7257).
 *
 * Why this helper exists:
 * `CSWindowManager.createWindow(window, gravity)` triggers
 * `IWindow.onViewCreated()` **before** the window is registered in `windowMap`.
 * Inside `onViewCreated`, `csWindowManager.getWindow(tag)` therefore returns
 * `null` and any reposition attempt is silently dropped. The robust fix is to
 * compute the initial `Location` **before** calling `createWindow`, which is
 * what this helper does.
 *
 * Falls back to [Gravity.CENTER] if mVB has not pushed a toolbar position yet
 * (e.g. mVB is not currently bound).
 */
object JoinClassWindowOpener {

    fun open(window: JoinClassWindow): Boolean {
        val toolbarStateManager: MvbToolbarStateManager =
            get(MvbToolbarStateManager::class.java)
        val currentPosition = toolbarStateManager.position.value
            ?: return CSWindowManager.createWindow(window, Gravity.CENTER)

        val (screenWidth, _) = DisplayUtils.getScreenSize()
        // VSFT-7257: prefer dynamic whiteboard top edge reported by mVB;
        // fall back to the hardcoded default for older mVB builds.
        val whiteboardTopOffsetPx = toolbarStateManager.whiteboardTopDp.value
            ?.toFloat()?.dpToPx()?.toInt()
            ?: JoinClassWindowPositioner.WHITEBOARD_TOP_OFFSET_DP_FALLBACK.dpToPx().toInt()
        val location = JoinClassWindowPositioner.calculate(
            toolbarPosition = currentPosition,
            screenWidth = screenWidth,
            windowSize = window.getCurrentSize(),
            horizontalMarginPx = JoinClassWindowPositioner.HORIZONTAL_MARGIN_DP.dpToPx().toInt(),
            whiteboardTopOffsetPx = whiteboardTopOffsetPx
        )
        return CSWindowManager.createWindow(window, location)
    }
}
