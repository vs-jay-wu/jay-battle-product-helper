package com.viewsonic.classswift.ui.windowmodel.task

import com.viewsonic.classswift.coordinator.RecordsCoordinator
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.task.RecordEventInfo
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.ui.widget.task.enums.RecordType
import com.viewsonic.classswift.ui.windowmodel.task.state.PushRespondEvent
import com.viewsonic.classswift.uimanager.PushRespondUiManager
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class PushRespondWindowModel(
    private val csWindowManager: CSWindowManager,
    private val pushRespondUiManager: PushRespondUiManager,
    private val networkManager: NetworkManager,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider,
) : IWindowModel {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private var recordsCoordinator: RecordsCoordinator? = null

    private val _uiEventFlow = MutableSharedFlow<PushRespondEvent>(replay = 0)
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    init {
        observeTaskList()
        observeNetworkStatus()
    }

    fun isMyViewBoardBound() = myViewBoardConnectionStateProvider.isBound()

    fun closeWindow() {
        csWindowManager.hideWindow(WindowTag.PUSH_RESPOND, isRecordHiddenState = true)
    }

    fun setRecordsCoordinator(coordinator: RecordsCoordinator) {
        recordsCoordinator = coordinator
        observeRecordList()
        observeRecordEvent()
    }


    private fun observeNetworkStatus() {
        coroutineScope.launch {
            networkManager.delayInformNetworkAvailabilityState.collect { isNetworkConnected ->
                Timber.d("Network Status: $isNetworkConnected")
                _uiEventFlow.emit(
                    PushRespondEvent.NetworkStatusChange(isNetworkConnected = isNetworkConnected)
                )
            }
        }
    }

    private fun observeTaskList() {
        coroutineScope.launch(Dispatchers.Default) {
            pushRespondUiManager.quizRespondConflictEventFlow.collect { data ->
                _uiEventFlow.emit(
                    PushRespondEvent.QuizRespondConflict(
                        isStopPushRespond = data.isStopPushRespond
                    )
                )
            }
        }
    }

    private fun observeRecordList() {
        recordsCoordinator?.let {
            coroutineScope.launch {
                it.recordResultFlow.collect { data ->
                    // only content type need update RecordMarkWidget UI.
                    if (data.taskInfo.recordType == RecordType.CONTENT) {
                        _uiEventFlow.emit(PushRespondEvent.UpdateStudentRecordList(data.recordList))
                    }
                }
            }
        }
    }

    private fun observeRecordEvent() {
        recordsCoordinator?.let {
            coroutineScope.launch {
                it.eventFlow.collect { data ->
                    if (data is RecordEventInfo.GetRecordByTaskIdFailed) {
                        _uiEventFlow.emit(PushRespondEvent.GetRecordListFailed)
                    }
                }
            }
        }
    }

    fun getStudentTaskResult(studentId: String) {
        recordsCoordinator?.getStudentTaskResult(studentId = studentId)
    }

    override fun onCleared() {
        //Do something here
        coroutineScope.cancel()
    }
}