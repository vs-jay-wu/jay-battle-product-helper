package com.viewsonic.classswift.data.socket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.moshi.MoshiProvider
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject

@JsonClass(generateAdapter = true)
data class ReleaseSeatSocketMessage(
    @Json(name = "display_name")
    val displayName: String = "",
    @Json(name = "serial_number")
    val serialNumber: Int = -1,
    @Json(name = "seat_number")
    val seatNumber: String = ""
) {
    companion object {
        private val moshiProvider : MoshiProvider by inject(MoshiProvider::class.java)

        fun fromJSONObject(jsonObject: JSONObject): ReleaseSeatSocketMessage {
            return moshiProvider.fromJson(ReleaseSeatSocketMessage::class.java, jsonObject.toString(), true)
        }
    }
}
