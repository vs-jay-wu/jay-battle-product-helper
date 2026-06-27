package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetLatestBatchQuizResponse(
    @Json(name = "data")
    val data: Data
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "batch_quizzes_id")
        val batchQuizzesId: String,
        @Json(name = "lesson_id")
        val lessonId: String,
        @Json(name = "start_time")
        val startTime: Long,
        @Json(name = "status")
        val status: String,
        @Json(name = "quizzes")
        val quizzes: List<Quiz>,
        @Json(name = "submitted_student_ids")
        val submittedStudentIds: List<String>
    )

    @JsonClass(generateAdapter = true)
    data class Quiz(
        @Json(name = "quiz_id")
        val quizId: String,
        @Json(name = "quiz_type")
        val quizType: String,
        @Json(name = "seq")
        val seq: Int,
        @Json(name = "content")
        val content: String,
        @Json(name = "img_url")
        val imgUrl: String?,
        @Json(name = "option_type")
        val optionType: String,
        @Json(name = "source_type")
        val sourceType: String,
        @Json(name = "status")
        val status: String,
        @Json(name = "option_list")
        val optionList: List<Option>,
        @Json(name = "ai_short_answer")
        val aiShortAnswer: String?,
        @Json(name = "chirp_id")
        val chirpId: String?,
        @Json(name = "collection_id")
        val collectionId: String
    )

    @JsonClass(generateAdapter = true)
    data class Option(
        @Json(name = "option_id")
        val optionId: Int,
        @Json(name = "content")
        val content: String,
        @Json(name = "is_answer")
        val isAnswer: Boolean
    )
}