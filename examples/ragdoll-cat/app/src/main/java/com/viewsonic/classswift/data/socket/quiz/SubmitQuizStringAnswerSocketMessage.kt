package com.viewsonic.classswift.data.socket.quiz

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.moshi.MoshiProvider
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

@JsonClass(generateAdapter = true)
class SubmitQuizStringAnswerSocketMessage(
    @Json(name = "quiz_id")
    val quizId: String = "",
    @Json(name = "lesson_id")
    val lessonId: String = "",
    @Json(name = "student_id")
    val studentId: String = "",
    @Json(name = "answer_data")
    val answerData: String = ""
) {
    companion object {
        private val moshiProvider: MoshiProvider by inject(MoshiProvider::class.java)

        fun fromJSONObject(jsonObject: JSONObject): SubmitQuizStringAnswerSocketMessage {
            return moshiProvider.fromJson(SubmitQuizStringAnswerSocketMessage::class.java, jsonObject.toString(), true)
        }
    }
}