package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateTaskResultBody(
    @Json(name = "student_id")
    val studentId: String = "",
    @Json(name = "task_type")
    val taskType: String = "SCREENSHOT",
    @Json(name = "img_url")
    val imgUrl: String = "",
    @Json(name = "task_result_type")
    val taskResultType: String = "GRADED",
    @Json(name = "version")
    val version: Int = 0
)