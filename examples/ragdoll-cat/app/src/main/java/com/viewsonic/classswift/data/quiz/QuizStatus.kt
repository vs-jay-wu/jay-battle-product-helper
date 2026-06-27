package com.viewsonic.classswift.data.quiz

enum class QuizStatus {
    UNSPECIFIED,
    OPEN,
    DISCLOSED,
    CANCEL,
    FINISH,
    CLOSE;

    companion object {
        fun safeValueOf(value: String?, default: QuizStatus = QuizStatus.UNSPECIFIED): QuizStatus {
            return try {
                // Use `uppercase()` to make the comparison case-insensitive.
                value?.let { QuizStatus.valueOf(it.uppercase()) } ?: default
            } catch (e: IllegalArgumentException) {
                // The string did not match any enum constant.
                default
            }
        }
    }
}