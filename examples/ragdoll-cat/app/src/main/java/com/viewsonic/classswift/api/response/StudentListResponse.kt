package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StudentListResponse(
    @Json(name = "student_attend")
    val attendedStudentDetailList: List<AttendedStudentDetail> = emptyList(),
) {
    @JsonClass(generateAdapter = true)
    data class AttendedStudentDetail(
        @Json(name = "serial_number")
        val serialNumber: Int = -1,
        @Json(name = "seat_number")
        val seatNumber: String = "",
        @Json(name = "display_name")
        val displayName: String = "",
        @Json(name = "user_type")
        val userType: String = "",
        @Json(name = "student_id")
        val studentId: String = "",
        @Json(name = "status") // Should be ACTIVE or INACTIVE
        val status: String = "",
        @Json(name = "points")
        val points: Int = 0,
        @Json(name = "group_id")
        val groupId: Int = 1,
    )
}