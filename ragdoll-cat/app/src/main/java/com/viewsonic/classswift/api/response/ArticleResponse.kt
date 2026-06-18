package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArticleResponse(
    @Json(name = "data")
    val articleData: ArticleData
) {
    @JsonClass(generateAdapter = true)
    data class ArticleData(
        @Json(name = "eula_id")
        val eulaID: String = "",
        @Json(name = "privacy_id")
        val privacyID: String = "",
        @Json(name = "service_id")
        val serviceID: String = "",
        @Json(name = "chirp_ai_id")
        val chirpAIID: String = "",
        @Json(name = "sla_id")
        val slaID: String = ""
    )
}


