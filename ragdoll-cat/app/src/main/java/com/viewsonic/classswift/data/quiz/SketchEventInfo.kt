package com.viewsonic.classswift.data.quiz

/**
 * `SketchTaskCoordinator` 對外 emit 的一次性事件（toast / dialog 觸發用）。
 *
 * 從 `RecordEventInfo` 拷貝簡化：只保留 Sketch 結果頁實際會 emit 的事件。
 * - 砍 `EndTaskSuccess/Failed`（Sketch 端課由 `closeBatchTask` 處理，邏輯在 8451）
 * - 砍 `GetTasksFailed`（Sketch 不走 `getTasks(filter)` 多 task 列表）
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8453">VSFT-8453</a>
 */
sealed class SketchEventInfo {
    data object GetRecordByTaskIdFailed : SketchEventInfo()
}
