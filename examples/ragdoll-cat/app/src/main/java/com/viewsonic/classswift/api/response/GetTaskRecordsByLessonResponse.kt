package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetTaskRecordsByLessonResponse(
    @Json(name = "data")
    val data: List<TaskData> = mutableListOf()
)

@JsonClass(generateAdapter = true)
data class TaskData(
    @Json(name = "task_id")
    val taskId: String = "",
    @Json(name = "assign")
    val assign: String = "",
    @Json(name = "created_at")
    val createdAt: Long = 0,
    @Json(name = "task_type")
    val taskType: String = "",
    @Json(name = "task_cover_preview_url")
    val taskCoverPreviewUrl: String = "",
    @Json(name = "task_result_type")
    val taskResultType: String = "",
    @Json(name = "pinned")
    val pinned: Boolean = false
)
