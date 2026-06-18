package com.viewsonic.classswift.data.clientapp.myviewboard.event

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.BuildConfig

@JsonClass(generateAdapter = true)
data class EventMissionStatus(
    val type: String = MyViewBoardEvent.EVENT_MISSION_STATUS,
    @Json(name = "app_version")
    val appVersion: String = BuildConfig.VERSION_NAME,
    @Json(name = "payload")
    val payload: EventMissionStatusPayload
)

@JsonClass(generateAdapter = true)
data class EventMissionStatusPayload(
    @Json(name = "mission_type")
    val missionType: String,
    @Json(name = "status")
    val status: Status
) {
    enum class Status {
        ONGOING,
        MINIMIZED,
        CLOSED
    }
}
