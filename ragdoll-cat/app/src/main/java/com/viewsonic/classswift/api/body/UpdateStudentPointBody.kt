package com.viewsonic.classswift.api.body

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateStudentPointBody(
    val point: Int = 0
)