package com.viewsonic.classswift.data.socket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject

@JsonClass(generateAdapter = true)
class SelectStudentSocketMessage(
    @Json(name = "student_id")
    val studentID: String = ""
) {
    fun toJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("student_id", studentID)
        return jsonObject
    }
}

