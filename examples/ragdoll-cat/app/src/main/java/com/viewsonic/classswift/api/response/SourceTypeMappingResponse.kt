package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SourceTypeMappingResponse(
    @Json(name = "data")
    val sourceTypeMappingDataList: List<SourceTypeMappingData> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SourceTypeMappingData(
    @Json(name = "origin")
    val origin: String = "",
    @Json(name = "source_type")
    val sourceTypeList: List<String> = emptyList()
)
