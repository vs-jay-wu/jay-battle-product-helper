package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.response.data.LinkMeta

@JsonClass(generateAdapter = true)
data class GetTasksResponse(
    @Json(name = "data")
    val data: List<TaskInfo> = mutableListOf()
)

@JsonClass(generateAdapter = true)
data class TaskInfo(
    @Json(name = "task_id")
    val taskId: String = "",
    @Json(name = "assign")
    val assign: String = "",
    @Json(name = "submitted_count")
    val submittedCount: Int = 0,
    @Json(name = "total_count")
    val totalCount: Int = 0,
    @Json(name = "submitted_rate")
    val submittedRate: Int = 0,
    @Json(name = "seq")
    val sequence: Int = 0,
    @Json(name = "img_url")
    val imgUrl: String = "",
    @Json(name = "link_url")
    val linkUrl: String = "",
    @Json(name = "link_opened_count")
    val linkOpenedCount: Int = 0,
    @Json(name = "link_total_count")
    val linkTotalCount: Int = 0,
    @Json(name = "link_opened_rate")
    val linkOpenedRate: Int = 0,
    @Json(name = "link_meta")
    val linkMeta: LinkMeta = LinkMeta(
        title = "",
        description = "",
        siteName = "",
        image = ""
    ),
    @Json(name = "end_time")
    val endTime: Int = 0
)