package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import timber.log.Timber

class PollAnswerWindowModel(private val quizManager: QuizManager) : IWindowModel {

    fun getPollAnswerResultInfos(): List<QuizAnswerResultInfo> {
        return quizManager.getPollAnswerResultInfos(QuizSharedUiInfo.quizOptionType)
    }

    override fun onCleared() {
        Timber.d("[PollAnswerWindowModel] onCleared")
    }
}