package com.viewsonic.classswift.ui.windowmodel

import android.content.Context
import com.viewsonic.classswift.api.QuizCollectionApiService
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.BatchQuizManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.uimanager.QuizUiManager
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Decision-table coverage for the ongoing-mission gate at the top of
 * [MvbQuizCollectionWindowModel.dispatchSelectedQuiz]. The function merges
 * two independent intents:
 *
 *   - VSFT-8451: detect sketch quizzing (no legacy `quizId`) via
 *     [UnclosedMissionUiManager.getUnclosedMissions]
 *   - VSFT-8604: distinguish [DispatchResult.OngoingConflict] from
 *     [DispatchResult.CancelOngoingAndRetry] via
 *     [QuizUiManager.getCurrentOpenedQuizWindowTag]
 *
 * Inputs:
 *   - hasBatchOngoing  ← `MissionType.BATCH_QUIZZES in getUnclosedMissions()`
 *   - hasQuizOngoing   ← `MissionType.QUIZ in getUnclosedMissions()`
 *   - windowOpen       ← `quizUiManager.getCurrentOpenedQuizWindowTag() != NONE`
 *
 * Outputs (only the gated path; downstream API calls aren't exercised here):
 *   | hasBatch | hasQuiz | windowOpen | DispatchResult           |
 *   |----------|---------|------------|--------------------------|
 *   |   true   |   any   |    any     | OngoingConflict          |
 *   |  false   |  true   |    true    | OngoingConflict          |
 *   |  false   |  true   |    false   | CancelOngoingAndRetry    |
 *   |  false   |  false  |    any     | (continues — out of scope)|
 */
class MvbQuizCollectionWindowModelDispatchTest {

    private val context: Context = mockk(relaxed = true)
    private val accountManager: AccountManager = mockk(relaxed = true) {
        every { canUseStandards } returns false
    }
    private val quizCollectionApiService: QuizCollectionApiService = mockk(relaxed = true)
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider = mockk(relaxed = true)
    private val quizManager: QuizManager = mockk(relaxed = true)
    private val batchQuizManager: BatchQuizManager = mockk(relaxed = true)
    private val quizUiManager: QuizUiManager = mockk(relaxed = true)
    private val unclosedMissionUiManager: UnclosedMissionUiManager = mockk(relaxed = true)

    private lateinit var sut: MvbQuizCollectionWindowModel

    @Before
    fun setUp() {
        sut = newSut()
        sut.selectQuiz(QuizInCollectionInfo())
    }

    private fun newSut(): MvbQuizCollectionWindowModel =
        MvbQuizCollectionWindowModel(
            applicationContext = context,
            accountManager = accountManager,
            quizCollectionApiService = quizCollectionApiService,
            myViewBoardConnectionStateProvider = myViewBoardConnectionStateProvider,
            quizManager = quizManager,
            batchQuizManager = batchQuizManager,
            quizUiManager = quizUiManager,
            unclosedMissionUiManager = unclosedMissionUiManager,
        )

    // ── Gate outputs ───────────────────────────────────────────────────────

    @Test
    fun `OngoingConflict when BATCH ongoing without quiz`() = runBlocking {
        coEvery { unclosedMissionUiManager.getUnclosedMissions() } returns
            listOf(MissionType.BATCH_QUIZZES)
        every { quizUiManager.getCurrentOpenedQuizWindowTag() } returns WindowTag.NONE

        assertEquals(
            MvbQuizCollectionWindowModel.DispatchResult.OngoingConflict,
            sut.dispatchSelectedQuiz(),
        )
    }

    @Test
    fun `OngoingConflict when BATCH and QUIZ ongoing with window open`() = runBlocking {
        coEvery { unclosedMissionUiManager.getUnclosedMissions() } returns
            listOf(MissionType.BATCH_QUIZZES, MissionType.QUIZ)
        every { quizUiManager.getCurrentOpenedQuizWindowTag() } returns
            WindowTag.MVB_SKETCH_RESPONSE_START_QUIZ

        assertEquals(
            MvbQuizCollectionWindowModel.DispatchResult.OngoingConflict,
            sut.dispatchSelectedQuiz(),
        )
    }

    @Test
    fun `OngoingConflict when only QUIZ ongoing with window open (active session)`() = runBlocking {
        coEvery { unclosedMissionUiManager.getUnclosedMissions() } returns
            listOf(MissionType.QUIZ)
        every { quizUiManager.getCurrentOpenedQuizWindowTag() } returns
            WindowTag.MVB_SKETCH_RESPONSE_START_QUIZ

        assertEquals(
            MvbQuizCollectionWindowModel.DispatchResult.OngoingConflict,
            sut.dispatchSelectedQuiz(),
        )
    }

    @Test
    fun `CancelOngoingAndRetry when only QUIZ ongoing without window (orphan quizId)`() = runBlocking {
        coEvery { unclosedMissionUiManager.getUnclosedMissions() } returns
            listOf(MissionType.QUIZ)
        every { quizUiManager.getCurrentOpenedQuizWindowTag() } returns WindowTag.NONE

        assertEquals(
            MvbQuizCollectionWindowModel.DispatchResult.CancelOngoingAndRetry,
            sut.dispatchSelectedQuiz(),
        )
    }

    @Test
    fun `CancelOngoingAndRetry takes priority — BATCH absent and QUIZ window closed`() = runBlocking {
        // Same as the previous case but documents the (false, true, false) branch explicitly
        // so the test inventory matches the kdoc decision table 1:1.
        coEvery { unclosedMissionUiManager.getUnclosedMissions() } returns
            listOf(MissionType.QUIZ)
        every { quizUiManager.getCurrentOpenedQuizWindowTag() } returns WindowTag.NONE

        val result = sut.dispatchSelectedQuiz()

        assertTrue(
            "expected CancelOngoingAndRetry, got $result",
            result is MvbQuizCollectionWindowModel.DispatchResult.CancelOngoingAndRetry,
        )
    }

    // ── Early-out guards ───────────────────────────────────────────────────

    @Test
    fun `SystemError when no quiz selected`() = runBlocking {
        val freshSut = newSut() // never call selectQuiz; selectedQuiz stays null
        coEvery { unclosedMissionUiManager.getUnclosedMissions() } returns emptyList()

        assertEquals(
            MvbQuizCollectionWindowModel.DispatchResult.SystemError,
            freshSut.dispatchSelectedQuiz(),
        )
    }
}
