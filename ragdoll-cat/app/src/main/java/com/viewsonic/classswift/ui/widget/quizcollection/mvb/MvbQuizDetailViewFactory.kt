package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.ui.window.quiz.start.MvbAudioQuizStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbMultipleChoiceStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbPollQuizStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbShortAnswerStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbTextShortAnswerStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbTextTrueFalseStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbTrueFalseStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.TextMultipleChoiceStartWindow
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow

/**
 * Factory for VSFT-7268 detail views and post-dispatch start-window routing.
 *
 * Two responsibilities:
 *  1. [createDetailView] picks the right [MvbCollectionQuizDetailView] subclass for the given quiz type
 *     (mode aware: text vs image variant).
 *  2. [resolveStartWindow] returns the start-window class to open after a successful dispatch. Image
 *     variants of TF, MC, Poll, Audio, and SA route to their Mvb start windows. Text variants of TF and
 *     SA also have dedicated Mvb start windows; only MC text still uses a standalone window. (Sketch
 *     Response is screenshot-based — its quiz always carries an imgUrl — so it never takes the text
 *     path; the text branch for it is an unreachable defensive fallback.)
 */
object MvbQuizDetailViewFactory {

    fun createDetailView(
        context: Context,
        quizType: QuizType,
        isTextQuiz: Boolean,
    ): MvbCollectionQuizDetailView = when (quizType) {
        QuizType.TRUE_FALSE -> if (isTextQuiz) {
            MvbCollectionTrueFalseTextQuizDetailView(context)
        } else {
            MvbCollectionTrueFalseQuizDetailView(context)
        }
        QuizType.SINGLE_SELECT,
        QuizType.MULTIPLE_SELECT -> if (isTextQuiz) {
            MvbCollectionMultipleChoiceTextQuizDetailView(context)
        } else {
            MvbCollectionMultipleChoiceQuizDetailView(context)
        }
        QuizType.SINGLE_POLL,
        QuizType.MULTIPLE_POLL -> MvbCollectionPollQuizDetailView(context)
        QuizType.RECORD -> MvbCollectionAudioQuizDetailView(context)
        QuizType.SHORT_ANSWER,
        QuizType.SKETCH_RESPONSE,
        QuizType.UNSPECIFIED -> if (isTextQuiz) {
            MvbCollectionShortAnswerTextQuizDetailView(context)
        } else {
            MvbCollectionShortAnswerImageQuizDetailView(context)
        }
    }

    /**
     * Resolves the start-window class to open after a successful dispatch.
     *
     * Image variants: TF → [MvbTrueFalseStartWindow], MC → [MvbMultipleChoiceStartWindow],
     * Poll → [MvbPollQuizStartWindow], Audio → [MvbAudioQuizStartWindow], SA → [MvbShortAnswerStartWindow].
     *
     * Text variants: TF → [MvbTextTrueFalseStartWindow], SA/Sketch/UNSPECIFIED →
     * [MvbTextShortAnswerStartWindow], MC → [TextMultipleChoiceStartWindow] (standalone),
     * Poll → [MvbPollQuizStartWindow], Audio → [MvbAudioQuizStartWindow]. Sketch is screenshot-based,
     * so its text branch is an unreachable defensive fallback grouped with SA.
     */
    fun resolveStartWindow(info: QuizInCollectionInfo): Class<out IWindow<*>> {
        val quizType = QuizType.safeValueOf(info.quizData.quizType)
        return if (info.isTextQuiz()) resolveTextStartWindow(quizType) else resolveImageStartWindow(quizType)
    }

    private fun resolveImageStartWindow(quizType: QuizType): Class<out IWindow<*>> = when (quizType) {
        QuizType.TRUE_FALSE -> MvbTrueFalseStartWindow::class.java
        QuizType.SINGLE_SELECT,
        QuizType.MULTIPLE_SELECT -> MvbMultipleChoiceStartWindow::class.java
        QuizType.SINGLE_POLL,
        QuizType.MULTIPLE_POLL -> MvbPollQuizStartWindow::class.java
        QuizType.RECORD -> MvbAudioQuizStartWindow::class.java
        QuizType.SHORT_ANSWER,
        QuizType.SKETCH_RESPONSE,
        QuizType.UNSPECIFIED -> MvbShortAnswerStartWindow::class.java
    }

    private fun resolveTextStartWindow(quizType: QuizType): Class<out IWindow<*>> = when (quizType) {
        QuizType.TRUE_FALSE -> MvbTextTrueFalseStartWindow::class.java
        QuizType.SINGLE_SELECT,
        QuizType.MULTIPLE_SELECT -> TextMultipleChoiceStartWindow::class.java
        QuizType.SHORT_ANSWER,
        QuizType.SKETCH_RESPONSE,
        QuizType.UNSPECIFIED -> MvbTextShortAnswerStartWindow::class.java
        QuizType.SINGLE_POLL,
        QuizType.MULTIPLE_POLL -> MvbPollQuizStartWindow::class.java
        QuizType.RECORD -> MvbAudioQuizStartWindow::class.java
    }
}
