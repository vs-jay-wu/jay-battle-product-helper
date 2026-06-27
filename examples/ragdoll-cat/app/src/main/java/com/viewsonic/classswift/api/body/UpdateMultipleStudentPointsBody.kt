package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateMultipleStudentPointsBody(
    @Json(name = "student_ids")
    val studentIds: List<String> = emptyList(),
    val points: Int = 0
)
