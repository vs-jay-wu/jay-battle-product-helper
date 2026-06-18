package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrganizationPermissionResponse(
    @Json(name = "data")
    val permissionData: PermissionData
) {
    @JsonClass(generateAdapter = true)
    data class PermissionData(
        //老師被admin從後台踢掉
        @Json(name = "is_cooldown")
        val isCooldown: Boolean,
        //該組織購買的concurrent數量已滿
        @Json(name = "is_full")
        val isFull: Boolean
    )
}