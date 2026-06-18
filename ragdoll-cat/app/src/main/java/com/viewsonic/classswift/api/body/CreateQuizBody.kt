package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.response.QuizzesInCollectionFolderResponse
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizSourceType
import com.viewsonic.classswift.data.quiz.QuizType

@JsonClass(generateAdapter = true)
data class CreateQuizBody(
    @Json(name = "img_url")
    val imgUrl: String?, //AI quiz does not have img_url
    @Json(name = "option_type")
    val optionType: QuizOptionType,
    @Json(name = "quiz_type")
    val quizType: QuizType,
    @Json(name = "source_type")
    val sourceType: QuizSourceType,
    @Json(name = "option_list")
    val quizOptionList: List<QuizOption>,
    @Json(name = "short_answer")
    val shortAnswer: QuizzesInCollectionFolderResponse.ShortAnswer = QuizzesInCollectionFolderResponse.ShortAnswer(),
    @Json(name = "chirp_id")
    val chirpId: String? = null, // *AI - The chirp_id given by quiz generator
    @Json(name = "collection_id")
    val collectionId: String? = null, //The id from quiz collection (If this quiz is from quiz collection / preset quiz, else pass "null")
    val content: String? = null // *AI
) {
    companion object {
        fun fromQuizInCollectionData(quizInCollectionData: QuizzesInCollectionFolderResponse.QuizInCollectionData): CreateQuizBody {
            return CreateQuizBody(
                collectionId = quizInCollectionData.id,
                imgUrl = quizInCollectionData.imgUrl,
                optionType = QuizOptionType.safeValueOf(quizInCollectionData.optionType),
                quizType = QuizType.safeValueOf(quizInCollectionData.quizType),
                sourceType = QuizSourceType.safeValueOf(quizInCollectionData.sourceType),
                quizOptionList = quizInCollectionData.optionList,
                shortAnswer = quizInCollectionData.shortAnswer,
                chirpId = quizInCollectionData.chirpId,
                content = quizInCollectionData.content
            )
        }
    }
}