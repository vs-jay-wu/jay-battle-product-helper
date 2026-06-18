package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiSubjectDisplayNamesResponse(
    @Json(name = "data")
    val aiSubjectDisplayNamesDataList: List<AiSubjectDisplayNamesData> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AiSubjectDisplayNamesData(
    @Json(name = "country")
    val country: String = "",
    @Json(name = "subjects")
    val subjects: List<AiSubjectDisplayNameItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AiSubjectDisplayNameItem(
    @Json(name = "key")
    val key: String = "",
    @Json(name = "display_name")
    val displayName: String = ""
)
