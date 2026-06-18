package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.data.state.SelectionOptionType
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import timber.log.Timber

class MultipleChoiceWindowModel(private val quizManager: QuizManager) : IWindowModel {

    fun getMultipleChoiceResultInfos(): List<QuizAnswerResultInfo> {
        return quizManager.getMultipleChoiceResultInfos(QuizSharedUiInfo.quizOptionType)
    }

    /** Transition from DISCLOSE_ANSWER → QUIZ_RESULTS (mirror TrueFalseWindowModel). */
    fun triggerQuizResultState() {
        quizManager.changeQuizState(QuizState.QUIZ_RESULTS)
    }

    override fun onCleared() {
        Timber.d("[MultipleChoiceWindowModel] onCleared")
    }

    companion object {
        const val DEFAULT_OPTION_COUNT: Int = 4
        val DEFAULT_OPTION_TYPE: QuizOptionType = QuizOptionType.NUMBER
        val DEFAULT_SELECTION_TYPE: SelectionOptionType = SelectionOptionType.SINGLE
    }
}