package com.viewsonic.classswift.ui.helper

import com.viewsonic.classswift.data.enum.MvbToolbarPosition
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * VSFT-7257: pure positional logic — exercises the positioner without any
 * Android dependencies. dp→px translation is the caller's responsibility, so
 * each test supplies px values directly.
 */
class JoinClassWindowPositionerTest {

    private val screenWidth = 1920
    private val windowSize = SizeInPixels(width = 600, height = 800)
    private val horizontalMarginPx = 0
    private val whiteboardTopOffsetPx = 24

    @Test
    fun `TOP BOTTOM LEFT all align to top-right with identical Location`() {
        val top = positionAt(MvbToolbarPosition.TOP)
        val bottom = positionAt(MvbToolbarPosition.BOTTOM)
        val left = positionAt(MvbToolbarPosition.LEFT)

        assertEquals(bottom, top)
        assertEquals(bottom, left)
        assertTrue(
            "TOP/BOTTOM/LEFT should land on the right half",
            top.coordinateX > screenWidth / 2
        )
        assertEquals(whiteboardTopOffsetPx, top.coordinateY)
    }

    @Test
    fun `BOTTOM X equals screenWidth minus window width minus horizontal margin`() {
        val location = positionAt(MvbToolbarPosition.BOTTOM)

        assertEquals(
            screenWidth - windowSize.width - horizontalMarginPx,
            location.coordinateX
        )
    }

    @Test
    fun `RIGHT positions window on left at horizontal margin`() {
        val location = positionAt(MvbToolbarPosition.RIGHT)

        assertEquals(horizontalMarginPx, location.coordinateX)
        assertEquals(whiteboardTopOffsetPx, location.coordinateY)
    }

    @Test
    fun `all four positions share the same Y`() {
        val top = positionAt(MvbToolbarPosition.TOP).coordinateY
        val bottom = positionAt(MvbToolbarPosition.BOTTOM).coordinateY
        val left = positionAt(MvbToolbarPosition.LEFT).coordinateY
        val right = positionAt(MvbToolbarPosition.RIGHT).coordinateY

        assertEquals(whiteboardTopOffsetPx, top)
        assertEquals(whiteboardTopOffsetPx, bottom)
        assertEquals(whiteboardTopOffsetPx, left)
        assertEquals(whiteboardTopOffsetPx, right)
    }

    @Test
    fun `narrow screen falls back to minimum left margin without negative X`() {
        // window wider than the screen → right-aligned X would go negative,
        // positioner must clamp to a positive margin.
        val narrow = JoinClassWindowPositioner.calculate(
            toolbarPosition = MvbToolbarPosition.BOTTOM,
            screenWidth = 400,
            windowSize = SizeInPixels(width = 800, height = 800),
            horizontalMarginPx = horizontalMarginPx,
            whiteboardTopOffsetPx = whiteboardTopOffsetPx
        )

        assertEquals(
            "X must clamp to horizontalMargin (not negative)",
            horizontalMarginPx,
            narrow.coordinateX
        )
    }

    private fun positionAt(position: MvbToolbarPosition) =
        JoinClassWindowPositioner.calculate(
            toolbarPosition = position,
            screenWidth = screenWidth,
            windowSize = windowSize,
            horizontalMarginPx = horizontalMarginPx,
            whiteboardTopOffsetPx = whiteboardTopOffsetPx
        )
}
