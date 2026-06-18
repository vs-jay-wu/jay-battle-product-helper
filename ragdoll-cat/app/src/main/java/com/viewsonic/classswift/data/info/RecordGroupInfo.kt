package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.data.task.TaskResultInfo

data class RecordGroupInfo(
    //Although the API response provides the group ID as a string,
    // the Hub backend management system enforces a numeric-only input.
    //Therefore, we keep the type as Int here for easier future sorting by group order.
    val groupId: Int,
    val taskId: String,
    val data: List<TaskResultInfo> = emptyList()
)
