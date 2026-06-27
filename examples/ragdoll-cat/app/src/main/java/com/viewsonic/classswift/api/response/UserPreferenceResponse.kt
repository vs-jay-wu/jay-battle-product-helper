package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserPreferenceResponse(@Json(name = "data") val userPreferenceData: UserPreferenceData = UserPreferenceData() ) {
    @JsonClass(generateAdapter = true)
    data class UserPreferenceData(
        @Json(name = "translation_tool")
        val translationTool: IsOnData = IsOnData(false),
        @Json(name = "leaderboard")
        val leaderboard: IsOnData = IsOnData(false),
        @Json(name = "tutorial")
        val tutorial: IsShownData = IsShownData(false),
    )
}

@JsonClass(generateAdapter = true)
data class IsOnData(@Json(name = "is_on") val isOn: Boolean)
@JsonClass(generateAdapter = true)
data class IsShownData(@Json(name = "is_shown") val isShown: Boolean)