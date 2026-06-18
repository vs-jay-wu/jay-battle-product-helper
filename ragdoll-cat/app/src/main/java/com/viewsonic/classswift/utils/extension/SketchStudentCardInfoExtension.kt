package com.viewsonic.classswift.utils.extension

import com.viewsonic.classswift.data.enum.SketchAnswerStatus
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.info.SketchStudentCardInfo
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState

/**
 * Maps the sketch-response polling result onto the shared [QuizAnsweringInfo]
 * used by [MvbQuizAnsweringAdapter] so sketch cards render identically to
 * TF / MC / SA / Audio / Poll quizzing cards. `canShowAnswer = false` keeps
 * the card in the "Submitted" text state — the sketch image itself is not
 * rendered inline on the card (review is in the post-quiz result window).
 */
fun SketchStudentCardInfo.toQuizAnsweringInfo(): QuizAnsweringInfo = QuizAnsweringInfo(
    serialNumber = serialNumber,
    displaySeatNumber = displaySeatNumber,
    displayName = displayName,
    studentId = studentId,
    answeringState = when (status) {
        SketchAnswerStatus.SUBMITTED -> AnsweringState.ANSWERED
        SketchAnswerStatus.NOT_SUBMITTED -> AnsweringState.NOT_ANSWER
        SketchAnswerStatus.ABSENT -> AnsweringState.ABSENT
    },
    quizType = QuizType.SKETCH_RESPONSE,
    canShowAnswer = false,
)
