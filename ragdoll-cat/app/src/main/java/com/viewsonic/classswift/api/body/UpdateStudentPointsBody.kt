package com.viewsonic.classswift.api.body

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateStudentPointsBody(
    val points: Int = 0
)