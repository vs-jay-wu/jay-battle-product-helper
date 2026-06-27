package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.api.response.DiscloseQuizResponse
import com.viewsonic.classswift.data.info.StudentInfo.Status
import com.viewsonic.classswift.data.quiz.QuizType.Companion.getAnswerType
import com.viewsonic.classswift.data.socket.quiz.data.AnswerData
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerType
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionLanguageType
import com.viewsonic.classswift.utils.LanguageUtils

data class QuizAnswerResultInfo(
    // For sorting used
    val serialNumber: Int = -1,
    val displaySeatNumber: String = "",
    val displayName: String = "",
    val studentId: String = "",
    val answerOption: List<Int> = emptyList(),
    val answerStringData: String = "",
    val answerResultState: AnswerResultState = AnswerResultState.NO_ANSWER,
    val optionLanguageType: OptionLanguageType = OptionLanguageType.ENGLISH,
    val quizOptionType: QuizOptionType = QuizOptionType.ALPHABET,
    val quizType: QuizType = QuizType.UNSPECIFIED,
    val isPartiallyVisible: Boolean = true
) {
    val answerType: AnswerType?
        get() = quizType.getAnswerType()
    companion object {
        fun fromStudentTrueFalseQuizzingInfo(
            studentQuizzingInfo: StudentQuizzingInfo,
            correctOptionId: Int,
        ): QuizAnswerResultInfo {
            return QuizAnswerResultInfo(
                serialNumber = studentQuizzingInfo.serialNumber,
                displaySeatNumber = studentQuizzingInfo.displaySeatNumber,
                displayName = studentQuizzingInfo.displayName,
                studentId = studentQuizzingInfo.studentId,
                answerOption = getSingleOptionId(studentQuizzingInfo.answerDataList),
                answerResultState = getSingleAnswerResultState(studentQuizzingInfo, correctOptionId),
                optionLanguageType = LanguageUtils.getTfOptionLanguageType(),
                quizType = QuizType.TRUE_FALSE
            )
        }

        fun fromStudentMultipleChoiceAnsweringInfo(
            studentAnsweringInfo: QuizAnsweringInfo,
            correctAnswerData: List<DiscloseQuizResponse.Data>,
            quizOptionType: QuizOptionType
        ): QuizAnswerResultInfo {
            return QuizAnswerResultInfo(
                serialNumber = studentAnsweringInfo.serialNumber,
                displaySeatNumber = studentAnsweringInfo.displaySeatNumber,
                displayName = studentAnsweringInfo.displayName,
                studentId = studentAnsweringInfo.studentId,
                answerOption = studentAnsweringInfo.answerOption,
                answerResultState = getMultipleChoiceResultState(studentAnsweringInfo, correctAnswerData.map { it.optionId }),
                quizOptionType = quizOptionType,
                quizType = studentAnsweringInfo.quizType
            )
        }

        fun fromStudentShortAnswerAnsweringInfo(
            studentAnsweringInfo: QuizAnsweringInfo
        ): QuizAnswerResultInfo {
            return QuizAnswerResultInfo(
                serialNumber = studentAnsweringInfo.serialNumber,
                displaySeatNumber = studentAnsweringInfo.displaySeatNumber,
                displayName = studentAnsweringInfo.displayName,
                studentId = studentAnsweringInfo.studentId,
                answerOption = studentAnsweringInfo.answerOption,
                answerStringData = studentAnsweringInfo.answerStringData,
                answerResultState = getNonStandardAnswerResultState(studentAnsweringInfo),
                quizOptionType = QuizOptionType.NO_OPTION,
                quizType = studentAnsweringInfo.quizType
            )
        }

        fun fromStudentPollAnswerAnsweringInfo(
            studentAnsweringInfo: QuizAnsweringInfo,
            quizOptionType: QuizOptionType
        ): QuizAnswerResultInfo {
            return QuizAnswerResultInfo(
                serialNumber = studentAnsweringInfo.serialNumber,
                displaySeatNumber = studentAnsweringInfo.displaySeatNumber,
                displayName = studentAnsweringInfo.displayName,
                studentId = studentAnsweringInfo.studentId,
                answerOption = studentAnsweringInfo.answerOption,
                answerResultState = getNonStandardAnswerResultState(studentAnsweringInfo),
                quizOptionType = quizOptionType,
                quizType = studentAnsweringInfo.quizType
            )
        }

        /**
         * For Single Option, e.g. 是非題, 單選題
         */
        private fun getSingleOptionId(answerData: List<AnswerData>): List<Int> {
            return if (answerData.isNotEmpty()) {
                listOf(answerData[0].optionId)
            } else {
                emptyList()
            }
        }

        /**
         * For Single Answer, e.g. 是非題
         */
        private fun getSingleAnswerResultState(
            studentQuizzingInfo: StudentQuizzingInfo,
            correctOptionId: Int
        ): AnswerResultState {
            if (studentQuizzingInfo.status == Status.INACTIVE) {
                return AnswerResultState.ABSENT
            }
            if (studentQuizzingInfo.answerDataList.isEmpty()) {
                return AnswerResultState.NO_ANSWER
            }
            return if (studentQuizzingInfo.answerDataList[0].optionId == correctOptionId) {
                AnswerResultState.CORRECT
            } else {
                AnswerResultState.INCORRECT
            }
        }

        /**
         * For Multiple Choice Single/Multiple Selection
         */
        private fun getMultipleChoiceResultState(
            studentAnsweringInfo: QuizAnsweringInfo,
            correctAnswerIds: List<Int>
        ): AnswerResultState {
            var answeringState = AnswerResultState.ABSENT
            if (studentAnsweringInfo.answeringState != AnsweringState.ABSENT) {
                answeringState = AnswerResultState.NO_ANSWER
                if (studentAnsweringInfo.answerOption.isNotEmpty()) {
                    answeringState = AnswerResultState.INCORRECT
                    if (areAnswersCorrect(studentAnsweringInfo.answerOption, correctAnswerIds)) {
                        answeringState = AnswerResultState.CORRECT
                    }
                }
            }
            return answeringState
        }

        private fun getNonStandardAnswerResultState(
            studentAnsweringInfo: QuizAnsweringInfo
        ): AnswerResultState {
            return when {
                studentAnsweringInfo.answeringState == AnsweringState.ABSENT -> AnswerResultState.ABSENT
                studentAnsweringInfo.answerStringData.isNotEmpty() -> AnswerResultState.ANSWERED
                studentAnsweringInfo.answerOption.isNotEmpty() -> AnswerResultState.ANSWERED
                else -> AnswerResultState.NO_ANSWER
            }
        }

        private fun areAnswersCorrect(answerOptionIds: List<Int>, correctAnswerIds: List<Int>): Boolean {
            if (answerOptionIds.size != correctAnswerIds.size) {
                // 兩邊數量不同一定答錯
                return false
            }
            // 檢查學生回答選項與解答選項是否相同
            val answerOptionArray = arrayOf<Boolean>(false, false, false, false, false, false)
            val correctAnswerArray = arrayOf<Boolean>(false, false, false, false, false, false)
            for (optionId in answerOptionIds) {
                answerOptionArray[optionId - 1] = true
            }
            for (correctId in correctAnswerIds) {
                correctAnswerArray[correctId - 1] = true
            }
            for (index in 0 until answerOptionArray.size) {
                if (answerOptionArray[index] != correctAnswerArray[index]) {
                    return false
                }
            }
            return true
        }
    }
}
