package com.viewsonic.classswift.data.info

data class SketchTaskInfo(
    val taskId: String = "",
    /** 個別 task ID 清單（Sprint 20 單題為 size = 1）。 */
    val taskIds: List<String> = emptyList(),
    val sketchCount: Int = 0,
    val previewImageUrl: String = "",
    val totalStudents: Int = 0,
)
