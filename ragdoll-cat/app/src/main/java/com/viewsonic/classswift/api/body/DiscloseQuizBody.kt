package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.quiz.QuizType

@JsonClass(generateAdapter = true)
data class DiscloseQuizBody(
    @Json(name = "correct_options")
    val correctOptions: List<Int>,
    @Json(name = "quiz_type")
    val quizType: QuizType
)