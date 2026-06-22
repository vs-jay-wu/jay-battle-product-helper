package com.viewsonic.classswift.ui.window.quiz.edit

import android.content.Context
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.ui.window.quiz.start.MvbShortAnswerStartWindow
import com.viewsonic.classswift.windowframework.core.enums.WindowTag

/** Short Answer editor (CMP hybrid) — image-only; no options, opens [MvbShortAnswerStartWindow]. */
class MvbShortAnswerEditWindow(context: Context) : MvbQuizEditHostWindow(context) {
    override var tag: WindowTag = WindowTag.MVB_SHORT_ANSWER_EDIT_QUIZ
    override val editType: MvbQuizType = MvbQuizType.SHORT_ANSWER
    override fun buildCreateArgs(): CreateArgs = CreateArgs(
        optionType = QuizOptionType.NO_OPTION,
        quizType = QuizType.SHORT_ANSWER,
        options = emptyList(),
    )
    override val startWindowClass: Class<*> = MvbShortAnswerStartWindow::class.java
    override val reopenSelfClass: Class<*> = MvbShortAnswerEditWindow::class.java
}
