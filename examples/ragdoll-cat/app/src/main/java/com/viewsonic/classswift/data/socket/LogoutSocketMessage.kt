package com.viewsonic.classswift.data.socket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class LogoutSocketMessage(
    @Json(name = "user_id")
    val userID: String = "",
    @Json(name = "org_id")
    val orgID: String = "",
) {
    fun toJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("user_id", userID)
        jsonObject.put("org_id", orgID)
        return jsonObject
    }
}
