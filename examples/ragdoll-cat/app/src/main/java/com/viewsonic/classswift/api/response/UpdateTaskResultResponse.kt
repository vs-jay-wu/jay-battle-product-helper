package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateTaskResultResponse(
    @Json(name = "success")
    val success: List<UpdateTaskResult> = mutableListOf(),
    @Json(name = "failed")
    val failed: List<UpdateTaskResult> = mutableListOf()
)

@JsonClass(generateAdapter = true)
data class UpdateTaskResult(
    @Json(name = "id")
    val id: String = "",
    @Json(name = "group_id")
    val groupId: String = "",
    @Json(name = "trigger_type")
    val triggerType: String = "",
    @Json(name = "is_opened")
    val isOpened: Boolean = false,
    @Json(name = "student_id")
    val studentId: String = "",
    @Json(name = "task_id")
    val taskId: String = "",
    @Json(name = "version")
    val version: Int = -1,
    @Json(name = "img_url")
    val imgUrl: String = "",
    @Json(name = "created_at")
    val createdAt: Long = 0L,
    @Json(name = "updated_at")
    val updatedAt: Long = 0L
)