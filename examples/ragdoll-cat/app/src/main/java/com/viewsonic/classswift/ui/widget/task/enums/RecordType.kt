package com.viewsonic.classswift.ui.widget.task.enums

enum class RecordType(val code: String) {
    CONTENT("SCREENSHOT"),
    LINK("LINK"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromString(status: String): RecordType {
            return when (status.uppercase()) {
                "SCREENSHOT" -> CONTENT
                "LINK" -> LINK
                else -> UNKNOWN
            }
        }
    }
}