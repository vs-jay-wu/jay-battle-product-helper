package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizStandard

@JsonClass(generateAdapter = true)
data class QuizzesInCollectionFolderResponse(
    @Json(name = "data")
    val quizDataList: List<QuizInCollectionData> = emptyList(),
    @Json(name = "meta")
    val metaData: MetaData = MetaData()
) {
    @JsonClass(generateAdapter = true)
    data class QuizInCollectionData(
        @Json(name = "id")
        val id: String = "",
        @Json(name = "created_at")
        val createdAt: Int = 0, // Epoch time in seconds
        @Json(name = "pin")
        val pin: Boolean = false,
        @Json(name = "content")
        val content: String = "",
        @Json(name = "country")
        val country: String = "",
        @Json(name = "subject")
        val subject: String = "",
        @Json(name = "chirp_id")
        val chirpId: String = "",
        @Json(name = "quiz_type")
        val quizType: String = "",
        @Json(name = "option_list")
        val optionList: List<QuizOption> = emptyList(),
        @Json(name = "option_type")
        val optionType: String = "",
        @Json(name = "source_type")
        val sourceType: String = "",
        @Json(name = "short_answer")
        val shortAnswer: ShortAnswer = ShortAnswer(),
        @Json(name = "img_url")
        val imgUrl: String = "",
        @Json(name = "standards")
        val standards: List<QuizStandard> = emptyList()
    )

    @JsonClass(generateAdapter = true)
    data class MetaData(
        @Json(name = "page")
        val page: Int = 0,
        @Json(name = "per_page")
        val perPage: Int = 0,
        @Json(name = "total_items")
        val totalItems: Int = 0,
        @Json(name = "total_pages")
        val totalPages: Int = 0,
    )


    @JsonClass(generateAdapter = true)
    data class ShortAnswer(
        @Json(name = "is_ai_answer")
        val isAiAnswer: Boolean = false,
        val answer: String = ""
    )

}
