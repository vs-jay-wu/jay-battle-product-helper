package com.viewsonic.classswift.data.socket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.moshi.MoshiProvider
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject

@JsonClass(generateAdapter = true)
data class BatchQuizzesStudentSubmittedSocketMessage(
    @Json(name = "batch_quizzes_id")
    val batchQuizzesId: String = "",
    @Json(name = "student_id")
    val studentId: String = ""
) {
    companion object {
        private val moshiProvider : MoshiProvider by inject(MoshiProvider::class.java)

        fun fromJSONObject(jsonObject: JSONObject): BatchQuizzesStudentSubmittedSocketMessage {
            return moshiProvider.fromJson(BatchQuizzesStudentSubmittedSocketMessage::class.java, jsonObject.toString(), true)
        }
    }
}