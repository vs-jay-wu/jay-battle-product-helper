package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChallengeCodeResponse(
    @Json(name = "code_challenge") val codeChallenge: String,
    @Json(name = "code_challenge_method") val codeChallengeMethod: String
)