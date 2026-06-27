package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TeacherStateResponse(
    @Json(name = "data")
    val teacherStateData: TeacherStateData
) {
    @JsonClass(generateAdapter = true)
    data class TeacherStateData(
        //有重複登入
        @Json(name ="is_multi_login")
        val isMultiLogin: Boolean = false,
        //老師被admin從後台踢掉
        @Json(name = "is_cooldown")
        val isCooldown: Boolean = false,
        //該組織購買的concurrent數量已滿
        @Json(name = "is_full")
        val isFull: Boolean = false
    )
}
