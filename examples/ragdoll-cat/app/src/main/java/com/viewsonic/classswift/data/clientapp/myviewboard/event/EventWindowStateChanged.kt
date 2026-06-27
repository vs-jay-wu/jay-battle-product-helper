package com.viewsonic.classswift.data.clientapp.myviewboard.event

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.windowframework.core.enums.WindowTag

@JsonClass(generateAdapter = true)
data class EventWindowStateChanged(
    val type: String = MyViewBoardEvent.EVENT_WINDOW_STATE_CHANGED,
    @Json(name = "app_version")
    val appVersion: String = BuildConfig.VERSION_NAME,
    @Json(name = "payload")
    val payload: EventWindowStateChangedPayload
)

@JsonClass(generateAdapter = true)
data class EventWindowStateChangedPayload(
    @Json(name = "window_tag")
    val windowTag: WindowTag,
    @Json(name = "state")
    val state: State,
    @Json(name = "should_toggle_off")
    val shouldToggleOff: Boolean = false
) {
    enum class State{
        VISIBLE,
        HIDDEN,
        TEMPORARILY_HIDDEN,
        MINIMIZED,
        CLOSED
    }
}


