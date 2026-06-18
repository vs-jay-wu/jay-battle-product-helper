package com.viewsonic.classswift.data.quiz

enum class QuizOptionType {
    TRUE_FALSE, NUMBER, ALPHABET, NO_OPTION;
    companion object {
        fun safeValueOf(value: String?, default: QuizOptionType = NO_OPTION): QuizOptionType {
            return try {
                value?.let { QuizOptionType.valueOf(it.uppercase()) } ?: default
            } catch (e: IllegalArgumentException) {
                default
            }
        }
    }
}
