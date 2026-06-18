package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LessonResponse(
    @Json(name = "student_attend")
    val attendedStudentDetailList: List<AttendedStudentDetail> = emptyList(),
    @Json(name = "student_list")
    val defaultStudentDetailList: List<DefaultStudentDetail> = emptyList(),
    @Json(name = "student_count")
    val studentCount: Int = 0,
    @Json(name = "start_time") // Epoch Time in Seconds
    val startTimeInSeconds: Int = 0,
    @Json(name = "end_time") // Epoch Time in Seconds
    val endTimeInSeconds: Int = 0,
    @Json(name = "room_id") // Also as known as class
    val roomId: String = ""
) {

    @JsonClass(generateAdapter = true)
    data class AttendedStudentDetail(
        @Json(name = "serial_number")
        val serialNumber: Int = -1,
        @Json(name = "student_id")
        val studentId: String = "",
        @Json(name = "display_name")
        val displayName: String = "",
        @Json(name = "points")
        val points: Int = -1,
        @Json(name = "status") // Should be ACTIVE or INACTIVE
        val status: String = "",
    )

    @JsonClass(generateAdapter = true)
    data class DefaultStudentDetail(
        @Json(name = "serial_number")
        val serialNumber: Int = -1,
        @Json(name = "seat_number")
        val seatNumber: String = "",
        @Json(name = "display_email")
        val displayEmail: String = "",
        @Json(name = "display_name")
        val displayName: String = "",
    )

}