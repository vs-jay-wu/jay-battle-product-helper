package com.viewsonic.classswift.uimanager

import android.content.Context
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.coordinator.RecordsCoordinator
import com.viewsonic.classswift.data.task.HasTaskInProgressInfo
import com.viewsonic.classswift.data.task.QuizRespondConflictEvent
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class PushRespondUiManager(
    private val csWindowManager: CSWindowManager,
    private val applicationContext: Context,
    private val classroomManager: ClassroomManager,
    private val studentManager: StudentManager,
    private var taskApiService: TaskApiService,
    private val socketManager: SocketManager,
    private val accountManager: AccountManager
) {
    private var recordsCoordinator: RecordsCoordinator ? = null

    private val _quizRespondConflictEventFlow = MutableSharedFlow<QuizRespondConflictEvent>(replay = 0)
    val quizRespondConflictEventFlow = _quizRespondConflictEventFlow.asSharedFlow()

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)



    fun stopPushRespond(): Boolean {
        coroutineScope.launch {
            _quizRespondConflictEventFlow.emit(
                QuizRespondConflictEvent(
                    isStopPushRespond = true
                )
            )
        }
        // if all tasks are ended, doesn't need to hide push and respond window
        if (recordsCoordinator?.getInProgressTask()?.isEmpty() == true) {
            return true
        }

        //The "Stop Push Respond" feature will be optimized in the future.
        // It will be changed to actively call an API to close all tasks.
        //Stopping push respond only hides the window; it doesn't actually close it.
        // The push respond feature is only truly closed when the lesson ends.
        return hidePushRespondWindow()
    }

    fun closePushRespond() {
        val isPushRespondExist = csWindowManager.isWindowExisted(
            WindowTag.PUSH_RESPOND
        )
        if(isPushRespondExist) {
            csWindowManager.removeWindow(WindowTag.PUSH_RESPOND)
        }
        recordsCoordinator = null
    }

    suspend fun hasTaskInProgress(): HasTaskInProgressInfo {
        if(recordsCoordinator == null) {
            recordsCoordinator = RecordsCoordinator(
                applicationContext = applicationContext,
                classroomManager = classroomManager,
                studentManager = studentManager,
                taskApiService = taskApiService,
                socketManager = socketManager,
                accountManager = accountManager
            )
        }
        return recordsCoordinator?.hasTaskInProgress() ?: HasTaskInProgressInfo(
            isUnexpectedState = true,
            hasTaskInProgress = false
        )
    }

    fun isPushRespondWindowShown(): Boolean = csWindowManager.isWindowExisted(WindowTag.PUSH_RESPOND) && !csWindowManager.isWindowHidden(WindowTag.PUSH_RESPOND)

    private fun hidePushRespondWindow(): Boolean {
        csWindowManager.hideWindow(WindowTag.PUSH_RESPOND, isRecordHiddenState = true)
        val isHide = csWindowManager.isWindowHidden(WindowTag.PUSH_RESPOND)
        return isHide
    }
}