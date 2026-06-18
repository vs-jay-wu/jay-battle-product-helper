package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateRoomBody(
    @Json(name = "display_name")
    val displayName: String,
    val icon: Int,
    @Json(name = "org_id")
    val orgId: String,
    @Json(name = "student_count")
    val studentCount: Int,
    @Json(name = "teacher_id")
    val teacherId: String
)