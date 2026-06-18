package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.quiz.QuizType.Companion.getAnswerType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerType
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType
import kotlin.collections.emptyList

data class BatchQuizResultDetailInfo(
    val quizType: QuizType = QuizType.UNSPECIFIED,
    val optionType: QuizOptionType = QuizOptionType.NO_OPTION,
    val correctStudentCount: Int = 0,
    val inCorrectStudentCount: Int = 0,
    val noAnswerStudentCount: Int = 0,
    val quizContent: String = "",
    var optionValueType: OptionValueType = OptionValueType.NUMBER,
    val optionList: List<QuizOption> = emptyList(),
    val studentAnswerResultList: List<QuizAnswerResultInfo> = emptyList()
)
    {
        val submittedStudentCount: Int
            get() = correctStudentCount + inCorrectStudentCount + noAnswerStudentCount
        val answerType: AnswerType
            get() = quizType.getAnswerType()
        val correctAnswerList:  List<Int>
            get() = optionList.filter { it.isCorrectAnswer() }.map { it.optionId }

        fun getAnswerOptionInfoList(): ArrayList<AnswerOptionInfo> {
            val answerOptionInfoList = ArrayList<AnswerOptionInfo>()
            // For Option data
            val submittedCount = submittedStudentCount
            optionList.forEachIndexed { index, option ->
                val answerResultState = if (optionList[index].isCorrectAnswer()) {
                    AnswerResultState.CORRECT
                } else {
                    AnswerResultState.INCORRECT
                }
                answerOptionInfoList.add(
                    AnswerOptionInfo(
                        position = index,
                        answerType = quizType.getAnswerType(),
                        answerResultState = answerResultState,
                        multipleType = QuizSharedUiInfo.quizOptionType,
                        answerCount = studentAnswerResultList.map { it.answerOption }
                            .count { it.contains(index + 1) },
                        totalStudents = submittedCount
                    )
                )
            }
            // For Unsubmitted Answers
            answerOptionInfoList.add(
                AnswerOptionInfo(
                    answerResultState = AnswerResultState.NO_ANSWER,
                    answerCount = studentAnswerResultList.count { it.answerResultState == AnswerResultState.NO_ANSWER },
                    totalStudents = submittedCount
                )
            )

            return answerOptionInfoList
        }
    }

