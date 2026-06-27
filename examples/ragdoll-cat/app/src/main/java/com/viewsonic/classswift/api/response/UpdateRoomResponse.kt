package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateRoomResponse(
    @Json(name = "data")
    val updateData: RoomsData
)

