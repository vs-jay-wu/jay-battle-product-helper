package com.viewsonic.classswift.data.quiz

import com.viewsonic.classswift.api.response.UpdateTaskResult

/**
 * `SketchMarkHandler` 的 mark + hand-back 結果狀態。
 *
 * 從 `ui/widgetmodel/records/state/MarkUpdateState.kt` 拷貝後簡化：
 * - 砍掉 `MultiMarkSuccess`（Sketch 結果頁只走 single-mark）
 * - `Failed` 砍掉 `isMultiMark` 旗標
 * - `Failed` 用 [FailureReason] sealed type 取代 raw `errorMessage: String`
 *   （避免 hardcoded "NetworkDisconnected" 等技術詞洩漏給 UI；
 *   由 caller 依 reason 翻譯成對應 R.string）
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8454">VSFT-8454</a>
 */
sealed class SketchMarkUpdateState {

    /**
     * 此 sealed class 透過 SharedFlow（replay=0）傳遞；Idle 從不 emit，
     * 僅作為 `when` exhaustive 覆蓋的結構完整性佔位。
     * Window 端第一個收到的 event 必為 [Success] 或 [Failed]。
     */
    data object Idle : SketchMarkUpdateState()

    data class Success(
        val id: String,
        val success: UpdateTaskResult?,
        val failed: UpdateTaskResult?,
    ) : SketchMarkUpdateState()

    data class Failed(
        val id: String,
        val reason: FailureReason,
    ) : SketchMarkUpdateState()

    sealed class FailureReason {
        /** Network 斷線。 */
        data object NetworkDisconnected : FailureReason()

        /** Backend 回 RFC7807 錯誤。 */
        data class ApiError(val title: String) : FailureReason()

        /** 其他未知錯誤（含 ExceptionFailure / 未匹配的 ApiResponse 分支）。 */
        data object Unknown : FailureReason()
    }
}
