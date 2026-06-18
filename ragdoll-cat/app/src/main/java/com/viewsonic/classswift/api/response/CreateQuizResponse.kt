package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.quiz.QuizStatus

@JsonClass(generateAdapter = true)
data class CreateQuizResponse(
    @Json(name = "data")
    val createQuizData: Data
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        val quiz: Quiz = Quiz(),
        @Json(name = "quiz_id")
        val quizId: String = "",
        val result: List<Result> = emptyList(),
        //insert db 後 db return 得到的 list
        val option: List<Option> = emptyList(),
        //code base 組成的 option list , 兩個都是正確的值，目前都有回傳，所以都可以用
        @Json(name = "option_list")
        val optionList: List<Option> = emptyList(),
    )

    @JsonClass(generateAdapter = true)
    data class Quiz(
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
        val updatedAt: Int = 0
    )

    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "created_at")
        val createdAt: Int = 0,
        val id: String = "",
        @Json(name = "option_list")
        val optionList: List<Int> = emptyList(),//Student's answer (a list of option_id)
        @Json(name = "quiz_id")
        val quizId: String = "",
        @Json(name = "student_id")
        val studentId: String = "",
        @Json(name = "updated_at")
        val updatedAt: Int = 0
    )

    @JsonClass(generateAdapter = true)
    data class Option(
        val content: String = "",
        @Json(name = "is_ai_answer")
        val isAiAnswer: Boolean = false,
        @Json(name = "is_answer")
        val isAnswer: Boolean = false,
        @Json(name = "option_id")
        val optionId: Int = 0,
        @Json(name = "quiz_id")
        val quizId: String = "",
        val reason: String = ""
    )
}
