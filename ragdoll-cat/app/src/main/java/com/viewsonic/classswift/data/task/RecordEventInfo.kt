package com.viewsonic.classswift.data.task


sealed class RecordEventInfo {
    data class EndTaskSuccess(
        val successCount: Int,
    ) : RecordEventInfo()

    data class EndTaskFailed(
        val successCount: Int,
        val failedCount: Int
    ) : RecordEventInfo()

    data object GetTasksFailed : RecordEventInfo()
    data object GetRecordByTaskIdFailed : RecordEventInfo()
}