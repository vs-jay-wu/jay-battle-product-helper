package com.viewsonic.classswift.data.quiz

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuizOption(
    val content: String = "",//*AI
    @Json(name = "is_ai_answer")
    val isAiAnswer: Boolean = false, //For quiz not from ai, this should be false
    @Json(name = "is_answer")
    val isAnswer: Boolean = false,
    @Json(name = "option_id")
    val optionId: Int, //The id of the option, start from 1
    val reason: String = "" //*AI
) {
    fun isCorrectAnswer(): Boolean {
        return isAnswer || isAiAnswer
    }
}