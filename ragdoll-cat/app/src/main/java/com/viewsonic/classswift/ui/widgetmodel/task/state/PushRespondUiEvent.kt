package com.viewsonic.classswift.ui.widgetmodel.task.state

import com.viewsonic.classswift.data.task.TaskApiResult
import com.viewsonic.classswift.data.task.TaskInfo

sealed class PushRespondUiEvent {
    data object ScreenshotFail : PushRespondUiEvent()
    data object EndQuizFail : PushRespondUiEvent()
    data class PushTaskFail(
        val successfullyCount: Int,
        val failedCount: Int,
        val data: List<TaskApiResult<TaskInfo>>,
        val successTasks: List<TaskInfo>
    ) : PushRespondUiEvent()
}