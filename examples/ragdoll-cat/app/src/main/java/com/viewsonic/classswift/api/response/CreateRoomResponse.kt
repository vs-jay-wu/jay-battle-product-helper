package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateRoomResponse(
    @Json(name = "data")
    val createRoomData: CreateRoomData
)

@JsonClass(generateAdapter = true)
data class CreateRoomData(
    @Json(name = "room_id")
    val roomId: String = "",
    @Json(name = "room_number")
    val roomNumber: String = "",
    @Json(name = "room_link")
    val roomLink: String = ""
)