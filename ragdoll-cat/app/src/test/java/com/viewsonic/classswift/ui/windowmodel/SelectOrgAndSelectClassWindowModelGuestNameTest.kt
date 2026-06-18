package com.viewsonic.classswift.ui.windowmodel

import com.viewsonic.classswift.ui.windowmodel.SelectOrgAndSelectClassWindowModel.Companion.buildGuestClassName
import com.viewsonic.classswift.ui.windowmodel.SelectOrgAndSelectClassWindowModel.Companion.nextGuestClassCounter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class SelectOrgAndSelectClassWindowModelGuestNameTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `buildGuestClassName returns correct format for given counter`() {
        val now = LocalDateTime.of(2026, 4, 28, 9, 0)
        assertEquals("Apr 28, 2026 (AM) #001", buildGuestClassName(now, 1))
        assertEquals("Apr 28, 2026 (AM) #012", buildGuestClassName(now, 12))
        assertEquals("Apr 28, 2026 (AM) #100", buildGuestClassName(now, 100))
    }

    @Test
    fun `nextGuestClassCounter returns 1 when no existing names match block`() {
        val now = LocalDateTime.of(2026, 4, 28, 9, 0)
        assertEquals(1, nextGuestClassCounter(emptyList(), now))
        assertEquals(1, nextGuestClassCounter(listOf("Science 101"), now))
    }

    @Test
    fun `nextGuestClassCounter returns max plus 1 when existing names in same block`() {
        val now = LocalDateTime.of(2026, 4, 28, 9, 0)
        val names = listOf("Apr 28, 2026 (AM) #001", "Apr 28, 2026 (AM) #003")
        assertEquals(4, nextGuestClassCounter(names, now))
    }

    @Test
    fun `nextGuestClassCounter ignores names from different AM-PM block`() {
        val now = LocalDateTime.of(2026, 4, 28, 9, 0)
        val names = listOf("Apr 28, 2026 (PM) #005", "Apr 27, 2026 (AM) #010")
        assertEquals(1, nextGuestClassCounter(names, now))
    }

    @Test
    fun `nextGuestClassCounter handles AM-PM boundary at noon`() {
        val am = LocalDateTime.of(2026, 4, 28, 11, 59)
        val pm = LocalDateTime.of(2026, 4, 28, 12, 1)
        val names = listOf("Apr 28, 2026 (AM) #005")
        assertEquals(6, nextGuestClassCounter(names, am))
        assertEquals(1, nextGuestClassCounter(names, pm))
    }

    @Test
    fun `nextGuestClassCounter handles midnight cross-date boundary`() {
        val pmYesterday = LocalDateTime.of(2026, 4, 27, 23, 59)
        val amToday = LocalDateTime.of(2026, 4, 28, 0, 1)
        val names = listOf("Apr 27, 2026 (PM) #003")
        assertEquals(4, nextGuestClassCounter(names, pmYesterday))
        assertEquals(1, nextGuestClassCounter(names, amToday))
    }

    @Test
    fun `buildGuestClassName produces uppercase AM-PM regardless of default Locale`() {
        Locale.setDefault(Locale.JAPAN)
        val now = LocalDateTime.of(2026, 4, 28, 9, 0)
        assertEquals("Apr 28, 2026 (AM) #001", buildGuestClassName(now, 1))
    }
}
