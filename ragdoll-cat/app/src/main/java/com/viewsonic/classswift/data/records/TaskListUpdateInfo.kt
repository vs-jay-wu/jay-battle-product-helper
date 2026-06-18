package com.viewsonic.classswift.data.records

data class TaskListUpdateInfo(
    val id: String = "",
    val isEndTask: Boolean = false,
    val data: List<RecordListInfo> = emptyList()
)
