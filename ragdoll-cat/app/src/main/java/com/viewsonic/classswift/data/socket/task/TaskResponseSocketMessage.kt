package com.viewsonic.classswift.data.socket.task

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.moshi.MoshiProvider
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class TaskResponseSocketMessage(
    @Json(name = "task_id")
    val taskId: String = "",
    @Json(name = "student_id")
    val studentId: String = "",
    @Json(name = "img_url")
    val imgUrl: String = "",
    @Json(name = "version")
    val version: Int = 0
) {
    companion object {
        private val moshiProvider: MoshiProvider by inject(MoshiProvider::class.java)
        fun fromJSONObject(jsonObject: JSONObject): TaskResponseSocketMessage {
            return moshiProvider.fromJson(
                TaskResponseSocketMessage::class.java,
                jsonObject.toString(),
                true
            )
        }
    }
}