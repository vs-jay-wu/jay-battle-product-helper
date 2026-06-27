package com.viewsonic.classswift.data.spinner
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CandidateStudentInfo(
    val studentId: String = "",
    val seatNumber: String = "",
    val name: String = "",
)