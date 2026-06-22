package com.viewsonic.classswift.ui.window.quiz.edit

import android.content.Context
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.data.state.SelectionOptionType
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.ui.window.quiz.start.MvbPollQuizStartWindow
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag

/** Poll editor (CMP hybrid) — image + option panel (poll labels); opens [MvbPollQuizStartWindow]. */
class MvbPollQuizEditWindow(context: Context) : MvbQuizEditHostWindow(context) {
    override var tag: WindowTag = WindowTag.MVB_POLL_EDIT_QUIZ
    override val editType: MvbQuizType = MvbQuizType.POLL
    override var size: SizeInPixels = SizeInPixels(541.33f.dpToPx().toInt(), 626f.dpToPx().toInt())

    private var count: Int = QuizSharedUiInfo.quizOptionCount
    private var letters: Boolean = QuizSharedUiInfo.quizOptionType != QuizOptionType.NUMBER
    private var single: Boolean = QuizSharedUiInfo.singleOrMultipleSelectionType == SelectionOptionType.SINGLE

    override val initialOptionCount: Int get() = count
    override val initialLetters: Boolean get() = letters
    override val initialSingle: Boolean get() = single

    override fun onViewCreated() {
        super.onViewCreated()
        QuizSharedUiInfo.setQuizTypeByTag(tag)
    }

    override fun onOptionConfig(count: Int, letters: Boolean, single: Boolean) {
        this.count = count
        this.letters = letters
        this.single = single
        QuizSharedUiInfo.quizOptionType = if (letters) QuizOptionType.ALPHABET else QuizOptionType.NUMBER
        QuizSharedUiInfo.singleOrMultipleSelectionType = if (single) SelectionOptionType.SINGLE else SelectionOptionType.MULTIPLE
        QuizSharedUiInfo.setQuizTypeByTag(tag)
    }

    override fun buildCreateArgs(): CreateArgs = CreateArgs(
        optionType = if (letters) QuizOptionType.ALPHABET else QuizOptionType.NUMBER,
        quizType = QuizSharedUiInfo.quizType,
        options = (1..count).map { QuizOption(optionId = it) },
        saveOptions = true,
    )
    override val startWindowClass: Class<*> = MvbPollQuizStartWindow::class.java
    override val reopenSelfClass: Class<*> = MvbPollQuizEditWindow::class.java
}
