package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.response.data.LinkMeta

@JsonClass(generateAdapter = true)
data class CreateTaskResponse(
    @Json(name = "task_id")
    val taskId: String,
    @Json(name = "lesson_id")
    val lessonId: String,
    @Json(name = "assign")
    val assign: String,
    @Json(name = "seq")
    val seq: Int,
    @Json(name = "task_type")
    val taskType: String,
    @Json(name = "img_url")
    val imageUrl: String = "",
    @Json(name = "seat_number_list")
    val seatNumberList: List<Int>,
    @Json(name = "link_url")
    val linkUrl: String = "",
    @Json(name = "link_meta")
    val linkMeta: LinkMeta = LinkMeta(
        title = "",
        description = "",
        siteName = "",
        image = ""
    ),
    @Json(name = "created_at")
    val createdAt: Long
)