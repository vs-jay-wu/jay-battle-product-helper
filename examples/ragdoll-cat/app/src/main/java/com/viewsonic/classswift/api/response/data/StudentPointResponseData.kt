package com.viewsonic.classswift.api.response.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StudentPointResponseData(
    @Json(name = "student_id")
    val studentId: String = "",
    @Json(name = "total_points")
    val totalPoints: Int = 0
)
