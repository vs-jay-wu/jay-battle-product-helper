package com.viewsonic.classswift.data.socket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.moshi.MoshiProvider
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject

@JsonClass(generateAdapter = true)
data class BuzzerResultSocketMessage(
    @Json(name = "student_id")
    val studentId: String = "",
    @Json(name = "seat_number")
    val seatNumber: String = "",
    @Json(name = "serial_number")
    val serialNumber: Int = 0
) {
    companion object {
        private val moshiProvider : MoshiProvider by inject(MoshiProvider::class.java)

        fun fromJSONObject(jsonObject: JSONObject): BuzzerResultSocketMessage {
            return moshiProvider.fromJson(BuzzerResultSocketMessage::class.java, jsonObject.toString(), true)
        }
    }
}