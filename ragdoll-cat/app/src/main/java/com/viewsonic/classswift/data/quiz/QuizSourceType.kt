package com.viewsonic.classswift.data.quiz

enum class QuizSourceType {
    MANUAL, KNSH, QUIZ_COLLECTION, QUIZ_GENERATOR, LESSON_PLANNER, IMPORT_CONTENT, LESSON_INSIGHT;

    companion object {
        fun safeValueOf(value: String?, default: QuizSourceType = MANUAL): QuizSourceType {
            return try {
                // Use `uppercase()` to make the comparison case-insensitive.
                value?.let { QuizSourceType.valueOf(it.uppercase()) } ?: default
            } catch (e: IllegalArgumentException) {
                // The string did not match any enum constant.
                default
            }
        }
    }
}