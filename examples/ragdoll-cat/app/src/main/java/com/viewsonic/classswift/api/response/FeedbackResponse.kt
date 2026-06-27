package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeedbackResponse(
    @Json(name = "data")
    val feedbackDataList: List<FeedbackData>
) {
    @JsonClass(generateAdapter = true)
    data class FeedbackData(
        @Json(name = "id")
        val id: String = "",
        @Json(name = "resource_id")
        val resourceId: String = "",
        @Json(name = "user_id")
        val userId: String = "",
        @Json(name = "resource_type")
        val resourceType: String = "",
        @Json(name = "content")
        val content: FeedbackContent = FeedbackContent()
    )

    @JsonClass(generateAdapter = true)
    data class FeedbackContent(
        @Json(name = "helped")
        val isHelped: Boolean = false
    )
}


