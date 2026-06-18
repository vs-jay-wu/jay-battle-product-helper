package com.viewsonic.classswift.api.response.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LinkMeta(
    @Json(name = "title")
    val title: String = "",
    @Json(name = "description")
    val description: String = "",
    @Json(name = "site_name")
    val siteName: String = "",
    @Json(name = "image")
    val image: String = ""
) {
    fun isEmptyLinkMeta(): Boolean {
        return title == "" &&
                description == "" &&
                siteName == "" &&
                image == ""
    }
}