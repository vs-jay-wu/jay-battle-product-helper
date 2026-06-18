package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BatchCreateTasksBody(
    @Json(name = "tasks")
    val tasks: List<CreateTaskBody>,
)
