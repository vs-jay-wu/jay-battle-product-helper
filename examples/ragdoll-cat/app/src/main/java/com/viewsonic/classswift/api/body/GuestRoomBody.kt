package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GuestRoomBody(
    @Json(name = "display_name")
    val displayName: String,
)
