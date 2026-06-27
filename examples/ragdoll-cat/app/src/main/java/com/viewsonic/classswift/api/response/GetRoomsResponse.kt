package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetRoomsResponse(
    @Json(name = "data")
    val roomsData: List<RoomsData>
)

@JsonClass(generateAdapter = true)
data class RoomsData(
    @Json(name = "display_name")
    val displayName: String = "",
    val icon: Int = -1,
    @Json(name = "room_id")
    val roomId: String = "",
    @Json(name = "room_number")
    val roomNumber: String = "",
    val status: String = "",
    @Json(name = "student_count")
    val studentCount: Int = -1,
    @Json(name = "origin_id")
    val originId: String = "",
    @Json(name = "origin_type")
    val originType: String = "",
    @Json(name = "room_link")
    val roomLink: String = "",
    @Json(name = "room_short_url")
    val roomShortUrl: String = "",
    @Json(name = "login_type")
    val loginType: String = ""
)


