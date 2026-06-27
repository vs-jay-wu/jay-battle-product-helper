package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.response.data.LinkMeta

@JsonClass(generateAdapter = true)
data class CreateBatchQuizResponse(
    @Json(name = "data")
    val data: Data,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "batch_quizzes_id")
        val batchQuizzesId: String,
    )
}