package com.viewsonic.classswift.data.socket.quiz.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnswerData(
    @Json(name = "option_id")
    val optionId: Int = -1,
    @Json(name = "content")
    val content: String = ""
)
