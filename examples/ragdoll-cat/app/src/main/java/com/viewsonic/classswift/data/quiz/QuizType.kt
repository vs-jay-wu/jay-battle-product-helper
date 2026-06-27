package com.viewsonic.classswift.data.quiz

import android.content.Context
import com.viewsonic.classswift.R
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerType

enum class QuizType {
    UNSPECIFIED, TRUE_FALSE, SINGLE_SELECT, MULTIPLE_SELECT, RECORD, SHORT_ANSWER, SINGLE_POLL, MULTIPLE_POLL, SKETCH_RESPONSE;
    companion object {
        fun safeValueOf(value: String?, default: QuizType = UNSPECIFIED): QuizType {
            return try {
                // Use `uppercase()` to make the comparison case-insensitive.
                value?.let { valueOf(it.uppercase()) } ?: default
            } catch (e: IllegalArgumentException) {
                // The string did not match any enum constant.
                default
            }
        }

        fun QuizType.isMultipleChoice(): Boolean {
            return when (this) {
                SINGLE_SELECT, MULTIPLE_SELECT -> true
                else -> false
            }
        }

        fun QuizType.isPoll(): Boolean {
            return when (this) {
                SINGLE_POLL, MULTIPLE_POLL -> true
                else -> false
            }
        }

        fun QuizType.getString(context: Context): String {
            return when (this) {
                TRUE_FALSE -> context.getString(R.string.quiz_types_true_false)
                SINGLE_SELECT,
                MULTIPLE_SELECT -> context.getString(R.string.quiz_types_multiple_choice)
                RECORD -> context.getString(R.string.quiz_types_audio)
                SHORT_ANSWER -> context.getString(R.string.short_answer_capitalized_first_word)
                else -> ""
            }
        }

        fun QuizType.getAnswerType(): AnswerType {
            return when (this) {
                TRUE_FALSE -> AnswerType.TRUE_FALSE
                SINGLE_SELECT,
                MULTIPLE_SELECT,
                SINGLE_POLL,
                MULTIPLE_POLL -> AnswerType.MULTIPLE_CHOICE
                else -> AnswerType.TRUE_FALSE
            }
        }
    }
}