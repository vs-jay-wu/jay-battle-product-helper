package com.viewsonic.classswift.data.task

import com.viewsonic.classswift.api.response.data.LinkMeta
import com.viewsonic.classswift.ui.widget.task.enums.TaskStatus

data class TaskRecordInfo(
    val taskId: String = "",
    val assign: String = "",
    val status: TaskStatus = TaskStatus.UNKNOWN,
    val seq: Int = 0,
    val createdAt: Long = 0,
    val linkUrl: String = "",
    val linkMeta: LinkMeta = LinkMeta(
        title = "",
        description = "",
        siteName = "",
        image = ""
    ),
    val imgUrl: String = "",
    val taskResults: List<TaskResultInfo> = mutableListOf()
)