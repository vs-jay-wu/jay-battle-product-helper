package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.response.data.QuizAnswerData
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizStatus

@JsonClass(generateAdapter = true)
data class UnclosedQuizResponse(
    @Json(name = "data")
    val unclosedQuizData: Data = Data()
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "ai_short_answer")
        val aiShortAnswer: String = "",
        @Json(name = "chirp_id")
        val chirpId: String = "",
        @Json(name = "end_time")
        val endTime: Int = 0,
        @Json(name = "img_url")
        val imgUrl: String = "",
        @Json(name = "option_list")
        val optionList: List<QuizOption> = emptyList(),
        @Json(name = "option_type")
        val optionType: String = "",
        @Json(name = "content")
        val content: String = "",
        @Json(name = "quiz_id")
        val quizId: String = "",
        @Json(name = "quiz_results")
        val quizResults: List<QuizResults> = emptyList(),
        @Json(name = "quiz_type")
        val quizType: String = "",
        @Json(name = "source_type")
        val sourceType: String = "",
        @Json(name = "start_time")
        val startTime: Int = 0,
        val status: QuizStatus = QuizStatus.UNSPECIFIED,
        @Json(name = "response_type")
        val responseType: String = "",
        @Json(name = "answer_type")
        val answerType: String = ""
    )

    @JsonClass(generateAdapter = true)
    data class QuizResults(
        @Json(name = "answer_data")
        val quizAnswerData: QuizAnswerData = QuizAnswerData.Text(""),
        @Json(name = "display_name")
        val displayName: String = "",
        @Json(name = "serial_number")
        val serialNumber: Int = 0,
        @Json(name = "student_id")
        val studentId: String = "",
        @Json(name = "seat_number")
        val seatNumber: String = "",
    )
}
