package com.viewsonic.classswift.data.quiz

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuizStandard(
    @Json(name = "id")
    val id: String = "",
    @Json(name = "code")
    val code: String = "",
    @Json(name = "content")
    val content: String = "",
    @Json(name = "domains")
    val domains: List<String> = emptyList(),
    @Json(name = "subject")
    val subject: String = "",
)
