package com.viewsonic.classswift.ui.widgetmodel.records.state

import com.viewsonic.classswift.data.task.TaskResultInfo

sealed class RecordMarkUiEvent {
    object UploadImageFailed : RecordMarkUiEvent()
    data class UploadImageSuccess(val imgUrl: String) : RecordMarkUiEvent()
    data class SwitchContent(val info: TaskResultInfo) : RecordMarkUiEvent()
    data class ReleaseSeat(val oldInfo: TaskResultInfo, val newinfo: TaskResultInfo) : RecordMarkUiEvent()
    data class UpdateCurrentRecord(val info: TaskResultInfo) : RecordMarkUiEvent()
}