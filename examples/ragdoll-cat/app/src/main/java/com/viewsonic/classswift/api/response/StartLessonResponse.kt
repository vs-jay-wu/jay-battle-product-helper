package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StartLessonResponse(
    @Json(name = "data")
    val result: String = "", // it would be "success" if starting is succeeded.
    @Json(name = "result")
    val informationList: List<Information> = emptyList()
) {
    @JsonClass(generateAdapter = true)
    data class Information(
        @Json(name = "created_at")
        val createdAt: Long = 0, // Epoch Time in Seconds
        @Json(name = "updated_at")
        val updatedAt: Long = 0, // Epoch Time in Seconds
        @Json(name = "student_count")
        val studentCount: Int = 0,
        @Json(name = "room_id")
        val roomId: String = "",
        @Json(name = "lesson_id")
        val lessonId: String = "",
        @Json(name = "start_time")
        val startTime: Long = 0, // Epoch Time in Seconds
        @Json(name = "end_time")
        val endTime: Long = 0, // Epoch Time in Seconds
        @Json(name = "is_delete")
        val isDelete: Boolean = false
    )
}