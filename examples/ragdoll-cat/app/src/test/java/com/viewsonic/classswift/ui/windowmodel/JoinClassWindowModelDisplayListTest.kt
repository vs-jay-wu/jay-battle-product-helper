package com.viewsonic.classswift.ui.windowmodel

import com.viewsonic.classswift.data.enum.ClassType
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.info.StudentInfo.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JoinClassWindowModelDisplayListTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun activeStudent(id: Int, displayName: String = "Active$id") = StudentInfo(
        serialNumber = id,
        studentId = "id_$id",
        displayName = displayName,
        status = Status.ACTIVE
    )

    private fun inactiveStudent(id: Int, displayName: String = "") = StudentInfo(
        serialNumber = id,
        studentId = "",
        displayName = displayName,
        status = Status.INACTIVE
    )

    // ── GUEST ─────────────────────────────────────────────────────────────────

    @Test
    fun `filterByClassType GUEST keeps only ACTIVE students`() {
        val list = listOf(activeStudent(1), inactiveStudent(2), activeStudent(3))

        val result = JoinClassWindowModel.filterByClassType(list, ClassType.GUEST)

        assertEquals(listOf(activeStudent(1), activeStudent(3)), result)
    }

    @Test
    fun `filterByClassType GUEST returns empty when all students are INACTIVE`() {
        val list = listOf(inactiveStudent(1), inactiveStudent(2))

        val result = JoinClassWindowModel.filterByClassType(list, ClassType.GUEST)

        assertTrue(result.isEmpty())
    }

    // ── SSO_GOOGLE ────────────────────────────────────────────────────────────

    @Test
    fun `filterByClassType SSO_GOOGLE keeps ACTIVE students`() {
        val active = activeStudent(1)
        val inactive = inactiveStudent(2)
        val list = listOf(active, inactive)

        val result = JoinClassWindowModel.filterByClassType(list, ClassType.SSO_GOOGLE)

        assertEquals(listOf(active), result)
    }

    @Test
    fun `filterByClassType SSO_GOOGLE keeps INACTIVE students with non-empty displayName`() {
        val withName = inactiveStudent(1, displayName = "Roster Name")
        val noName = inactiveStudent(2, displayName = "")
        val list = listOf(withName, noName)

        val result = JoinClassWindowModel.filterByClassType(list, ClassType.SSO_GOOGLE)

        assertEquals(listOf(withName), result)
    }

    @Test
    fun `filterByClassType SSO_GOOGLE keeps student that is ACTIVE and has displayName`() {
        val student = activeStudent(1, displayName = "Both")
        val result = JoinClassWindowModel.filterByClassType(listOf(student), ClassType.SSO_GOOGLE)

        assertEquals(listOf(student), result)
    }

    @Test
    fun `filterByClassType SSO_GOOGLE excludes INACTIVE students with empty displayName`() {
        val list = listOf(inactiveStudent(1, displayName = ""))

        val result = JoinClassWindowModel.filterByClassType(list, ClassType.SSO_GOOGLE)

        assertTrue(result.isEmpty())
    }

    // ── ROSTER / TW_DEFAULT / OTHER — no filtering ───────────────────────────

    @Test
    fun `filterByClassType ROSTER returns list unchanged`() {
        val list = listOf(activeStudent(1), inactiveStudent(2))
        assertEquals(list, JoinClassWindowModel.filterByClassType(list, ClassType.ROSTER))
    }

    @Test
    fun `filterByClassType TW_DEFAULT returns list unchanged`() {
        val list = listOf(activeStudent(1), inactiveStudent(2))
        assertEquals(list, JoinClassWindowModel.filterByClassType(list, ClassType.TW_DEFAULT))
    }

    @Test
    fun `filterByClassType OTHER returns list unchanged`() {
        val list = listOf(activeStudent(1), inactiveStudent(2))
        assertEquals(list, JoinClassWindowModel.filterByClassType(list, ClassType.OTHER))
    }
}
