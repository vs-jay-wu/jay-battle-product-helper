package com.viewsonic.classswift.data.task

import com.viewsonic.classswift.api.response.data.LinkMeta

data class RemoteTaskInfo(
    val taskId: String = "",
    val assign: String = "",
    val submittedCount: Int = 0,
    val totalCount: Int = 0,
    val submittedRate: Int = 0,
    val seq: Int = 0,
    val imgUrl: String = "",
    val linkUrl: String = "",
    val linkOpenedCount: Int = 0,
    val linkTotalCount: Int = 0,
    val linkOpenedRate: Int = 0,
    val linkMeta: LinkMeta = LinkMeta(
        title = "",
        description = "",
        siteName = "",
        image = ""
    ),
    val endTime: Int = 0
)
