package com.viewsonic.classswift.api.response


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class MaintenanceAnnouncementsResponse(
    @Json(name = "en")
    val en: LanguageContent = LanguageContent(),
    @Json(name = "zh")
    val zh: LanguageContent = LanguageContent()
) {
    @JsonClass(generateAdapter = true)
    data class LanguageContent(
        @Json(name = "phases")
        val phases: Phases = Phases(),
        @Json(name = "time_variables")
        val timeVariablesJSONObject: JSONObject = JSONObject(),
        @Json(name = "variables")
        val variablesJSONObject: JSONObject = JSONObject()
    ) {
        @JsonClass(generateAdapter = true)
        data class Phases(
            @Json(name = "early")
            val early: Phase = Phase(),
            @Json(name = "ongoing")
            val ongoing: Phase = Phase(),
            @Json(name = "recent")
            val recent: Phase = Phase(),
        ) {
            @JsonClass(generateAdapter = true)
            data class Phase(
                @Json(name = "content")
                val content: Content = Content(),
                @Json(name = "showtime")
                val showtime: String = "" // Time is formatted in ISO 8601 format. Example: 2025-09-14T09:30:00.000Z
            ) {
                @JsonClass(generateAdapter = true)
                data class Content(
                    @Json(name = "body")
                    val body: List<String> = listOf(),
                    @Json(name = "title")
                    val title: String = ""
                )
            }
        }
    }
}