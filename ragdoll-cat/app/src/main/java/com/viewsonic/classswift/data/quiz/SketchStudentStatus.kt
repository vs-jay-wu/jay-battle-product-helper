package com.viewsonic.classswift.data.quiz

import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus

/**
 * Sketch Response 結果頁學生 card 顯示用 5 狀態。
 *
 * Confluence US 3-2 對應：
 *  - 有提交、老師未批 → [CLICK_TO_VIEW]（可點擊批改）
 *  - 老師已批改並 handed back → [HANDED_BACK]（可點擊再批改）
 *  - 未提交 → [NOT_SUBMITTED]（不可點）
 *  - 學生未加入課堂 → [ABSENT]（不可點）
 *
 * [SUBMITTED] enum 值保留作為 future state placeholder；Sprint 20 [fromTaskResult] 不會 emit
 * （`RESPONSE` 直接走 [CLICK_TO_VIEW]）。Sprint 21+ 若 PM 決定要區分「已提交但 GRADED 前」
 * 與「已 handed back」需要不同視覺，再啟用 SUBMITTED。
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8454">VSFT-8454</a>
 */
enum class SketchStudentStatus {
    /** 學生已提交、老師尚未批改 → bg primary、可點擊進 SketchReviewWidget（Sprint 20 不使用） */
    SUBMITTED,

    /** 老師已批改並 handed back 給學生 → bg success + chip 含 check icon + "Handed back"、可點擊重新批改。 */
    HANDED_BACK,

    /** 學生提交但老師尚未批 → bg success + hand-finger-tilt icon + "Click to view"、可點擊批改。 */
    CLICK_TO_VIEW,

    /** 學生未提交 → bg white、不可點 */
    NOT_SUBMITTED,

    /** 學生未加入課堂 → bg disabled、不可點 */
    ABSENT;

    /** 此狀態是否允許點擊 card 進 SketchReviewWidget */
    val isClickable: Boolean
        get() = this == SUBMITTED || this == HANDED_BACK || this == CLICK_TO_VIEW

    companion object {
        /**
         * 從 [TaskResultInfo] 對應到 [SketchStudentStatus]。
         *
         * Mapping（對齊 Confluence US 3-2 語意）：
         * - [TaskResultInfo.Guest] → [ABSENT]
         * - [TaskResultInfo.Content] + [SubmitStatus.UNSUBMITTED] / [SubmitStatus.UNKNOWN] → [NOT_SUBMITTED]
         * - [TaskResultInfo.Content] + [SubmitStatus.RESPONSE] → [CLICK_TO_VIEW]
         *   （學生有提交、老師尚未批改 — UI 提示「可點開來看 / 批改」）
         * - [TaskResultInfo.Content] + [SubmitStatus.GRADED] → [HANDED_BACK]
         *   （老師已批改並 handed back — `updateTaskResult` 內帶 `taskResultType=GRADED`，
         *   後端持久化後 re-fetch 即為此狀態）
         * - [TaskResultInfo.ApiFail] / [TaskResultInfo.Link] → [NOT_SUBMITTED]（預期不會發生，fallback）
         */
        fun fromTaskResult(info: TaskResultInfo): SketchStudentStatus {
            return when (info) {
                is TaskResultInfo.Guest -> ABSENT
                is TaskResultInfo.Content -> when (info.triggerType) {
                    SubmitStatus.UNSUBMITTED, SubmitStatus.UNKNOWN -> NOT_SUBMITTED
                    SubmitStatus.RESPONSE -> CLICK_TO_VIEW
                    SubmitStatus.GRADED -> HANDED_BACK
                }
                is TaskResultInfo.ApiFail,
                is TaskResultInfo.Link -> NOT_SUBMITTED
            }
        }
    }
}
