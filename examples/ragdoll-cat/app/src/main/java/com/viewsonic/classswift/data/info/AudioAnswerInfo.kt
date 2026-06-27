package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.AudioState

data class AudioAnswerInfo(
    // For sorting used
    val serialNumber: Int = -1,
    val displaySeatNumber: String = "",
    val displayName: String = "",
    val studentId: String = "",
    val answeringState: AnsweringState = AnsweringState.NOT_ANSWER,
    val audioUrl: String = "",
    val quizType: QuizType = QuizType.RECORD,
    var canShowAnswer: Boolean = false,
    var isPartiallyVisible: Boolean = true,
    var isInResultState: Boolean = false,
    var audioState: AudioState = AudioState.INIT,
    var answerResultState: AnswerResultState = AnswerResultState.ABSENT,
    var audioDuration: Long? = null,
    var audioRemainTime: Long = 0L
) {
    companion object {
        fun toQuizAnswerResultInfo(
            info: AudioAnswerInfo
        ): QuizAnswerResultInfo {
            return QuizAnswerResultInfo(
                serialNumber = info.serialNumber,
                displaySeatNumber = info.displaySeatNumber,
                displayName = info.displayName,
                studentId = info.studentId,
                answerResultState = info.answerResultState,
                quizOptionType = QuizOptionType.NO_OPTION,
                quizType = info.quizType
            )
        }
    }
}