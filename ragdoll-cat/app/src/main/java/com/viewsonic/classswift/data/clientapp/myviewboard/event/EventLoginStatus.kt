package com.viewsonic.classswift.data.clientapp.myviewboard.event

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.BuildConfig

@JsonClass(generateAdapter = true)
data class EventLoginStatus(
    val type: String = MyViewBoardEvent.EVENT_LOGIN_STATUS,
    @Json(name = "app_version")
    val appVersion: String = BuildConfig.VERSION_NAME,
    @Json(name = "payload")
    val payload: EventLoginStatusPayload
)

@JsonClass(generateAdapter = true)
data class EventLoginStatusPayload(
    @Json(name = "status")
    val status: String,
    @Json(name = "reason_code")
    val reasonCode: String = "",
    @Json(name = "reason_message")
    val reasonMessage: String = ""
)
