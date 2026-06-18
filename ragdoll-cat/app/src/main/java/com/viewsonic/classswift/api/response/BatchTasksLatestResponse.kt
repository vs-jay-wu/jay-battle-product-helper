package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.enum.SketchAnswerStatus
import com.viewsonic.classswift.data.info.SketchStudentCardInfo
import com.viewsonic.classswift.data.info.SketchTaskInfo

/**
 * Response for `GET /api/v3/lessons/{lesson_id}/tasks/batch_tasks/latest`
 * (operationId: `get_latest_unclosed_batch_task`). Mirrors the live OpenAPI
 * contract — `data` is nullable when no batch task is currently in flight,
 * and `student_id` / `display_name` are nullable per the StudentInfo schema.
 */
@JsonClass(generateAdapter = true)
data class BatchTasksLatestResponse(
    @Json(name = "data")
    val batchData: BatchTasksLatestData? = null,
)

@JsonClass(generateAdapter = true)
data class BatchTasksLatestData(
    @Json(name = "batch_task_id")
    val batchTaskId: String = "",
    @Json(name = "lesson_id")
    val lessonId: String = "",
    @Json(name = "start_time")
    val startTime: Long = 0L,
    val status: String = "IN_PROGRESS",
    @Json(name = "tasks_count")
    val tasksCount: Int = 0,
    @Json(name = "task_ids")
    val taskIds: List<String> = emptyList(),
    val students: List<BatchTasksLatestStudent> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BatchTasksLatestStudent(
    @Json(name = "student_id")
    val studentId: String? = null,
    @Json(name = "display_name")
    val displayName: String? = null,
    @Json(name = "submitted_count")
    val submittedCount: Int = 0,
)

fun BatchTasksLatestResponse.toSketchTaskInfo(): SketchTaskInfo {
    val data = batchData ?: return SketchTaskInfo()
    return SketchTaskInfo(
        taskId = data.batchTaskId,
        taskIds = data.taskIds,
        sketchCount = data.tasksCount,
        previewImageUrl = "",
        totalStudents = data.students.size,
    )
}

fun BatchTasksLatestResponse.toSketchStudentCardInfos(): List<SketchStudentCardInfo> {
    val data = batchData ?: return emptyList()
    val tasksCount = data.tasksCount.coerceAtLeast(1)
    return data.students
        .filter { !it.studentId.isNullOrBlank() }
        .mapIndexed { index, student ->
            SketchStudentCardInfo(
                studentId = student.studentId.orEmpty(),
                serialNumber = index + 1,
                displaySeatNumber = "",
                displayName = student.displayName.orEmpty(),
                status = if (student.submittedCount >= tasksCount) {
                    SketchAnswerStatus.SUBMITTED
                } else {
                    SketchAnswerStatus.NOT_SUBMITTED
                },
            )
        }
}
