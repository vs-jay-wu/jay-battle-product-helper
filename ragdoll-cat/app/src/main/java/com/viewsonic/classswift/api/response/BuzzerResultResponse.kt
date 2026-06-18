package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BuzzerResultResponse(
    @Json(name = "data")
    val buzzerResultData: Data = Data()
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "record_id")
        val recordId: String = "",
        @Json(name = "lesson_id")
        val lessonId: String = "",
        @Json(name = "student_id")
        val studentId: String = "",
        @Json(name = "race_student_seat_number")
        val raceStudentSeatNumber: String = "",
        @Json(name = "race_student_serial_number")
        val raceStudentSerialNumber: Int = 0
    )
}

