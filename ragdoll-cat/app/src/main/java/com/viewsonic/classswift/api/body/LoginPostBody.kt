package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginWithMvbTokenPostBody(
    @Json(name ="token")
    var token: String,
    @Json(name ="client")
    val client: String = "ANDROID"
)

@JsonClass(generateAdapter = true)
data class LoginWithRefreshTokenPostBody(
    @Json(name ="redirect_uri")
    var redirectUri: String,
    @Json(name ="refresh_token")
    var refreshToken: String = "",
    @Json(name ="client")
    val client: String = "ANDROID"
)

@JsonClass(generateAdapter = true)
data class LoginPostNoTokenBody(
    @Json(name = "code")
    var code: String,
    @Json(name ="code_challenge")
    var codeChallenge: String,
    @Json(name ="redirect_uri")
    var redirectUri: String,
    @Json(name ="client")
    val client: String = "ANDROID"
)
