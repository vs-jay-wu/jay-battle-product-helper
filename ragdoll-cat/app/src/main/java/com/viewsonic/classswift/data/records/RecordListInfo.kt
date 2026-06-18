package com.viewsonic.classswift.data.records

import com.viewsonic.classswift.ui.widget.task.enums.RecordType
import com.viewsonic.classswift.ui.widget.task.enums.TaskStatus

sealed class RecordListInfo {

    data class Header(
        val type: HeaderType
    ) : RecordListInfo()

    data class TaskItem(
        val id: String,
        val isSelected: Boolean,
        val createAt: Long = 0,
        val taskType: RecordType,
        val taskStatus: TaskStatus,
        val endTime: Int = 0,
        val sequenceNumber: Int = 0
    ) : RecordListInfo()

    enum class HeaderType {
        RECEIVING,
        TASKENDED
    }
}
