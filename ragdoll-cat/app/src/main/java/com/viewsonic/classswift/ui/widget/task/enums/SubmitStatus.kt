package com.viewsonic.classswift.ui.widget.task.enums

enum class SubmitStatus(val code: String) {
    UNSUBMITTED("UNSUBMITTED"),
    RESPONSE("RESPONSE"),
    GRADED("GRADED"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromCode(code: String): SubmitStatus {
            return SubmitStatus.entries.find { it.code == code } ?: UNKNOWN
        }
    }

    /**
     * Checks if the status is either RESPONSE or GRADED.
     */
    fun isSaveAndSendStatus(): Boolean {
        return this == RESPONSE || this == GRADED
    }

}