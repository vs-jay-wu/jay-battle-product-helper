package com.viewsonic.classswift.uimanager

import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventMissionStatusPayload
import com.viewsonic.classswift.data.clientapp.myviewboard.notifier.MyViewBoardEventNotifier
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.manager.BatchQuizManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class UnclosedMissionUiManager {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val classroomManager: ClassroomManager by inject(ClassroomManager::class.java)
    private val batchQuizManager: BatchQuizManager by inject(BatchQuizManager::class.java)
    private val pushRespondUiManager: PushRespondUiManager by inject(PushRespondUiManager::class.java)
    private val quizManager: QuizManager by inject(QuizManager::class.java)
    private val quizUiManager: QuizUiManager by inject(QuizUiManager::class.java)
    private val myViewBoardEventNotifier: MyViewBoardEventNotifier by inject(MyViewBoardEventNotifier::class.java)
    private val ongoingMissionSet = mutableSetOf<MissionType>()
    private val notifiedMinimizedSet = mutableSetOf<MissionType>()
    var shouldCheckUnclosedMission: Boolean = false
        private set

    fun setShouldCheckUnclosedMission(shouldCheck: Boolean) {
        Timber.d("[B][setShouldCheckUnclosedMission] : shouldCheck = $shouldCheck")
        this.shouldCheckUnclosedMission = shouldCheck
    }

    /**
     * Sketch response bypasses `quizManager.createQuiz` (uses `taskApiService.batchCreateTasks`
     * instead), so `quizManager.quizId` is never set for it. Tracked here via WindowTag so
     * mission lookups see the sketch quizzing window alongside the other quiz types.
     */
    private fun isMvbSketchResponseQuizzingExisted(): Boolean =
        quizUiManager.getCurrentOpenedQuizWindowTag() == WindowTag.MVB_SKETCH_RESPONSE_START_QUIZ

    /**
     * True when ANY quiz state is in flight — an Edit window, a started quiz (`quizId`), or the
     * MVB sketch quizzing window. Used to gate the PUSH_AND_RESPOND_TASK branch: once a quiz is
     * active, the "tasks in progress" the API reports must not be classified as push-respond,
     * because quiz dispatch (sketch via `batchCreateTasks`, others via legacy paths that may
     * leave rows in `/tasks`) can pollute `hasTaskInProgress()`. Without this gate,
     * `bringOngoingMissionToTop` would look for a (non-existent) PUSH_RESPOND window and the
     * minimized quiz could not be restored.
     */
    private fun isAnyQuizActive(): Boolean =
        quizUiManager.isQuizEditWindowExisted() ||
            quizManager.quizId.isNotEmpty() ||
            isMvbSketchResponseQuizzingExisted()

    suspend fun getLastUnclosedMission(): MissionType = withContext(Dispatchers.IO) {
        return@withContext when {
            isAnyQuizActive() -> MissionType.QUIZ
            pushRespondUiManager.isPushRespondWindowShown() || pushRespondUiManager.hasTaskInProgress().hasTaskInProgress -> MissionType.PUSH_AND_RESPOND_TASK
            batchQuizManager.batchQuizzesId.isNotEmpty() -> MissionType.BATCH_QUIZZES
            else -> MissionType.NONE
        }
    }

    suspend fun getUnclosedMissions(): List<MissionType> = withContext(Dispatchers.IO) {
        val missions = mutableListOf<MissionType>()
        val isQuizActive = isAnyQuizActive()
        // Suppress PUSH_AND_RESPOND_TASK while any quiz is active — `hasTaskInProgress()` may
        // report quiz-side task rows (sketch's batchCreateTasks, or stale rows from past quizzes).
        if (!isQuizActive &&
            (pushRespondUiManager.isPushRespondWindowShown() || pushRespondUiManager.hasTaskInProgress().hasTaskInProgress)
        ) {
            missions.add(MissionType.PUSH_AND_RESPOND_TASK)
        }
        if (batchQuizManager.batchQuizzesId.isNotEmpty()) {
            missions.add(MissionType.BATCH_QUIZZES)
        }
        if (isQuizActive) {
            missions.add(MissionType.QUIZ)
        }
        return@withContext missions
    }

    suspend fun closeUnclosedMissions() = withContext(Dispatchers.IO) {
        getUnclosedMissions().forEach {
            closeMission(it)
        }
    }

    fun notifyMissionOngoingIfNeeded(missionType: MissionType) {
        Timber.d("[B][notifyMissionOngoingIfNeeded] : missionType = $missionType")
        if (missionType == MissionType.NONE) {
            return
        }
        Timber.d("[B][notifyMissionOngoingIfNeeded] : isMissionActuallyOngoing = ${isMissionActuallyOngoing(missionType)}")
        Timber.d("[B][notifyMissionOngoingIfNeeded] : ongoingMissionSet exist = ${ongoingMissionSet.contains(missionType)}")
        if (!isMissionActuallyOngoing(missionType)) {
            return
        }
        if (!ongoingMissionSet.add(missionType)) {
            return
        }
        notifiedMinimizedSet.remove(missionType)
        myViewBoardEventNotifier.notifyMissionStatus(
            missionType,
            EventMissionStatusPayload.Status.ONGOING
        )
    }

    fun notifyMissionMinimizedIfNeeded(missionType: MissionType) {
        Timber.d("[B][notifyMissionMinimizedIfNeeded] : missionType = $missionType")
        if (missionType == MissionType.NONE) {
            return
        }
        Timber.d("[B][notifyMissionMinimizedIfNeeded] : ongoingMissionSet exist = ${ongoingMissionSet.contains(missionType)}")
        if (!ongoingMissionSet.contains(missionType)) {
            return
        }
        // Dedupe consecutive MINIMIZED notifications. Without this, every MinimizeAll
        // IPC re-fires MINIMIZED while the mission is already minimized, racing with
        // the optimistic MissionOngoing emit on the MVB side and flickering the Quiz
        // button selected → unselected → selected.
        if (!notifiedMinimizedSet.add(missionType)) {
            return
        }
        myViewBoardEventNotifier.notifyMissionStatus(
            missionType,
            EventMissionStatusPayload.Status.MINIMIZED
        )
    }

    fun notifyMissionClosedIfNeeded(missionType: MissionType) {
        Timber.d("[B][notifyMissionClosedIfNeeded] : missionType = $missionType")
        if (missionType == MissionType.NONE) {
            return
        }
        Timber.d("[B][notifyMissionClosedIfNeeded] : isMissionActuallyOngoing = ${isMissionActuallyOngoing(missionType)}")
        Timber.d("[B][notifyMissionClosedIfNeeded] : ongoingMissionSet exist = ${ongoingMissionSet.contains(missionType)}")
        if (!isMissionActuallyClosed(missionType)) {
            return
        }
        if (!ongoingMissionSet.remove(missionType)) {
            return
        }
        notifiedMinimizedSet.remove(missionType)
        myViewBoardEventNotifier.notifyMissionStatus(
            missionType,
            EventMissionStatusPayload.Status.CLOSED
        )
    }

    suspend fun bringOngoingMissionToTop(): Boolean = withContext(Dispatchers.Main) {
        val missionType = getLastUnclosedMission()
        val brought = when (missionType) {
            MissionType.QUIZ -> {
                val windowTag = quizUiManager.getCurrentOpenedQuizWindowTag()
                if (windowTag == WindowTag.NONE || !csWindowManager.isWindowExisted(windowTag)) {
                    return@withContext false
                }
                if (csWindowManager.isWindowHidden(windowTag) || csWindowManager.isWindowMinimized(windowTag)) {
                    csWindowManager.showWindow(windowTag)
                }
                csWindowManager.bringWindowToTop(windowTag)
                true
            }

            MissionType.BATCH_QUIZZES -> {
                val windowTag = when {
                    csWindowManager.isWindowExisted(WindowTag.BATCH_QUIZ_RESULT) -> WindowTag.BATCH_QUIZ_RESULT
                    csWindowManager.isWindowExisted(WindowTag.BATCH_START_QUIZ) -> WindowTag.BATCH_START_QUIZ
                    else -> WindowTag.NONE
                }
                if (windowTag == WindowTag.NONE) {
                    return@withContext false
                }
                if (csWindowManager.isWindowHidden(windowTag) || csWindowManager.isWindowMinimized(windowTag)) {
                    csWindowManager.showWindow(windowTag)
                }
                csWindowManager.bringWindowToTop(windowTag)
                true
            }

            MissionType.PUSH_AND_RESPOND_TASK -> {
                val windowTag = WindowTag.PUSH_RESPOND
                if (!csWindowManager.isWindowExisted(windowTag)) {
                    return@withContext false
                }
                if (csWindowManager.isWindowHidden(windowTag) || csWindowManager.isWindowMinimized(windowTag)) {
                    csWindowManager.showWindow(windowTag)
                }
                csWindowManager.bringWindowToTop(windowTag)
                true
            }

            MissionType.NONE -> false
        }
        if (brought) {
            // Resync MVB mission status: bringWindowToTop alone leaves MVB stuck on the
            // last MINIMIZED notification (sent by the preceding MinimizeAllWindows IPC),
            // so re-emit ONGOING here to clear the toolbar minimized indicator.
            notifiedMinimizedSet.remove(missionType)
            myViewBoardEventNotifier.notifyMissionStatus(missionType, EventMissionStatusPayload.Status.ONGOING)
        }
        brought
    }

    suspend fun closeMission(missionType: MissionType): Boolean = withContext(Dispatchers.Main) {
        Timber.d("[B][closeMission] : missionType = $missionType")
        when (missionType) {
            MissionType.QUIZ -> {
                return@withContext if (quizManager.quizId.isNotEmpty()) {
                    val isClosedSuccessful = quizManager.updateQuizStatus(UpdateQuizStatusType.CLOSE) != null
                    if (isClosedSuccessful) {
                        csWindowManager.removeWindow(quizUiManager.getCurrentOpenedQuizWindowTag())
                        notifyMissionClosedIfNeeded(MissionType.QUIZ)
                    }
                    isClosedSuccessful
                } else {
                    // No legacy quizId — e.g. MVB edit, or sketch quizzing (batchCreateTasks only).
                    closeCurrentQuizWindowIfExisted()
                }
            }
            MissionType.BATCH_QUIZZES -> {
                val isClosedSuccessful = batchQuizManager.closeBatchQuiz()
                if (isClosedSuccessful) {
                    csWindowManager.removeWindow(WindowTag.BATCH_START_QUIZ)
                    csWindowManager.removeWindow(WindowTag.BATCH_QUIZ_RESULT)
                    notifyMissionClosedIfNeeded(MissionType.BATCH_QUIZZES)
                }
                return@withContext isClosedSuccessful
            }
            MissionType.PUSH_AND_RESPOND_TASK -> {
                if (pushRespondUiManager.isPushRespondWindowShown()) {
                    csWindowManager.hideWindow(WindowTag.PUSH_RESPOND, isRecordHiddenState = true)
                }
                val isStopped = pushRespondUiManager.stopPushRespond()
                if (isStopped) {
                    notifyMissionClosedIfNeeded(MissionType.PUSH_AND_RESPOND_TASK)
                }
                return@withContext isStopped
            }
            else -> return@withContext true
        }
    }

    /**
     * Removes the topmost tracked quiz window when there is no [QuizManager.quizId] to close via API
     * (sketch Start, MVB edit, etc.). Returns whether a window was removed or none was open.
     */
    private fun closeCurrentQuizWindowIfExisted(): Boolean {
        val windowTag = quizUiManager.getCurrentOpenedQuizWindowTag()
        if (windowTag == WindowTag.NONE || !csWindowManager.isWindowExisted(windowTag)) {
            return true
        }
        csWindowManager.removeWindow(windowTag)
        notifyMissionClosedIfNeeded(MissionType.QUIZ)
        return true
    }

    private fun isMissionActuallyOngoing(missionType: MissionType): Boolean {
        return when (missionType) {
            MissionType.QUIZ -> {
                quizUiManager.getCurrentOpenedQuizWindowTag() != WindowTag.NONE ||
                        quizManager.quizId.isNotEmpty()
            }

            MissionType.BATCH_QUIZZES -> {
                batchQuizManager.batchQuizzesId.isNotEmpty() ||
                        csWindowManager.isWindowExisted(WindowTag.BATCH_START_QUIZ) ||
                        csWindowManager.isWindowExisted(WindowTag.BATCH_QUIZ_RESULT)
            }

            MissionType.PUSH_AND_RESPOND_TASK -> {
                csWindowManager.isWindowExisted(WindowTag.PUSH_RESPOND) &&
                        !csWindowManager.isWindowHidden(WindowTag.PUSH_RESPOND)
            }

            MissionType.NONE -> false
        }
    }

    private fun isMissionActuallyClosed(missionType: MissionType): Boolean {
        return when (missionType) {
            MissionType.QUIZ -> {
                quizUiManager.getCurrentOpenedQuizWindowTag() == WindowTag.NONE &&
                        quizManager.quizId.isEmpty()
            }

            MissionType.BATCH_QUIZZES -> {
                batchQuizManager.batchQuizzesId.isEmpty() &&
                        !csWindowManager.isWindowExisted(WindowTag.BATCH_START_QUIZ) &&
                        !csWindowManager.isWindowExisted(WindowTag.BATCH_QUIZ_RESULT)
            }

            MissionType.PUSH_AND_RESPOND_TASK -> {
                !csWindowManager.isWindowExisted(WindowTag.PUSH_RESPOND) ||
                        csWindowManager.isWindowHidden(WindowTag.PUSH_RESPOND)
            }

            MissionType.NONE -> true
        }
    }
}
