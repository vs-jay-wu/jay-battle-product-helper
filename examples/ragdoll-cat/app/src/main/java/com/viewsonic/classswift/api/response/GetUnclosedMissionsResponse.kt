package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetUnclosedMissionsResponse(
    @Json(name = "data")
    val data: Data
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "mission_type")
        val missionType: String = ""
    )
}

