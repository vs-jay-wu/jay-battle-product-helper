package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BatchCreateTasksResponse(
    @Json(name = "data")
    val data: Data,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "batch_task_id")
        val batchTaskId: String,
        @Json(name = "task_ids")
        val taskIds: List<String>,
    )
}
