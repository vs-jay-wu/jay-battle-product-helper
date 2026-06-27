package com.viewsonic.classswift.data.spinner

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpinnerStudentInfo(
    val data: List<CandidateStudentInfo> = emptyList<CandidateStudentInfo>()
)
