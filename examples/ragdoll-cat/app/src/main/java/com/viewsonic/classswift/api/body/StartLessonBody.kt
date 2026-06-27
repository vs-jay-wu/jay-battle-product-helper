package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StartLessonBody(
    @Json(name = "is_start")
    var isStart: Boolean = true
)