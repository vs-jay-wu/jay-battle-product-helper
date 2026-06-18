package com.viewsonic.classswift.data.socket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class SetLessonIdSocketMessage(
    @Json(name = "lesson_id")
    val lessonId: String
) {
    fun toJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("lesson_id", lessonId)
        return jsonObject
    }
}

