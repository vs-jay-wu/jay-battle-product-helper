package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.quiz.QuizStatus

@JsonClass(generateAdapter = true)
data class UpdateQuizStatusResponse(
    @Json(name = "data")
    val quizData: Data = Data()
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "chirp_id")
        val chirpId: String = "",
        @Json(name = "collection_id")
        val collectionId: String = "",
        val content: String = "",
        @Json(name = "created_at")
        val createdAt: Int = 0,
        @Json(name = "end_time")
        val endTime: Int = 0,
        @Json(name = "img_key")
        val imgKey: String = "",
        @Json(name = "lesson_id")
        val lessonId: String = "",
        @Json(name = "option_type")
        val optionType: String = "",
        @Json(name = "quiz_id")
        val quizId: String = "",
        @Json(name = "quiz_type")
        val quizType: String = "",
        val seq: String = "",
        @Json(name = "start_time")
        val startTime: Int = 0,
        val status: QuizStatus = QuizStatus.UNSPECIFIED,
        @Json(name = "updated_at")
        val updatedAt: Int = 0,
        @Json(name = "source_type")
        val sourceType: String = "",
    )
}