package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.info.BatchQuizSummaryInfo
import com.viewsonic.classswift.data.quiz.QuizType

@JsonClass(generateAdapter = true)
data class GetBatchQuizSummaryResponse(
    @Json(name = "data")
    val data: Data
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "batch_quizzes_id")
        val batchQuizzesId: String,
        @Json(name = "lesson_id")
        val lessonId: String,
        @Json(name = "quizzes")
        val quizzes: List<QuizSummary>
    )

    @JsonClass(generateAdapter = true)
    data class QuizSummary(
        @Json(name = "collection_id")
        val collectionId: String,
        @Json(name = "quiz_id")
        val quizId: String,
        @Json(name = "seq")
        val sequence: Int,
        @Json(name = "quiz_type")
        val quizType: String,
        @Json(name = "correct_student_ids")
        val correctStudentIds: List<String>,
        @Json(name = "incorrect_student_ids")
        val incorrectStudentIds: List<String>,
        @Json(name = "no_answer_student_ids")
        val noAnswerStudentIds: List<String>
    ){
        fun toBatchQuizSummaryInfo(): BatchQuizSummaryInfo {
            return BatchQuizSummaryInfo(
                collectionId = collectionId,
                quizId = quizId,
                sequence = sequence,
                quizType = QuizType.safeValueOf(quizType, QuizType.UNSPECIFIED),
                correctStudentIds = correctStudentIds,
                incorrectStudentIds = incorrectStudentIds,
                noAnswerStudentIds = noAnswerStudentIds,
            )
        }
    }
}