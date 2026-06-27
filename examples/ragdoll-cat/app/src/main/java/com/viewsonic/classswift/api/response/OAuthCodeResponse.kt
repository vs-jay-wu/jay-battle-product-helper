package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OAuthCodeResponse(
    @Json(name = "auth_code")
    val oAuthCode: String = ""
)