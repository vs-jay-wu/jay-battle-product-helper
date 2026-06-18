package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.info.FillUserInfo

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "data")
    val loginData: LoginData = LoginData(accessToken = "", idToken = "", refreshToken = "")
)

@JsonClass(generateAdapter = true)
data class LoginData(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "id_token") val idToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "is_filled_info") val isFilledInfo: Boolean = false,
    @Json(name = "is_chirp_ai_consent") val isChirpAIConsent: Boolean = false,
    @Json(name = "is_consent") val isConsent: Boolean = false,
    @Json(name = "country") val country: String = "",
    @Json(name = "add_ons") val addOns: List<String> = listOf()
)

fun LoginData.toFillUserInfo(): FillUserInfo {
    // true代表有填過，false代表沒有填過
    return FillUserInfo(!isFilledInfo, !isChirpAIConsent, !isConsent, country)
}
