package com.viewsonic.classswift.uimanager

import com.viewsonic.classswift.data.clientapp.myviewboard.notifier.MyViewBoardEventNotifier
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.task.HasTaskInProgressInfo
import com.viewsonic.classswift.manager.BatchQuizManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Covers the priority / suppression rules in [UnclosedMissionUiManager.getLastUnclosedMission]
 * and [UnclosedMissionUiManager.getUnclosedMissions] that this PR introduced:
 *   - QUIZ wins over PUSH_AND_RESPOND_TASK when any quiz state is active
 *     (sketch quizzing window OR legacy quizId OR Edit window)
 *   - PUSH is suppressed even when `hasTaskInProgress()` returns true, because sketch
 *     dispatches via `batchCreateTasks` and stale task rows can pollute that flag
 *   - BATCH_QUIZZES is independent of the quiz / push axis
 *
 * Uses Koin's GlobalContext + mockk fakes for the seven `by inject(...)` dependencies; methods
 * under test run on Dispatchers.IO so `runBlocking` is enough (matches project convention —
 * see feedback_test_mockk_patterns memory).
 */
class UnclosedMissionUiManagerTest {

    private lateinit var sut: UnclosedMissionUiManager

    private val csWindowManager: CSWindowManager = mockk(relaxed = true)
    private val classroomManager: ClassroomManager = mockk(relaxed = true)
    private val batchQuizManager: BatchQuizManager = mockk(relaxed = true)
    private val pushRespondUiManager: PushRespondUiManager = mockk(relaxed = true)
    private val quizManager: QuizManager = mockk(relaxed = true)
    private val quizUiManager: QuizUiManager = mockk(relaxed = true)
    private val eventNotifier: MyViewBoardEventNotifier = mockk(relaxed = true)

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { csWindowManager }
                    single { classroomManager }
                    single { batchQuizManager }
                    single { pushRespondUiManager }
                    single { quizManager }
                    single { quizUiManager }
                    single { eventNotifier }
                },
            )
        }
        // Default benign baseline — each test overrides what it needs.
        every { quizUiManager.isQuizEditWindowExisted() } returns false
        every { quizUiManager.getCurrentOpenedQuizWindowTag() } returns WindowTag.NONE
        every { quizManager.quizId } returns ""
        every { batchQuizManager.batchQuizzesId } returns ""
        every { pushRespondUiManager.isPushRespondWindowShown() } returns false
        coEvery { pushRespondUiManager.hasTaskInProgress() } returns HasTaskInProgressInfo()

        sut = UnclosedMissionUiManager()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // ── getLastUnclosedMission priority ────────────────────────────────────

    @Test
    fun `getLastUnclosedMission returns NONE when nothing is active`() = runBlocking {
        assertEquals(MissionType.NONE, sut.getLastUnclosedMission())
    }

    @Test
    fun `getLastUnclosedMission returns QUIZ when legacy quizId set`() = runBlocking {
        every { quizManager.quizId } returns "q123"
        assertEquals(MissionType.QUIZ, sut.getLastUnclosedMission())
    }

    @Test
    fun `getLastUnclosedMission returns QUIZ when edit window open`() = runBlocking {
        every { quizUiManager.isQuizEditWindowExisted() } returns true
        assertEquals(MissionType.QUIZ, sut.getLastUnclosedMission())
    }

    @Test
    fun `getLastUnclosedMission returns QUIZ for sketch quizzing window`() = runBlocking {
        every { quizUiManager.getCurrentOpenedQuizWindowTag() } returns WindowTag.MVB_SKETCH_RESPONSE_START_QUIZ
        assertEquals(MissionType.QUIZ, sut.getLastUnclosedMission())
    }

    @Test
    fun `getLastUnclosedMission picks QUIZ over PUSH when both look active`() = runBlocking {
        every { quizManager.quizId } returns "q1"
        every { pushRespondUiManager.isPushRespondWindowShown() } returns true
        coEvery { pushRespondUiManager.hasTaskInProgress() } returns
            HasTaskInProgressInfo(hasTaskInProgress = true)

        assertEquals(MissionType.QUIZ, sut.getLastUnclosedMission())
    }

    @Test
    fun `getLastUnclosedMission returns PUSH_AND_RESPOND_TASK when only push window shown`() = runBlocking {
        every { pushRespondUiManager.isPushRespondWindowShown() } returns true
        assertEquals(MissionType.PUSH_AND_RESPOND_TASK, sut.getLastUnclosedMission())
    }

    @Test
    fun `getLastUnclosedMission returns PUSH_AND_RESPOND_TASK when only hasTaskInProgress`() = runBlocking {
        coEvery { pushRespondUiManager.hasTaskInProgress() } returns
            HasTaskInProgressInfo(hasTaskInProgress = true)
        assertEquals(MissionType.PUSH_AND_RESPOND_TASK, sut.getLastUnclosedMission())
    }

    @Test
    fun `getLastUnclosedMission returns BATCH_QUIZZES when only batch id present`() = runBlocking {
        every { batchQuizManager.batchQuizzesId } returns "b1"
        assertEquals(MissionType.BATCH_QUIZZES, sut.getLastUnclosedMission())
    }

    // ── getUnclosedMissions composition + suppression ──────────────────────

    @Test
    fun `getUnclosedMissions returns empty when nothing active`() = runBlocking {
        assertEquals(emptyList<MissionType>(), sut.getUnclosedMissions())
    }

    @Test
    fun `getUnclosedMissions suppresses PUSH when sketch quizzing left stale task rows`() = runBlocking {
        every { quizUiManager.getCurrentOpenedQuizWindowTag() } returns WindowTag.MVB_SKETCH_RESPONSE_START_QUIZ
        coEvery { pushRespondUiManager.hasTaskInProgress() } returns
            HasTaskInProgressInfo(hasTaskInProgress = true)

        assertEquals(listOf(MissionType.QUIZ), sut.getUnclosedMissions())
    }

    @Test
    fun `getUnclosedMissions suppresses PUSH when push window shown but quiz is active`() = runBlocking {
        every { quizManager.quizId } returns "q1"
        every { pushRespondUiManager.isPushRespondWindowShown() } returns true

        assertEquals(listOf(MissionType.QUIZ), sut.getUnclosedMissions())
    }

    @Test
    fun `getUnclosedMissions includes BATCH_QUIZZES alongside QUIZ`() = runBlocking {
        every { batchQuizManager.batchQuizzesId } returns "b1"
        every { quizManager.quizId } returns "q1"

        assertEquals(
            listOf(MissionType.BATCH_QUIZZES, MissionType.QUIZ),
            sut.getUnclosedMissions(),
        )
    }

    @Test
    fun `getUnclosedMissions returns PUSH when only push side is active`() = runBlocking {
        every { pushRespondUiManager.isPushRespondWindowShown() } returns true

        assertEquals(listOf(MissionType.PUSH_AND_RESPOND_TASK), sut.getUnclosedMissions())
    }

    @Test
    fun `getUnclosedMissions returns PUSH plus BATCH when both active without quiz`() = runBlocking {
        every { pushRespondUiManager.isPushRespondWindowShown() } returns true
        every { batchQuizManager.batchQuizzesId } returns "b1"

        assertEquals(
            listOf(MissionType.PUSH_AND_RESPOND_TASK, MissionType.BATCH_QUIZZES),
            sut.getUnclosedMissions(),
        )
    }
}
