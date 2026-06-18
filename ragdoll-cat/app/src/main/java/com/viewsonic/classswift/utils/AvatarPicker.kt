package com.viewsonic.classswift.utils

import androidx.annotation.DrawableRes
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.StudentInfo

object AvatarPicker {

    private val avatars = listOf(
        R.drawable.ic_avatar_student_01,
        R.drawable.ic_avatar_student_02,
        R.drawable.ic_avatar_student_03,
        R.drawable.ic_avatar_student_04,
    )

    /**
     * 依學生資料選取頭像。
     *
     * JOINED 狀態的學生 [StudentInfo.studentId] 一定有值（UUID-like），
     * 以 [String.hashCode] 取得天然隨機分布，避免連續座號（1, 2, 3, 4, 5…）
     * 直接對 avatars.size 取模造成的循環圖案（0-1-2-3-0-1-2-3…）。
     *
     * 萬一 studentId 為空（邊界情況），以 [scatter] 對 serialNumber 做非線性雜湊，
     * 同樣避免循環。
     */
    @DrawableRes
    fun pick(studentInfo: StudentInfo): Int {
        val key = if (studentInfo.studentId.isNotEmpty()) {
            studentInfo.studentId.hashCode()
        } else {
            scatter(studentInfo.serialNumber)
        }
        return avatars[Math.floorMod(key, avatars.size)]
    }

    /**
     * 32-bit 非線性雜湊（MurmurHash3 finalizer）。
     *
     * 確保連續整數映射到非規律的輸出，
     * 避免 `serialNumber % avatars.size` 的循環問題。
     */
    private fun scatter(value: Int): Int {
        var h = value
        h = h xor (h ushr 16)
        h *= -2048144725  // 0x85ebca6b — MurmurHash3 mix constant
        h = h xor (h ushr 13)
        h *= -1057398731  // 0xc2b2ae35 — MurmurHash3 mix constant
        return h xor (h ushr 16)
    }
}
