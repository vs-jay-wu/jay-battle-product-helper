package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateBatchQuizBody(
    @Json(name = "quizzes")
    val quizzes: List<CreateQuizBody> = listOf()
)