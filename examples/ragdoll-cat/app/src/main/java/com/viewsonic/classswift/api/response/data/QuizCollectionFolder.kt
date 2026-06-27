package com.viewsonic.classswift.api.response.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuizCollectionFolder(
    @Json(name = "id")
    val id: String = "",
    @Json(name = "name")
    val name: String = "",
    @Json(name = "resource_path")
    val resourcePath: String = "",
    @Json(name = "is_default")
    val isDefault: Boolean = false
)