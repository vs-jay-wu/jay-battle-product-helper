package com.viewsonic.classswift.manager

import com.viewsonic.classswift.data.enum.MvbToolbarPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * VSFT-7257: contract test for [MvbToolbarStateManager] — focuses on the
 * "null whiteboardTopDp keeps previous value" backwards-compat behaviour
 * that observers rely on when an older mVB sends a position update without
 * the new optional field.
 */
class MvbToolbarStateManagerTest {

    @Test
    fun `initial state has null position and null whiteboardTopDp`() {
        val m = MvbToolbarStateManager()

        assertNull(m.position.value)
        assertNull(m.whiteboardTopDp.value)
    }

    @Test
    fun `update without whiteboardTopDp leaves the field untouched`() {
        val m = MvbToolbarStateManager()

        m.update(newPosition = MvbToolbarPosition.TOP)

        assertEquals(MvbToolbarPosition.TOP, m.position.value)
        assertNull("whiteboardTopDp should stay null when not provided", m.whiteboardTopDp.value)
    }

    @Test
    fun `update with whiteboardTopDp records both fields`() {
        val m = MvbToolbarStateManager()

        m.update(newPosition = MvbToolbarPosition.BOTTOM, newWhiteboardTopDp = 70.5)

        assertEquals(MvbToolbarPosition.BOTTOM, m.position.value)
        assertEquals(70.5, m.whiteboardTopDp.value!!, 0.001)
    }

    @Test
    fun `subsequent update with null whiteboardTopDp keeps last known value`() {
        val m = MvbToolbarStateManager()
        m.update(newPosition = MvbToolbarPosition.TOP, newWhiteboardTopDp = 70.0)

        // Older mVB or follow-up message without the optional field.
        m.update(newPosition = MvbToolbarPosition.BOTTOM, newWhiteboardTopDp = null)

        assertEquals(MvbToolbarPosition.BOTTOM, m.position.value)
        assertEquals(
            "whiteboardTopDp must not regress to null when caller omits it",
            70.0, m.whiteboardTopDp.value!!, 0.001
        )
    }

    @Test
    fun `subsequent update with new whiteboardTopDp overrides the previous one`() {
        val m = MvbToolbarStateManager()
        m.update(newPosition = MvbToolbarPosition.TOP, newWhiteboardTopDp = 70.0)

        m.update(newPosition = MvbToolbarPosition.RIGHT, newWhiteboardTopDp = 100.5)

        assertEquals(MvbToolbarPosition.RIGHT, m.position.value)
        assertEquals(100.5, m.whiteboardTopDp.value!!, 0.001)
    }
}
