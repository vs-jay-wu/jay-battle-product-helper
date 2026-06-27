package com.viewsonic.classswift.data.task

import com.viewsonic.classswift.ui.widget.task.enums.RecordType
import com.viewsonic.classswift.ui.widget.task.enums.TaskStatus

data class TaskStatusInfo(
    val infoId: String = "",
    val taskId: String = "",
    val assign: String = "",
    val status: TaskStatus = TaskStatus.UNKNOWN,
    val sequenceNumber: Int = 0,
    val createAt: Long = 0,
    val recordType: RecordType = RecordType.UNKNOWN
)
