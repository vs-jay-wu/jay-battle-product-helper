package com.viewsonic.classswift.manager

import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.info.StudentInfo.Status
import org.junit.Assert.assertEquals
import org.junit.Test

class StudentManagerTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun student(
        serialNumber: Int,
        displayName: String = "Student$serialNumber",
        displaySeatNumber: String = "S$serialNumber",
        studentId: String = "id_$serialNumber",
        status: Status = Status.ACTIVE
    ) = StudentInfo(
        serialNumber = serialNumber,
        displayName = displayName,
        displaySeatNumber = displaySeatNumber,
        studentId = studentId,
        status = status
    )

    // ── fullReplace = true ────────────────────────────────────────────────────

    @Test
    fun `buildMergedList fullReplace true on non-empty list replaces entire list`() {
        val current = listOf(student(1), student(2), student(3))
        val incoming = listOf(student(4), student(5))

        val result = StudentManager.buildMergedList(current, incoming, fullReplace = true)

        assertEquals(listOf(student(4), student(5)), result)
    }

    @Test
    fun `buildMergedList fullReplace true with empty incoming clears list`() {
        val current = listOf(student(1), student(2))

        val result = StudentManager.buildMergedList(current, emptyList(), fullReplace = true)

        assertEquals(emptyList<StudentInfo>(), result)
    }

    @Test
    fun `buildMergedList fullReplace true does not carry over students absent in newList`() {
        val current = listOf(student(1), student(2), student(3))
        val incoming = listOf(student(2))

        val result = StudentManager.buildMergedList(current, incoming, fullReplace = true)

        assertEquals(listOf(student(2)), result)
    }

    // ── fullReplace = false (merge) ───────────────────────────────────────────

    @Test
    fun `buildMergedList fullReplace false merges by serialNumber`() {
        val current = listOf(student(1), student(2))
        val incoming = listOf(student(3))

        val result = StudentManager.buildMergedList(current, incoming, fullReplace = false)

        assertEquals(listOf(student(1), student(2), student(3)), result)
    }

    @Test
    fun `buildMergedList fullReplace false updates existing student by serialNumber`() {
        val current = listOf(student(1, displayName = "OldName"))
        val incoming = listOf(student(1, displayName = "NewName"))

        val result = StudentManager.buildMergedList(current, incoming, fullReplace = false)

        assertEquals(1, result.size)
        assertEquals("NewName", result[0].displayName)
    }

    @Test
    fun `buildMergedList fullReplace false retains students not in newList`() {
        val current = listOf(student(1), student(2))
        val incoming = listOf(student(2, displayName = "Updated"))

        val result = StudentManager.buildMergedList(current, incoming, fullReplace = false)

        assertEquals(2, result.size)
        assertEquals(student(1), result[0])
        assertEquals("Updated", result[1].displayName)
    }

    // ── sort order ────────────────────────────────────────────────────────────

    @Test
    fun `buildMergedList sorts by serialNumber ascending`() {
        val current = emptyList<StudentInfo>()
        val incoming = listOf(student(3), student(1), student(2))

        val result = StudentManager.buildMergedList(current, incoming, fullReplace = false)

        assertEquals(listOf(1, 2, 3), result.map { it.serialNumber })
    }

    @Test
    fun `buildMergedList puts negative serialNumber at end`() {
        val current = emptyList<StudentInfo>()
        val incoming = listOf(student(-1), student(2), student(1))

        val result = StudentManager.buildMergedList(current, incoming, fullReplace = false)

        assertEquals(listOf(1, 2, -1), result.map { it.serialNumber })
    }

    @Test
    fun `buildMergedList puts student with empty displayName and displaySeatNumber at end`() {
        val anonymous = StudentInfo(serialNumber = 1, displayName = "", displaySeatNumber = "")
        val named = student(2)
        val current = emptyList<StudentInfo>()
        val incoming = listOf(anonymous, named)

        val result = StudentManager.buildMergedList(current, incoming, fullReplace = false)

        assertEquals(listOf(named.serialNumber, anonymous.serialNumber), result.map { it.serialNumber })
    }
}
