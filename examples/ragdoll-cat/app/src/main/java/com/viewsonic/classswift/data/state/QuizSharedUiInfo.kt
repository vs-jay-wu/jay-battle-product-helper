package com.viewsonic.classswift.data.state

import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.ui.windowmodel.quiz.MultipleChoiceWindowModel.Companion.DEFAULT_OPTION_COUNT
import com.viewsonic.classswift.ui.windowmodel.quiz.MultipleChoiceWindowModel.Companion.DEFAULT_OPTION_TYPE
import com.viewsonic.classswift.ui.windowmodel.quiz.MultipleChoiceWindowModel.Companion.DEFAULT_SELECTION_TYPE
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import timber.log.Timber

object QuizSharedUiInfo {

    var quizType: QuizType = QuizType.UNSPECIFIED
        private set(value) {
            field = value
            isNonStandardAnswerType = when (value) {
                QuizType.SHORT_ANSWER, QuizType.RECORD, QuizType.SINGLE_POLL, QuizType.MULTIPLE_POLL -> true
                else -> false
            }
        }
    var isOngoing: Boolean = false
        private set
    var isNonStandardAnswerType: Boolean = false
        private set

    var screenshotImageUri: String = ""

    // for multiple Choice quiz, for shared window model can only for multiple choice quiz
    var quizContent: String = ""
    val quizOptionList: MutableList<QuizOption> = mutableListOf()
    var quizOptionCount: Int = DEFAULT_OPTION_COUNT
    var quizOptionType: QuizOptionType = DEFAULT_OPTION_TYPE
    var singleOrMultipleSelectionType: SelectionOptionType = DEFAULT_SELECTION_TYPE

    // region For Ongoing feature
    fun setOngoingFlag(isOngoing: Boolean) {
        Timber.d("[setOngoingFlag] - $isOngoing")
        this.isOngoing = isOngoing
    }

    fun setQuizTypeByTag(windowTag: WindowTag) {
        quizType = when (windowTag) {
            // TODO: has other quiz type, check by EditQuiz's tag
            WindowTag.TRUE_FALSE_EDIT_QUIZ,
            WindowTag.MVB_TRUE_FALSE_EDIT_QUIZ -> {
                QuizType.TRUE_FALSE
            }
            WindowTag.MULTIPLE_CHOICE_EDIT_QUIZ,
            WindowTag.MVB_MULTIPLE_CHOICE_EDIT_QUIZ -> {
                if (singleOrMultipleSelectionType == SelectionOptionType.SINGLE) {
                    QuizType.SINGLE_SELECT
                } else {
                    QuizType.MULTIPLE_SELECT
                }
            }
            WindowTag.SHORT_ANSWER_EDIT_QUIZ,
            WindowTag.MVB_SHORT_ANSWER_EDIT_QUIZ -> {
                QuizType.SHORT_ANSWER
            }
            WindowTag.AUDIO_EDIT_QUIZ,
            WindowTag.MVB_AUDIO_EDIT_QUIZ -> {
                QuizType.RECORD
            }
            WindowTag.POLL_EDIT_QUIZ,
            WindowTag.MVB_POLL_EDIT_QUIZ -> {
                if (singleOrMultipleSelectionType == SelectionOptionType.SINGLE) {
                    QuizType.SINGLE_POLL
                } else {
                    QuizType.MULTIPLE_POLL
                }
            }
            WindowTag.MVB_SKETCH_RESPONSE_EDIT_QUIZ -> {
                QuizType.SKETCH_RESPONSE
            }
            else -> {
                QuizType.UNSPECIFIED
            }
        }
    }

    fun updateQuizType(type: QuizType) {
        quizType = type
    }

    // leave class should reset the multiple option.
    fun resetMultipleOptions() {
        quizOptionCount = DEFAULT_OPTION_COUNT
        quizOptionType = DEFAULT_OPTION_TYPE
        singleOrMultipleSelectionType = DEFAULT_SELECTION_TYPE
    }
}

enum class SelectionOptionType {
    UNSPECIFIED, SINGLE, MULTIPLE;
    companion object {
        /**
         * Safely converts a string to a [SelectionOptionType], returning a default value if the string is invalid.
         *
         * @param value The string to convert. Case-insensitive.
         * @param default The value to return if conversion fails. Defaults to [UNSPECIFIED].
         * @return The corresponding [SelectionOptionType] or the default value.
         */
        fun safeValueOf(value: String?, default: SelectionOptionType = UNSPECIFIED): SelectionOptionType {
            return try {
                // Use `uppercase()` to make the comparison case-insensitive.
                value?.let { valueOf(it.uppercase()) } ?: default
            } catch (e: IllegalArgumentException) {
                // The string did not match any enum constant.
                default
            }
        }
    }
}
