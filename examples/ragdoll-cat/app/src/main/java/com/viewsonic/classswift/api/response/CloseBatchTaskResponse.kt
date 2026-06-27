package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response for `PUT /api/v3/lessons/{lesson_id}/tasks/batch_tasks/{batch_task_id}`
 * (operationId: `close_batch_task`).
 *
 * VSFT-8453/8454：Sketch Response 結果頁 [X] 關窗時呼叫，把整批 batch task 設為 CLOSED。
 * `end_time` 在 batch 關閉後才有值（nullable）；`closed_task_ids` 為實際被關閉的 task id。
 */
@JsonClass(generateAdapter = true)
data class CloseBatchTaskResponse(
    @Json(name = "data")
    val data: Data = Data(),
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "batch_task_id")
        val batchTaskId: String = "",
        @Json(name = "status")
        val status: String = "",
        @Json(name = "end_time")
        val endTime: Long? = null,
        @Json(name = "closed_task_ids")
        val closedTaskIds: List<String> = emptyList(),
    )
}
