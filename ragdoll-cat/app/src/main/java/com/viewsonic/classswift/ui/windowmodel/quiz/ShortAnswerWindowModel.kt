package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import timber.log.Timber

class ShortAnswerWindowModel(private val quizManager: QuizManager) : IWindowModel {

    fun getShortAnswerResultInfos(): List<QuizAnswerResultInfo> {
        return quizManager.getShortAnswerResultInfos()
    }

    override fun onCleared() {
        Timber.d("[ShortAnswerWindowModel] onCleared")
    }
}