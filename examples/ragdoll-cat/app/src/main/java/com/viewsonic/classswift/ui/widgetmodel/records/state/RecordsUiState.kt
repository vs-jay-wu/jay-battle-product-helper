package com.viewsonic.classswift.ui.widgetmodel.records.state

import com.viewsonic.classswift.api.response.UpdateTaskResult
import com.viewsonic.classswift.data.records.TaskListUpdateInfo
import com.viewsonic.classswift.data.task.TaskApiResult
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.data.task.TaskStatusInfo

sealed class RecordsUiState {
    data object Idle : RecordsUiState()

    data class RecordListUpdate(
        val id: String,
        val data: TaskListUpdateInfo
    ) : RecordsUiState()

    data class RecordResultUpdate(
        val id: String,
        val taskStatusInfo: TaskStatusInfo,
        val data: List<TaskResultInfo>
    ) : RecordsUiState()

    data class SingleMarkUpdate(
        val id: String,
        val success: UpdateTaskResult?,
        val failed: UpdateTaskResult?
    ) : RecordsUiState()

    data class MultiMarkUpdate(
        val id: String,
        val success: List<UpdateTaskResult>,
        val failed: List<UpdateTaskResult>
    ) : RecordsUiState()

    data class PushRecordCompleted(
        val id: String,
        val data: List<TaskApiResult<TaskResultInfo>>
    ): RecordsUiState()

}