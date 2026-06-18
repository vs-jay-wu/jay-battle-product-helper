package com.viewsonic.classswift.ui.widgetmodel.records.state

import com.viewsonic.classswift.data.task.RecordEventInfo
import com.viewsonic.classswift.data.task.TaskApiResult
import com.viewsonic.classswift.data.task.TaskResultInfo

sealed class RecordUiEvent {
    data class RecordEventUpdate(val data: RecordEventInfo) : RecordUiEvent()

    data class MarkUpdateFailed(
        val id: String,
        val errorMessage: String,
        val isMultiMark: Boolean,
    ) : RecordUiEvent()

    data class PushRecordFail(
        val successfullyCount: Int,
        val failedCount: Int,
        val data: List<TaskApiResult<TaskResultInfo>>
    ) : RecordUiEvent()
}