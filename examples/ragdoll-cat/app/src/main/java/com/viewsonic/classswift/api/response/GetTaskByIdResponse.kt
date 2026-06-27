package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.response.data.LinkMeta

@JsonClass(generateAdapter = true)
data class GetTaskByIdResponse(
    @Json(name = "data")
    val data: TaskRecord
)

@JsonClass(generateAdapter = true)
data class TaskRecord(
    @Json(name = "task_id")
    val taskId: String = "",
    @Json(name = "assign")
    val assign: String = "",
    @Json(name = "status")
    val status: String = "",
    @Json(name = "seq")
    val sequence: Int = 0,
    @Json(name = "created_at")
    val createdAt: Long = 0,
    @Json(name = "link_url")
    val linkUrl: String = "",
    @Json(name = "link_meta")
    val linkMeta: LinkMeta = LinkMeta(
        title = "",
        description = "",
        siteName = "",
        image = ""
    ),

    @Json(name = "img_url")
    val imgUrl: String = "",
    @Json(name = "task_results")
    val taskResults: List<TaskResult> = mutableListOf()
)

@JsonClass(generateAdapter = true)
data class TaskResult(
    @Json(name = "display_name")
    val displayName: String = "",
    @Json(name = "student_id")
    val studentId: String = "",
    @Json(name = "trigger_type")
    val triggerType: String = "",
    @Json(name = "seat_number")
    val seatNumber: String = "",
    @Json(name = "img_url")
    val imgUrl: String = "",
    @Json(name = "link_is_opened")
    val linkIsOpened: Boolean = false,
    @Json(name = "serial_number")
    val serialNumber: Int = 0,
    @Json(name = "group_id")
    val groupId: String = "",
    @Json(name = "status")
    val status: String = "",
    @Json(name = "version")
    val version: Int = 0
)
