package com.viewsonic.classswift.ui.widget.quiz.enums

enum class AnswerButtonState {
    CHOSEN,
    NOT_CHOSEN,
    DISABLED;

    companion object {
        fun from(value: Int): AnswerButtonState {
            return when (value) {
                0 -> CHOSEN
                1 -> NOT_CHOSEN
                2 -> DISABLED
                else -> throw IllegalArgumentException("Invalid value for AnswerButtonState: $value")
            }
        }
    }
}