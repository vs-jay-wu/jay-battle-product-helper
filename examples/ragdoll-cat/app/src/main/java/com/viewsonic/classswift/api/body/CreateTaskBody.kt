package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateTaskBody(
    @Json(name = "assign")
    var assign: String,
    @Json(name = "img_url")
    var imageUrl: String? = null,
    @Json(name = "link_url")
    var linkUrl: String = "",
    @Json(name = "task_type")
    var taskType: String,
    @Json(name = "seat_number_list")
    val seatNumberList: List<Int>,
    @Json(name = "link_meta")
    val linkMeta: LinkMeta? = null
)

@JsonClass(generateAdapter = true)
data class LinkMeta(
    @Json(name = "title")
    val title: String,
    @Json(name = "description")
    val description: String,
    @Json(name = "site_name")
    val siteName: String,
    @Json(name = "image")
    val image: String
)
