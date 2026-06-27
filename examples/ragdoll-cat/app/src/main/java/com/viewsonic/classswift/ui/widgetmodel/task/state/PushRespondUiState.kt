package com.viewsonic.classswift.ui.widgetmodel.task.state

import com.viewsonic.classswift.data.task.TaskApiResult
import com.viewsonic.classswift.data.task.TaskInfo

sealed class PushRespondUiState {
    data object Idle : PushRespondUiState()
    data class TaskListUpdate(val data: List<TaskInfo>) : PushRespondUiState()
    data class PushTaskCompleted(
        val stateId: String,
        val data: List<TaskApiResult<TaskInfo>>
    ) : PushRespondUiState()
}