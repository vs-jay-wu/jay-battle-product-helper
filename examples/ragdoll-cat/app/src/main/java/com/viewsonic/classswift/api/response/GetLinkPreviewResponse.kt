package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetLinkPreviewResponse(
    @Json(name = "title")
    val title: String = "",
    @Json(name = "description")
    val description: String = "",
    @Json(name = "site_name")
    val siteName: String = "",
    @Json(name = "image")
    val imageUrl: String = ""
)