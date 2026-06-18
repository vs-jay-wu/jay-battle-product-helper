package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetGuestRoomResponse(
    @Json(name = "data")
    val roomData: GuestRoomData
)

@JsonClass(generateAdapter = true)
data class GuestRoomData(
    @Json(name = "room_id")
    val roomId: String = "",
    @Json(name = "room_number")
    val roomNumber: String = "",
    @Json(name = "room_link")
    val roomLink: String = "",
    @Json(name = "room_short_url")
    val roomShortUrl: String = "",
)
