package com.viewsonic.classswift.ui.widgetmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordMarkWidgetModelTest {

    // ── empty list ────────────────────────────────────────────────────────────

    @Test
    fun `coerceIndexOrNull returns null when list is empty`() {
        val result = RecordMarkWidgetModel.coerceIndexOrNull(listSize = 0, index = 0)

        assertNull(result)
    }

    @Test
    fun `coerceIndexOrNull returns null when list is empty even with large index`() {
        val result = RecordMarkWidgetModel.coerceIndexOrNull(listSize = 0, index = 99)

        assertNull(result)
    }

    // ── within bounds ─────────────────────────────────────────────────────────

    @Test
    fun `coerceIndexOrNull returns index when within bounds`() {
        val result = RecordMarkWidgetModel.coerceIndexOrNull(listSize = 3, index = 1)

        assertEquals(1, result)
    }

    // ── out of bounds ─────────────────────────────────────────────────────────

    @Test
    fun `coerceIndexOrNull coerces negative index to 0`() {
        val result = RecordMarkWidgetModel.coerceIndexOrNull(listSize = 3, index = -5)

        assertEquals(0, result)
    }

    @Test
    fun `coerceIndexOrNull coerces overflow index to lastIndex`() {
        val result = RecordMarkWidgetModel.coerceIndexOrNull(listSize = 3, index = 99)

        assertEquals(2, result)
    }

    // ── single item list ──────────────────────────────────────────────────────

    @Test
    fun `coerceIndexOrNull returns 0 when single item list and index 0`() {
        val result = RecordMarkWidgetModel.coerceIndexOrNull(listSize = 1, index = 0)

        assertEquals(0, result)
    }

    @Test
    fun `coerceIndexOrNull returns lastIndex when single item and overflow`() {
        val result = RecordMarkWidgetModel.coerceIndexOrNull(listSize = 1, index = 5)

        assertEquals(0, result)
    }
}
