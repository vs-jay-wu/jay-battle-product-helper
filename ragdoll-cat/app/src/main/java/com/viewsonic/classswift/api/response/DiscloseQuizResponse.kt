package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.quiz.QuizOption

@JsonClass(generateAdapter = true)
data class DiscloseQuizResponse(
    @Json(name = "data")
    val discloseData: List<Data>
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        val content: String = "",
        @Json(name = "created_at")
        val createdAt: Int = 0,
        @Json(name = "is_ai_answer")
        val isAiAnswer: Boolean = false,
        @Json(name = "is_answer")
        val isAnswer: Boolean = false,
        @Json(name = "option_id")
        val optionId: Int = 0,
        @Json(name = "quiz_id")
        val quizId: String = "",
        val reason: String = "",
        @Json(name = "updated_at")
        val updatedAt: Int = 0
    ) {
        companion object {
            fun fromOption(option: QuizOption): Data {
                return Data(
                    content = option.content,
                    isAiAnswer = option.isAiAnswer,
                    isAnswer = option.isAnswer,
                    optionId = option.optionId,
                    reason = option.reason
                )
            }
        }
    }
}