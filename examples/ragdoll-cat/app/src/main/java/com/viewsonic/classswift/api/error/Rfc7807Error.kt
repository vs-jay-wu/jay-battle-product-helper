package com.viewsonic.classswift.api.error

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Rfc7807Error (
    val type: String = "",
    val title: String = "",
    val status: Int = -1,
    val detail: String = "",
    val instance: String = "",
    val additional: String = ""
)