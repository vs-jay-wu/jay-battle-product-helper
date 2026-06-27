package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CloseTaskResponse(
    @Json(name = "result")
    val result: CloseTaskResult = CloseTaskResult()
)

@JsonClass(generateAdapter = true)
data class CloseTaskResult(
    @Json(name = "created_at")
    val createdAt: Int = 0,
    @Json(name = "updated_at")
    val updatedAt: Int = 0,
    @Json(name = "img_key")
    val imgKey: String = "",
    @Json(name = "link_url")
    val linkUrl: String = "",
    @Json(name = "lesson_id")
    val lessonId: String = "",
    @Json(name = "status")
    val status: String = "",
    @Json(name = "assign")
    val assign: String = "",
    @Json(name = "seq")
    val seq: Int = 0,
    @Json(name = "task_type")
    val taskType: String = "",
    @Json(name = "end_time")
    val endTime: Int = 0,
    @Json(name = "id")
    val id: String = ""
)