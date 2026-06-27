package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.enum.MissionType

@JsonClass(generateAdapter = true)
data class UnclosedMissionsResponse(
    @Json(name = "data")
    val data: Data = Data()
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "mission_type")
        val missionType: MissionType = MissionType.NONE
    )
}

