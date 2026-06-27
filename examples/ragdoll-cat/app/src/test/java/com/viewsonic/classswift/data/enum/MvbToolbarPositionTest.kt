package com.viewsonic.classswift.data.enum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * VSFT-7257: protects the `MvbToolbarPosition.fromIpcValue` contract that
 * the [com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageParser]
 * depends on.
 */
class MvbToolbarPositionTest {

    @Test
    fun `fromIpcValue parses all four canonical values`() {
        assertEquals(MvbToolbarPosition.TOP, MvbToolbarPosition.fromIpcValue("TOP"))
        assertEquals(MvbToolbarPosition.BOTTOM, MvbToolbarPosition.fromIpcValue("BOTTOM"))
        assertEquals(MvbToolbarPosition.LEFT, MvbToolbarPosition.fromIpcValue("LEFT"))
        assertEquals(MvbToolbarPosition.RIGHT, MvbToolbarPosition.fromIpcValue("RIGHT"))
    }

    @Test
    fun `fromIpcValue is case insensitive`() {
        assertEquals(MvbToolbarPosition.TOP, MvbToolbarPosition.fromIpcValue("top"))
        assertEquals(MvbToolbarPosition.RIGHT, MvbToolbarPosition.fromIpcValue("rIgHt"))
    }

    @Test
    fun `fromIpcValue trims surrounding whitespace`() {
        assertEquals(MvbToolbarPosition.LEFT, MvbToolbarPosition.fromIpcValue("  LEFT  "))
    }

    @Test
    fun `fromIpcValue returns null for unknown and edge cases`() {
        assertNull(MvbToolbarPosition.fromIpcValue("DIAGONAL"))
        assertNull(MvbToolbarPosition.fromIpcValue(""))
        assertNull(MvbToolbarPosition.fromIpcValue(null))
    }
}
