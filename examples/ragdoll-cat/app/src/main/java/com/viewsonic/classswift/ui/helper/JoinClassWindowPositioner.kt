package com.viewsonic.classswift.ui.helper

import com.viewsonic.classswift.data.enum.MvbToolbarPosition
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels

/**
 * Computes the Join Class window's screen position based on the current
 * myViewBoard main toolbar position (VSFT-7257).
 *
 * Rules (per Figma):
 * - Toolbar at TOP / BOTTOM / LEFT → window aligns to the screen's top-right corner.
 * - Toolbar at RIGHT               → window aligns to the screen's top-left corner.
 *
 * Y is identical across all four positions because the mVB toolbar never
 * overlaps the window horizontally (TOP/BOTTOM toolbars are horizontally
 * centered; LEFT/RIGHT toolbars are vertically centered on the opposite side
 * of the window), so no toolbar-height accommodation is needed.
 *
 * Intentionally **pure** (no Android dependencies). Callers convert dp→px
 * ahead of time so this object stays unit-testable on the JVM.
 */
object JoinClassWindowPositioner {

    /** Per Figma — window flush to screen's left/right edge. */
    const val HORIZONTAL_MARGIN_DP = 0

    /**
     * Fallback Y offset of the window's top edge — aligned to the mVB whiteboard
     * top edge — when mVB has not (yet) provided the precise `whiteboard_top_dp`
     * via IPC.
     *
     * Sourced from Droid_Flutter `lib/widget/title_bar.dart` `getHeight()` =
     * `_getTopBarHeight()` (40dp main title row) + `_getBottomBarHeight()`
     * (30dp document tab row) = 70dp on dpr 2.0 (1920×1080 IFP).
     *
     * For cross-device precision, mVB is expected to send `whiteboard_top_dp`
     * in `MessageToolbarPositionChanged.payload`; this constant is only the
     * fallback when that field is missing (older mVB builds).
     */
    const val WHITEBOARD_TOP_OFFSET_DP_FALLBACK = 70

    /**
     * @param toolbarPosition         Latest known position of the mVB main toolbar.
     * @param screenWidth             Pixel width of the active display.
     * @param windowSize              Measured pixel size of the window;
     *                                **only `width` is used** (height is left in
     *                                the type for callsite ergonomics).
     * @param horizontalMarginPx      Pixel margin between window and the nearest screen edge.
     * @param whiteboardTopOffsetPx   Pixel Y position of the window's top edge
     *                                (= mVB whiteboard top edge); see
     *                                [WHITEBOARD_TOP_OFFSET_DP_FALLBACK].
     */
    fun calculate(
        toolbarPosition: MvbToolbarPosition,
        screenWidth: Int,
        windowSize: SizeInPixels,
        horizontalMarginPx: Int,
        whiteboardTopOffsetPx: Int
    ): Location {
        val rightAlignedX = (screenWidth - windowSize.width - horizontalMarginPx)
            .coerceAtLeast(horizontalMarginPx)

        return when (toolbarPosition) {
            MvbToolbarPosition.TOP,
            MvbToolbarPosition.BOTTOM,
            MvbToolbarPosition.LEFT -> Location(
                coordinateX = rightAlignedX,
                coordinateY = whiteboardTopOffsetPx
            )
            MvbToolbarPosition.RIGHT -> Location(
                coordinateX = horizontalMarginPx,
                coordinateY = whiteboardTopOffsetPx
            )
        }
    }
}
