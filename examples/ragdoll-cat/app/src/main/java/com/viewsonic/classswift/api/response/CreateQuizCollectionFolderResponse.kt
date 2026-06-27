package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateQuizCollectionFolderResponse(
    val id: String = "",
    val name: String = "",
    @Json(name = "org_id")
    val orgId: String = "",
    @Json(name = "user_id")
    val userId: String = ""
)
