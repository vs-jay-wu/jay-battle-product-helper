package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateQuizCollectionFolderBody(
    @Json(name = "org_id")
    val orgId: String,
    val name: String
)