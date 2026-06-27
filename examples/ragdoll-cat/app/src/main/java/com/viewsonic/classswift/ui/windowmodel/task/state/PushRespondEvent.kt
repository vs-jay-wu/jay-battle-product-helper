package com.viewsonic.classswift.ui.windowmodel.task.state

import com.viewsonic.classswift.data.task.TaskResultInfo

sealed class PushRespondEvent {
    data class QuizRespondConflict(
        val isStopPushRespond: Boolean = false
    ) : PushRespondEvent()

    data class NetworkStatusChange(
        val isNetworkConnected: Boolean
    ) : PushRespondEvent()

    data class UpdateStudentRecordList(val recordList: List<TaskResultInfo>) : PushRespondEvent()

    data object GetRecordListFailed: PushRespondEvent()

}