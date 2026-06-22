package com.viewsonic.classswift.ui.window.quiz.edit

import android.content.Context
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.ui.window.quiz.start.MvbTrueFalseStartWindow
import com.viewsonic.classswift.windowframework.core.enums.WindowTag

/** True/False editor (CMP hybrid) — image-only; 2 fixed options, opens [MvbTrueFalseStartWindow]. */
class MvbTrueFalseEditWindow(context: Context) : MvbQuizEditHostWindow(context) {
    override var tag: WindowTag = WindowTag.MVB_TRUE_FALSE_EDIT_QUIZ
    override val editType: MvbQuizType = MvbQuizType.TRUE_FALSE
    override fun buildCreateArgs(): CreateArgs = CreateArgs(
        optionType = QuizOptionType.TRUE_FALSE,
        quizType = QuizType.TRUE_FALSE,
        options = listOf(QuizOption(optionId = 1), QuizOption(optionId = 2)),
    )
    override val startWindowClass: Class<*> = MvbTrueFalseStartWindow::class.java
    override val reopenSelfClass: Class<*> = MvbTrueFalseEditWindow::class.java
}
