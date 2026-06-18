package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateRoomBody(
    @Json(name = "display_name")
    val displayName: String,
    val icon: Int = 0
)