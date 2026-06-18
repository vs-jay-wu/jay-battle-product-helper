package com.viewsonic.classswift.data.clientapp.myviewboard.event

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.BuildConfig

@JsonClass(generateAdapter = true)
data class EventCommandResult(
    val type: String = MyViewBoardEvent.EVENT_COMMAND_RESULT,
    @Json(name = "request_id")
    val requestId: String,
    @Json(name = "response_to")
    val responseTo: String,
    @Json(name = "app_version")
    val appVersion: String = BuildConfig.VERSION_NAME,
    @Json(name = "payload")
    val payload: EventCommandResultPayload
)

@JsonClass(generateAdapter = true)
data class EventCommandResultPayload(
    @Json(name = "status")
    val status: String,
    @Json(name = "reason_code")
    val reasonCode: String = "",
    @Json(name = "reason_message")
    val reasonMessage: String = ""
)
