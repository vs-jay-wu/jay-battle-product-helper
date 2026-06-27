package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateLessonResponse(
    @Json(name = "data")
    val lessonData: LessonData
) {
    @JsonClass(generateAdapter = true)
    data class LessonData(
        val duration: String = "",
        @Json(name = "lesson_id")
        val lessonId: String =""
    )
}