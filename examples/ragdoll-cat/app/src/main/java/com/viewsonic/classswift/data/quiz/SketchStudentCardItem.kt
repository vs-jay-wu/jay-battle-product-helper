package com.viewsonic.classswift.data.quiz

import com.viewsonic.classswift.data.task.TaskResultInfo

/**
 * `MvbSketchStudentCardAdapter` 顯示用 item。
 *
 * @param record 原始 [TaskResultInfo]，點 card 後傳回給上層觸發 SketchReviewWidget
 * @param status UI 狀態（依 record 計算）
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8454">VSFT-8454</a>
 */
data class SketchStudentCardItem(
    val record: TaskResultInfo,
    val status: SketchStudentStatus,
) {
    val studentId: String get() = record.studentId
    val seatNumber: String get() = record.seatNumber
    val displayName: String get() = record.displayName
    val serialNumber: Int get() = record.serialNumber

    /**
     * Card name bar 顯示文字 — Figma 5058-115722 / 6607-28713 規格：只顯示名字、不帶座號前綴。
     *
     * 對齊 "show students' name" toggle 行為：toggle ON 顯示 [displayName]；
     * toggle OFF 在 adapter 層改 `View.GONE` 整個 name bar 隱藏。
     */
    val numberAndName: String
        get() = displayName

    companion object {
        fun fromTaskResult(record: TaskResultInfo): SketchStudentCardItem =
            SketchStudentCardItem(
                record = record,
                status = SketchStudentStatus.fromTaskResult(record),
            )
    }
}
