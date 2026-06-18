package com.viewsonic.classswift.api.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PreSignedUrlResponse(
    var put: String = "",
    var get: String = ""
)