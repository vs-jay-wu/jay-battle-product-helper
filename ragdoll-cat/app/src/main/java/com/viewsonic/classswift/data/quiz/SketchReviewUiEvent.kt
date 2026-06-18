package com.viewsonic.classswift.data.quiz

import com.viewsonic.classswift.data.task.TaskResultInfo

/**
 * `SketchReviewWidget` <-> `SketchReviewWidgetModel` 之間的 UI event。
 *
 * 從 `ui/widgetmodel/records/state/RecordMarkUiEvent.kt` 拷貝後簡化：
 * - 砍 `ReleaseSeat`（學生離座自動切換 — Sprint 21+ 行為，且 Sketch 單筆無 list 概念）
 * - 砍 `UpdateCurrentRecord`（學生訂正後自動跳回 — Sprint 21+ 行為）
 * - `SwitchContent` 改名 `SetContent`（單筆設定，非多筆切換）
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8454">VSFT-8454</a>
 */
sealed class SketchReviewUiEvent {
    data object UploadImageFailed : SketchReviewUiEvent()
    data class UploadImageSuccess(val imgUrl: String) : SketchReviewUiEvent()
    data class SetContent(val info: TaskResultInfo) : SketchReviewUiEvent()
}
