package com.viewsonic.classswift.data.socket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.moshi.MoshiProvider
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject

@JsonClass(generateAdapter = true)
data class ChooseAndRejoinSeatSocketMessage(
    @Json(name = "student_id")
    val studentId: String = "",
    @Json(name = "display_name")
    val displayName: String = "", // 若為訪客時會是空值，需要轉換顯示為訪客
    @Json(name = "serial_number")
    val serialNumber: Int = -1,
    @Json(name = "seat_number")
    val seatNumber: String = "",
    val points: Int = 0,
) {
    companion object {
        private val moshiProvider : MoshiProvider by inject(MoshiProvider::class.java)

        fun fromJSONObject(jsonObject: JSONObject): ChooseAndRejoinSeatSocketMessage {
            return moshiProvider.fromJson(ChooseAndRejoinSeatSocketMessage::class.java, jsonObject.toString(), true)
        }
    }
}
