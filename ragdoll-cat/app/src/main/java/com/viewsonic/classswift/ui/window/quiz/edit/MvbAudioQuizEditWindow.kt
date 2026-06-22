package com.viewsonic.classswift.ui.window.quiz.edit

import android.content.Context
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.ui.window.quiz.start.MvbAudioQuizStartWindow
import com.viewsonic.classswift.windowframework.core.enums.WindowTag

/** Audio editor (CMP hybrid) — image-only; no options, opens [MvbAudioQuizStartWindow]. */
class MvbAudioQuizEditWindow(context: Context) : MvbQuizEditHostWindow(context) {
    override var tag: WindowTag = WindowTag.MVB_AUDIO_EDIT_QUIZ
    override val editType: MvbQuizType = MvbQuizType.AUDIO
    override fun buildCreateArgs(): CreateArgs = CreateArgs(
        optionType = QuizOptionType.NO_OPTION,
        quizType = QuizType.RECORD,
        options = emptyList(),
    )
    override val startWindowClass: Class<*> = MvbAudioQuizStartWindow::class.java
    override val reopenSelfClass: Class<*> = MvbAudioQuizEditWindow::class.java
}
