package com.viewsonic.classswift.data.state

import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuizSharedUiInfoTest {

    @Before
    fun setup() {
        // QuizSharedUiInfo is a singleton; reset state that leaks between tests.
        QuizSharedUiInfo.resetMultipleOptions()
    }

    // Simple Edit-quiz tag mappings (no SelectionOptionType dependency).

    @Test
    fun `TRUE_FALSE_EDIT_QUIZ maps to TRUE_FALSE`() {
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.TRUE_FALSE_EDIT_QUIZ)
        assertEquals(QuizType.TRUE_FALSE, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `MVB_TRUE_FALSE_EDIT_QUIZ maps to TRUE_FALSE`() {
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.MVB_TRUE_FALSE_EDIT_QUIZ)
        assertEquals(QuizType.TRUE_FALSE, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `SHORT_ANSWER_EDIT_QUIZ maps to SHORT_ANSWER`() {
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.SHORT_ANSWER_EDIT_QUIZ)
        assertEquals(QuizType.SHORT_ANSWER, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `AUDIO_EDIT_QUIZ maps to RECORD`() {
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.AUDIO_EDIT_QUIZ)
        assertEquals(QuizType.RECORD, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `MVB_AUDIO_EDIT_QUIZ maps to RECORD`() {
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.MVB_AUDIO_EDIT_QUIZ)
        assertEquals(QuizType.RECORD, QuizSharedUiInfo.quizType)
    }

    // Multiple-choice and Poll branch on singleOrMultipleSelectionType.

    @Test
    fun `MULTIPLE_CHOICE_EDIT_QUIZ with SINGLE selection maps to SINGLE_SELECT`() {
        QuizSharedUiInfo.singleOrMultipleSelectionType = SelectionOptionType.SINGLE
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.MULTIPLE_CHOICE_EDIT_QUIZ)
        assertEquals(QuizType.SINGLE_SELECT, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `MULTIPLE_CHOICE_EDIT_QUIZ with MULTIPLE selection maps to MULTIPLE_SELECT`() {
        QuizSharedUiInfo.singleOrMultipleSelectionType = SelectionOptionType.MULTIPLE
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.MULTIPLE_CHOICE_EDIT_QUIZ)
        assertEquals(QuizType.MULTIPLE_SELECT, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `MVB_MULTIPLE_CHOICE_EDIT_QUIZ with SINGLE selection maps to SINGLE_SELECT`() {
        QuizSharedUiInfo.singleOrMultipleSelectionType = SelectionOptionType.SINGLE
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.MVB_MULTIPLE_CHOICE_EDIT_QUIZ)
        assertEquals(QuizType.SINGLE_SELECT, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `MVB_MULTIPLE_CHOICE_EDIT_QUIZ with MULTIPLE selection maps to MULTIPLE_SELECT`() {
        QuizSharedUiInfo.singleOrMultipleSelectionType = SelectionOptionType.MULTIPLE
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.MVB_MULTIPLE_CHOICE_EDIT_QUIZ)
        assertEquals(QuizType.MULTIPLE_SELECT, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `POLL_EDIT_QUIZ with SINGLE selection maps to SINGLE_POLL`() {
        QuizSharedUiInfo.singleOrMultipleSelectionType = SelectionOptionType.SINGLE
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.POLL_EDIT_QUIZ)
        assertEquals(QuizType.SINGLE_POLL, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `POLL_EDIT_QUIZ with MULTIPLE selection maps to MULTIPLE_POLL`() {
        QuizSharedUiInfo.singleOrMultipleSelectionType = SelectionOptionType.MULTIPLE
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.POLL_EDIT_QUIZ)
        assertEquals(QuizType.MULTIPLE_POLL, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `MVB_POLL_EDIT_QUIZ with SINGLE selection maps to SINGLE_POLL`() {
        QuizSharedUiInfo.singleOrMultipleSelectionType = SelectionOptionType.SINGLE
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.MVB_POLL_EDIT_QUIZ)
        assertEquals(QuizType.SINGLE_POLL, QuizSharedUiInfo.quizType)
    }

    @Test
    fun `MVB_POLL_EDIT_QUIZ with MULTIPLE selection maps to MULTIPLE_POLL`() {
        QuizSharedUiInfo.singleOrMultipleSelectionType = SelectionOptionType.MULTIPLE
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.MVB_POLL_EDIT_QUIZ)
        assertEquals(QuizType.MULTIPLE_POLL, QuizSharedUiInfo.quizType)
    }

    // Non-Edit window tags fall through to UNSPECIFIED.

    @Test
    fun `non-edit window tag falls through to UNSPECIFIED`() {
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.MVB_AUDIO_START_QUIZ)
        assertEquals(QuizType.UNSPECIFIED, QuizSharedUiInfo.quizType)
    }

    // Side-effect: quizType's setter updates isNonStandardAnswerType.

    @Test
    fun `RECORD quizType sets isNonStandardAnswerType to true`() {
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.MVB_AUDIO_EDIT_QUIZ)
        assertTrue(QuizSharedUiInfo.isNonStandardAnswerType)
    }

    @Test
    fun `TRUE_FALSE quizType sets isNonStandardAnswerType to false`() {
        QuizSharedUiInfo.setQuizTypeByTag(WindowTag.TRUE_FALSE_EDIT_QUIZ)
        assertFalse(QuizSharedUiInfo.isNonStandardAnswerType)
    }
}
