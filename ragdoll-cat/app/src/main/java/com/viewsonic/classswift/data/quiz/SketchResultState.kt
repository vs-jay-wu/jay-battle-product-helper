package com.viewsonic.classswift.data.quiz

import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.data.task.TaskStatusInfo

/**
 * `SketchTaskCoordinator` 對外 emit 的單題結果狀態。
 *
 * 從 `RecordsCoordinator.RecordResultState` 拷貝重命名（內容欄位不變）。
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8453">VSFT-8453</a>
 */
data class SketchResultState(
    val taskInfo: TaskStatusInfo = TaskStatusInfo(),
    val recordList: List<TaskResultInfo> = emptyList(),
    /** Task-level 題目截圖 URL（給左 question section preview 用）。 */
    val taskImgUrl: String = "",
)
