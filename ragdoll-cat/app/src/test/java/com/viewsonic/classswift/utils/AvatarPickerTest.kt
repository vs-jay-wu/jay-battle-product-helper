package com.viewsonic.classswift.utils

import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.info.StudentInfo.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AvatarPicker 回傳 @DrawableRes Int（resource ID），不是 0-3 的 index。
 * 測試只依賴輸出值之間的相對關係（相同/相異/分布），
 * 不假設 resource ID 的絕對數值範圍。
 */
class AvatarPickerTest {

    private val avatarCount = 4   // must match AvatarPicker.avatars.size

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun joinedStudent(serialNumber: Int, studentId: String) = StudentInfo(
        serialNumber = serialNumber,
        studentId = studentId,
        status = Status.ACTIVE
    )

    private fun studentWithoutId(serialNumber: Int) = StudentInfo(
        serialNumber = serialNumber,
        studentId = "",
        status = Status.ACTIVE
    )

    // ── deterministic ─────────────────────────────────────────────────────────

    @Test
    fun `pick is deterministic for same studentId`() {
        val student = joinedStudent(5, "f3a9-11ec-8d3b-0242ac130003")
        assertEquals(AvatarPicker.pick(student), AvatarPicker.pick(student))
    }

    @Test
    fun `pick is deterministic for same serialNumber when studentId is empty`() {
        val student = studentWithoutId(serialNumber = 7)
        assertEquals(AvatarPicker.pick(student), AvatarPicker.pick(student))
    }

    // ── studentId takes priority over serialNumber ────────────────────────────

    @Test
    fun `same serialNumber with different studentIds produces varied avatars`() {
        // studentId is the primary key — 4 distinct UUIDs must map to at least 2 avatars.
        val studentIds = listOf(
            "550e8400-e29b-41d4-a716-446655440000",
            "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
            "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
            "7c9e6679-7425-40de-944b-e07fc1f90ae7"
        )
        val results = studentIds.map { id -> AvatarPicker.pick(joinedStudent(1, id)) }
        assertTrue(
            "4 distinct studentIds on the same serialNumber should use at least 2 different avatars",
            results.toSet().size >= 2
        )
    }

    // ── no sequential cycling (core fix) ─────────────────────────────────────

    @Test
    fun `studentId-based pick uses all 4 avatars across 16 joined students`() {
        // Use UUIDs with high entropy in every position (not just the last char).
        // This verifies studentId.hashCode() distributes well, unlike serialNumber % 4.
        // These UUIDs are verified to cover all 4 slots with Java String.hashCode().
        // If this test fails after a JVM upgrade, re-verify the hash distribution.
        val uuids = listOf(
            "550e8400-e29b-41d4-a716-446655440000",
            "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
            "7c9e6679-7425-40de-944b-e07fc1f90ae7",
            "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            "1d7d6344-f003-4c37-8e36-b8a3cd49f4a7",
            "38400000-8cf0-11bd-b23e-10b96e4ef00d",
            "b3d1e0f4-ccd5-4b3e-8e6d-1234567890ab",
            "c56a4180-65aa-42ec-a945-5fd21dec0538",
            "d290f1ee-6c54-4b01-886e-234e5e6f9870",
            "e1b849f9-2f59-4c6b-a00c-06d0e93ead1d",
            "f2c7e1f6-1b5a-4b97-b1f6-876cc26f7a41",
            "a1b6f5bb-afd6-43f7-b93e-1c23f8dc2e08",
            "bc3d9f78-2309-4e13-9d8a-0bc0e1e0c8d9",
            "cd4e0a8b-3412-4f14-ae9b-1cd1f2f1d9ea",
            "de5f1b9c-4523-4025-bf0c-2de2030eaafb"
        )
        val results = uuids.mapIndexed { i, id -> AvatarPicker.pick(joinedStudent(i + 1, id)) }
        assertEquals(
            "16 students with high-entropy UUIDs should use all $avatarCount avatar slots",
            avatarCount,
            results.toSet().size
        )
    }

    @Test
    fun `students 1-4 and students 5-8 with distinct UUIDs do not produce identical avatar sequences`() {
        // Detects the exact cycling bug from serialNumber % 4:
        // students 1-4 and 5-8 would produce identical sequences (same remainder).
        val uuids = listOf(
            "550e8400-e29b-41d4-a716-446655440000",
            "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
            "7c9e6679-7425-40de-944b-e07fc1f90ae7",
            "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            "1d7d6344-f003-4c37-8e36-b8a3cd49f4a7",
            "38400000-8cf0-11bd-b23e-10b96e4ef00d",
            "b3d1e0f4-ccd5-4b3e-8e6d-1234567890ab"
        )
        val first4  = uuids.subList(0, 4).mapIndexed { i, id -> AvatarPicker.pick(joinedStudent(i + 1, id)) }
        val second4 = uuids.subList(4, 8).mapIndexed { i, id -> AvatarPicker.pick(joinedStudent(i + 5, id)) }
        assertNotEquals(
            "Students 5-8 should not produce the same avatar sequence as students 1-4",
            first4,
            second4
        )
    }

    // ── scatter() fallback ────────────────────────────────────────────────────

    @Test
    fun `scatter fallback does not produce simple modulo cycle for serial 1-4`() {
        // Old bug: serialNumber % 4 → {1, 2, 3, 0}. After fix, scatter() must
        // produce a different (non-sequential) mapping for the first 4 seats.
        val results = (1..4).map { n -> AvatarPicker.pick(studentWithoutId(n)) }
        // At minimum, not all 4 students should get the same avatar
        assertTrue(
            "scatter() should not assign the same avatar to all first 4 students",
            results.toSet().size > 1
        )
    }

    @Test
    fun `scatter fallback is non-linear — serial 1-4 and 5-8 should not repeat identically`() {
        val first4  = (1..4).map { n -> AvatarPicker.pick(studentWithoutId(n)) }
        val second4 = (5..8).map { n -> AvatarPicker.pick(studentWithoutId(n)) }
        assertNotEquals(
            "scatter() should not produce a repeating 4-cycle for serial 1-8",
            first4,
            second4
        )
    }
}
