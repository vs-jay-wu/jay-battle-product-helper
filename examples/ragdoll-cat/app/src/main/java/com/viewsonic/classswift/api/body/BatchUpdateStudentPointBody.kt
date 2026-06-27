package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BatchUpdateStudentPointBody(
    @Json(name = "students")
    val studentPoints: List<StudentPoint> = listOf()
) {
    @JsonClass(generateAdapter = true)
    data class StudentPoint(
        @Json(name = "student_id")
        val studentId: String,
        @Json(name = "points")
        val points: Int
    )
}