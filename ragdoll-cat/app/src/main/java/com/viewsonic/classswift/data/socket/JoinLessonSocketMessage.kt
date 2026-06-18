package com.viewsonic.classswift.data.socket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class JoinLessonSocketMessage(
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "lesson_id")
    val lessonId: String,
    val role: String = "teacher/student"
) {
    fun toJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("user_id", userId)
        jsonObject.put("lesson_id", lessonId)
        jsonObject.put("role", role)
        return jsonObject
    }
}
