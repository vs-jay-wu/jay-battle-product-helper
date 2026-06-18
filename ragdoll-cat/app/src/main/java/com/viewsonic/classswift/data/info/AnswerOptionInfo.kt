package com.viewsonic.classswift.data.info

import androidx.annotation.IdRes
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerType
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionLanguageType
import com.viewsonic.classswift.utils.LanguageUtils


data class AnswerOptionInfo(
    val position: Int = -1,
    val answerType: AnswerType = AnswerType.TRUE_FALSE,
    val answerResultState: AnswerResultState = AnswerResultState.NO_ANSWER,
    val trueFalseType: OptionLanguageType = LanguageUtils.getTfOptionLanguageType(),
    val multipleType: QuizOptionType = QuizOptionType.ALPHABET,
    val answerCount: Int = 0,
    val totalStudents: Int = 50
) {
    fun answerPercent(): Float {
        return answerCount / totalStudents.toFloat()
    }

    @IdRes
    fun getAnswerIcon(): Int? {
        if (answerResultState == AnswerResultState.NO_ANSWER) return null

        return when (answerType) {
            AnswerType.TRUE_FALSE -> {
                when (trueFalseType) {
                    OptionLanguageType.ENGLISH -> if (position == 0) R.drawable.ic_true else R.drawable.ic_false
                    OptionLanguageType.CHINESE -> if (position == 0) R.drawable.ic_true_tw else R.drawable.ic_false_tw
                }
            }

            AnswerType.MULTIPLE_CHOICE -> {
                when (position) {
                    0 -> {
                        if (multipleType == QuizOptionType.NUMBER) R.drawable.ic_answer_1 else R.drawable.ic_answer_a
                    }

                    1 -> {
                        if (multipleType == QuizOptionType.NUMBER) R.drawable.ic_answer_2 else R.drawable.ic_answer_b
                    }

                    2 -> {
                        if (multipleType == QuizOptionType.NUMBER) R.drawable.ic_answer_3 else R.drawable.ic_answer_c
                    }

                    3 -> {
                        if (multipleType == QuizOptionType.NUMBER) R.drawable.ic_answer_4 else R.drawable.ic_answer_d
                    }

                    4 -> {
                        if (multipleType == QuizOptionType.NUMBER) R.drawable.ic_answer_5 else R.drawable.ic_answer_e
                    }

                    5 -> {
                        if (multipleType == QuizOptionType.NUMBER) R.drawable.ic_answer_6 else R.drawable.ic_answer_f
                    }

                    else -> {
                        null
                    }
                }
            }
        }
    }
}
