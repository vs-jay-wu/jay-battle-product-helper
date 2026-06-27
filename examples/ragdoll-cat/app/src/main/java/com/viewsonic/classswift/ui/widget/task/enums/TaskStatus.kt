package com.viewsonic.classswift.ui.widget.task.enums

enum class TaskStatus(val code: String) {
    UNKNOWN("UNKNOWN"),
    CLOSED("CLOSED"),
    IN_PROGRESS("IN_PROGRESS");

    companion object {
        fun fromString(status: String): TaskStatus {
            return when (status.uppercase()) {
                "IN_PROGRESS" -> IN_PROGRESS
                "CLOSED" -> CLOSED
                else -> UNKNOWN
            }
        }
    }
}