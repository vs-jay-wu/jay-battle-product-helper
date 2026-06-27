package com.viewsonic.classswift.utils

import androidx.annotation.IdRes
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerType
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionLanguageType

object QuizUtils {
    @IdRes
    fun getAnswerResultIcon(info: QuizAnswerResultInfo): Int? {
        with(info) {
            if (answerResultState == AnswerResultState.NO_ANSWER) return null

            return when (answerType) {
                AnswerType.TRUE_FALSE -> {
                    when (optionLanguageType) {
                        OptionLanguageType.ENGLISH -> if (answerOption[0] == 1) R.drawable.ic_true else R.drawable.ic_false
                        OptionLanguageType.CHINESE -> if (answerOption[0] == 1) R.drawable.ic_true_tw else R.drawable.ic_false_tw
                    }
                }

                AnswerType.MULTIPLE_CHOICE -> {
                    when (answerOption[0]) {
                        1 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_1 else R.drawable.ic_answer_a
                        }

                        2 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_2 else R.drawable.ic_answer_b
                        }

                        3 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_3 else R.drawable.ic_answer_c
                        }

                        4 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_4 else R.drawable.ic_answer_d
                        }

                        5 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_5 else R.drawable.ic_answer_e
                        }

                        6 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_6 else R.drawable.ic_answer_f
                        }

                        else -> {
                            null
                        }
                    }
                }

                null -> null
            }
        }
    }

    @IdRes
    fun getAnsweringIcon(info: QuizAnsweringInfo): Int? {
        with(info) {
            if (answeringState != AnsweringState.ANSWERED) return null

            return when (answerType) {
                AnswerType.TRUE_FALSE -> {
                    when (optionLanguageType) {
                        OptionLanguageType.ENGLISH -> if (answerOption[0] == 1) R.drawable.ic_true else R.drawable.ic_false
                        OptionLanguageType.CHINESE -> if (answerOption[0] == 1) R.drawable.ic_true_tw else R.drawable.ic_false_tw
                    }
                }

                AnswerType.MULTIPLE_CHOICE -> {
                    when (answerOption[0]) {
                        1 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_1 else R.drawable.ic_answer_a
                        }

                        2 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_2 else R.drawable.ic_answer_b
                        }

                        3 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_3 else R.drawable.ic_answer_c
                        }

                        4 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_4 else R.drawable.ic_answer_d
                        }

                        5 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_5 else R.drawable.ic_answer_e
                        }

                        6 -> {
                            if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_6 else R.drawable.ic_answer_f
                        }

                        else -> {
                            null
                        }
                    }
                }
            }
        }
    }

    @IdRes
    fun getAnsweringIcon(quizOptionType: QuizOptionType, answerOption: Int) =
        when (answerOption) {
            1 -> {
                if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_1 else R.drawable.ic_answer_a
            }

            2 -> {
                if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_2 else R.drawable.ic_answer_b
            }

            3 -> {
                if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_3 else R.drawable.ic_answer_c
            }

            4 -> {
                if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_4 else R.drawable.ic_answer_d
            }

            5 -> {
                if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_5 else R.drawable.ic_answer_e
            }

            6 -> {
                if (quizOptionType == QuizOptionType.NUMBER) R.drawable.ic_answer_6 else R.drawable.ic_answer_f
            }

            else -> {
                null
            }
        }
}