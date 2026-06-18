package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.data.info.AnswerOptionInfo
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerType
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import timber.log.Timber

class TrueFalseWindowModel(
    private val quizManager: QuizManager,
) : IWindowModel {
    fun generateAnswerOptionInfoList(trueNumber: Int, falseNumber: Int, noAnswerNumber: Int, correctOptionId: Int): ArrayList<AnswerOptionInfo> {
        val correctAnswerOptionInfo = AnswerOptionInfo(
            position = 0,
            answerType = AnswerType.TRUE_FALSE,
            answerResultState = if (correctOptionId == TRUE_OPTION_INDEX) AnswerResultState.CORRECT else AnswerResultState.INCORRECT,
            answerCount = trueNumber,
            totalStudents = trueNumber + falseNumber + noAnswerNumber
        )
        val incorrectAnswerOptionInfo = AnswerOptionInfo(
            position = 1,
            answerType = AnswerType.TRUE_FALSE,
            answerResultState = if (correctOptionId == FALSE_OPTION_INDEX) AnswerResultState.CORRECT else AnswerResultState.INCORRECT,
            answerCount = falseNumber,
            totalStudents = trueNumber + falseNumber + noAnswerNumber
        )
        val noAnswerOptionInfo = AnswerOptionInfo(
            position = 2,
            answerType = AnswerType.TRUE_FALSE,
            answerResultState = AnswerResultState.NO_ANSWER,
            answerCount = noAnswerNumber,
            totalStudents = trueNumber + falseNumber + noAnswerNumber
        )
        return arrayListOf<AnswerOptionInfo>(correctAnswerOptionInfo, incorrectAnswerOptionInfo, noAnswerOptionInfo)
    }

    /**
     * Transition quiz state from DISCLOSE_ANSWER → QUIZ_RESULTS after disclose API success.
     * Called by [com.viewsonic.classswift.ui.window.quiz.start.MvbTrueFalseStartWindow] once
     * discloseAnswerData becomes non-empty (one-shot; caller guards re-entry).
     */
    fun triggerQuizResultState() {
        quizManager.changeQuizState(QuizState.QUIZ_RESULTS)
    }

    override fun onCleared() {
        Timber.d("onCleared")
    }

    companion object {
        const val TRUE_OPTION_INDEX = 1
        const val FALSE_OPTION_INDEX = 2
    }
}
