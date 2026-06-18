package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.socket.quiz.data.AnswerData
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerType
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionLanguageType
import com.viewsonic.classswift.utils.LanguageUtils

data class QuizAnsweringInfo(
    // For sorting used
    val serialNumber: Int = -1,
    val displaySeatNumber: String = "",
    val displayName: String = "",
    val studentId: String = "",
    val answerOption: List<Int> = emptyList<Int>(),
    val answerType: AnswerType = AnswerType.TRUE_FALSE,
    val answeringState: AnsweringState = AnsweringState.NOT_ANSWER,
    val answerStringData: String = "",
    val optionLanguageType: OptionLanguageType = OptionLanguageType.ENGLISH,
    val quizOptionType: QuizOptionType = QuizOptionType.ALPHABET,
    val quizType: QuizType = QuizType.UNSPECIFIED,
    var canShowAnswer: Boolean = false
) {
    companion object {
        fun fromStudentTrueFalseQuizzingInfo(
            studentQuizzingInfo: StudentQuizzingInfo,
            answeringState: AnsweringState,
            answerData: List<AnswerData>
        ): QuizAnsweringInfo {
            return QuizAnsweringInfo(
                serialNumber = studentQuizzingInfo.serialNumber,
                displaySeatNumber = studentQuizzingInfo.displaySeatNumber,
                displayName = studentQuizzingInfo.displayName,
                studentId = studentQuizzingInfo.studentId,
                answerOption = getAnswerOption(answerData),
                answerType = AnswerType.TRUE_FALSE,
                answeringState = answeringState,
                optionLanguageType = LanguageUtils.getTfOptionLanguageType(),
                quizType = QuizType.TRUE_FALSE,
                canShowAnswer = studentQuizzingInfo.canShowAnswer
            )
        }

        fun fromStudentMultipleChoiceQuizzingInfo(
            studentQuizzingInfo: StudentQuizzingInfo,
            quizOptionType: QuizOptionType,
            quizType: QuizType,
            answeringState: AnsweringState,
            answerData: List<AnswerData>
        ): QuizAnsweringInfo {
            return QuizAnsweringInfo(
                serialNumber = studentQuizzingInfo.serialNumber,
                displaySeatNumber = studentQuizzingInfo.displaySeatNumber,
                displayName = studentQuizzingInfo.displayName,
                studentId = studentQuizzingInfo.studentId,
                answerOption = getAnswerOption(answerData),
                answerType = AnswerType.MULTIPLE_CHOICE,
                answeringState = answeringState,
                quizOptionType = quizOptionType,
                quizType = quizType,
                canShowAnswer = studentQuizzingInfo.canShowAnswer
            )
        }

        fun fromStudentShortAnswerQuizzingInfo(
            studentQuizzingInfo: StudentQuizzingInfo,
            answeringState: AnsweringState,
        ): QuizAnsweringInfo {
            return QuizAnsweringInfo(
                serialNumber = studentQuizzingInfo.serialNumber,
                displaySeatNumber = studentQuizzingInfo.displaySeatNumber,
                displayName = studentQuizzingInfo.displayName,
                studentId = studentQuizzingInfo.studentId,
                answeringState = answeringState,
                answerStringData = studentQuizzingInfo.answerStringData,
                quizOptionType = QuizOptionType.ALPHABET,
                quizType = QuizType.SHORT_ANSWER,
                canShowAnswer = studentQuizzingInfo.canShowAnswer
            )
        }

        fun fromStudentAudioQuizzingInfo(
            studentQuizzingInfo: StudentQuizzingInfo,
            answeringState: AnsweringState,
            isInResultState: Boolean,
            answerResultState: AnswerResultState

        ): AudioAnswerInfo {
            return AudioAnswerInfo(
                serialNumber = studentQuizzingInfo.serialNumber,
                displaySeatNumber = studentQuizzingInfo.displaySeatNumber,
                displayName = studentQuizzingInfo.displayName,
                studentId = studentQuizzingInfo.studentId,
                answeringState = answeringState,
                audioUrl = studentQuizzingInfo.answerStringData,
                canShowAnswer = studentQuizzingInfo.canShowAnswer,
                isInResultState = isInResultState,
                answerResultState = answerResultState
            )
        }

        fun fromStudentPollQuizzingInfo(
            studentQuizzingInfo: StudentQuizzingInfo,
            quizOptionType: QuizOptionType,
            quizType: QuizType,
            answeringState: AnsweringState,
            answerData: List<AnswerData>
        ): QuizAnsweringInfo {
            return QuizAnsweringInfo(
                serialNumber = studentQuizzingInfo.serialNumber,
                displaySeatNumber = studentQuizzingInfo.displaySeatNumber,
                displayName = studentQuizzingInfo.displayName,
                studentId = studentQuizzingInfo.studentId,
                answerOption = getAnswerOption(answerData),
                answerType = AnswerType.MULTIPLE_CHOICE,
                answeringState = answeringState,
                quizOptionType = quizOptionType,
                quizType = quizType,
                canShowAnswer = studentQuizzingInfo.canShowAnswer
            )
        }

        private fun getAnswerOption(answerData: List<AnswerData>): List<Int> {
            return if (answerData.isNotEmpty()) {
                answerData.map {
                    it.optionId
                }
            } else {
                emptyList()
            }
        }
    }
}
