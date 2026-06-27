package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SocketTokenResponse(
    @Json(name = "client_id") val clientID: String,
    @Json(name = "access_token") val socketAccessToken: String
)