package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import com.viewsonic.classswift.api.response.QuizzesInCollectionFolderResponse
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.ui.window.quiz.start.MvbAudioQuizStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbMultipleChoiceStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbPollQuizStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbShortAnswerStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbTextShortAnswerStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbTextTrueFalseStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbTrueFalseStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.TextMultipleChoiceStartWindow
import org.junit.Assert.assertEquals
import org.junit.Test

class MvbQuizDetailViewFactoryTest {

    // Image variant (imgUrl non-blank): MVB start windows for every type
    // (TF / MC / Poll / Audio / ShortAnswer; UNSPECIFIED falls through to ShortAnswer).

    @Test
    fun `image TRUE_FALSE routes to MvbTrueFalseStartWindow`() {
        assertEquals(
            MvbTrueFalseStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(imageInfo("TRUE_FALSE")),
        )
    }

    @Test
    fun `image SINGLE_SELECT routes to MvbMultipleChoiceStartWindow`() {
        assertEquals(
            MvbMultipleChoiceStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(imageInfo("SINGLE_SELECT")),
        )
    }

    @Test
    fun `image MULTIPLE_SELECT routes to MvbMultipleChoiceStartWindow`() {
        assertEquals(
            MvbMultipleChoiceStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(imageInfo("MULTIPLE_SELECT")),
        )
    }

    @Test
    fun `image SINGLE_POLL routes to MvbPollQuizStartWindow`() {
        assertEquals(
            MvbPollQuizStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(imageInfo("SINGLE_POLL")),
        )
    }

    @Test
    fun `image MULTIPLE_POLL routes to MvbPollQuizStartWindow`() {
        assertEquals(
            MvbPollQuizStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(imageInfo("MULTIPLE_POLL")),
        )
    }

    @Test
    fun `image RECORD routes to MvbAudioQuizStartWindow`() {
        assertEquals(
            MvbAudioQuizStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(imageInfo("RECORD")),
        )
    }

    @Test
    fun `image SHORT_ANSWER routes to MvbShortAnswerStartWindow`() {
        assertEquals(
            MvbShortAnswerStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(imageInfo("SHORT_ANSWER")),
        )
    }

    @Test
    fun `image unknown quizType falls through to MvbShortAnswerStartWindow`() {
        // QuizType.safeValueOf returns UNSPECIFIED for unrecognized strings.
        assertEquals(
            MvbShortAnswerStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(imageInfo("UNKNOWN_TYPE")),
        )
    }

    // Text variant (imgUrl blank): MVB text windows for TF / SA / SKETCH / UNSPECIFIED,
    // standalone TextMultipleChoice for SELECT, Poll / RECORD share the image path's routing.

    @Test
    fun `text TRUE_FALSE routes to MvbTextTrueFalseStartWindow`() {
        assertEquals(
            MvbTextTrueFalseStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(textInfo("TRUE_FALSE")),
        )
    }

    @Test
    fun `text SINGLE_SELECT routes to TextMultipleChoiceStartWindow`() {
        assertEquals(
            TextMultipleChoiceStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(textInfo("SINGLE_SELECT")),
        )
    }

    @Test
    fun `text MULTIPLE_SELECT routes to TextMultipleChoiceStartWindow`() {
        assertEquals(
            TextMultipleChoiceStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(textInfo("MULTIPLE_SELECT")),
        )
    }

    @Test
    fun `text SHORT_ANSWER routes to MvbTextShortAnswerStartWindow`() {
        assertEquals(
            MvbTextShortAnswerStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(textInfo("SHORT_ANSWER")),
        )
    }

    @Test
    fun `text SKETCH_RESPONSE falls back to MvbTextShortAnswerStartWindow`() {
        // SKETCH_RESPONSE is screenshot-based (isTextQuiz() is always false for it), so this text
        // branch is unreachable in practice — a defensive fallback grouped with SHORT_ANSWER.
        assertEquals(
            MvbTextShortAnswerStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(textInfo("SKETCH_RESPONSE")),
        )
    }

    @Test
    fun `text unknown quizType falls through to MvbTextShortAnswerStartWindow`() {
        assertEquals(
            MvbTextShortAnswerStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(textInfo("UNKNOWN_TYPE")),
        )
    }

    @Test
    fun `text SINGLE_POLL routes to MvbPollQuizStartWindow`() {
        assertEquals(
            MvbPollQuizStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(textInfo("SINGLE_POLL")),
        )
    }

    @Test
    fun `text MULTIPLE_POLL routes to MvbPollQuizStartWindow`() {
        assertEquals(
            MvbPollQuizStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(textInfo("MULTIPLE_POLL")),
        )
    }

    @Test
    fun `text RECORD routes to MvbAudioQuizStartWindow`() {
        assertEquals(
            MvbAudioQuizStartWindow::class.java,
            MvbQuizDetailViewFactory.resolveStartWindow(textInfo("RECORD")),
        )
    }

    private fun imageInfo(quizType: String): QuizInCollectionInfo =
        QuizInCollectionInfo(
            quizData = QuizzesInCollectionFolderResponse.QuizInCollectionData(
                quizType = quizType,
                imgUrl = "https://example.com/q.png",
            ),
        )

    private fun textInfo(quizType: String): QuizInCollectionInfo =
        QuizInCollectionInfo(
            quizData = QuizzesInCollectionFolderResponse.QuizInCollectionData(
                quizType = quizType,
                imgUrl = "",
            ),
        )
}
