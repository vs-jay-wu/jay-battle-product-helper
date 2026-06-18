package com.viewsonic.classswift.ui.windowmodel.tool

import com.viewsonic.classswift.api.RecoverApiService
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.constant.AppConstants.STOPWATCH_INTERVAL_TIME
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.socket.BuzzerResultSocketMessage
import com.viewsonic.classswift.data.socket.SetLessonIdSocketMessage
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.manager.SocketManager.EmittedEvent
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.ui.widget.stopwatch.StopwatchTool
import com.viewsonic.classswift.utils.extension.millionSecondToStopwatch
import com.viewsonic.classswift.utils.extension.repeatWithDelay
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

class BuzzerWindowModel(
    private val socketManager: SocketManager,
    private val apiService: RecoverApiService,
    private val classroomManager: ClassroomManager,
    private val accountManager: AccountManager,
    private val studentManager: StudentManager,
    private val networkManager: NetworkManager
) :
    IWindowModel {
    private val _updateUIFlow = MutableSharedFlow<BuzzerUIEvent>()
    val updateUIFlow = _updateUIFlow.asSharedFlow()
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var stopwatch = StopwatchTool()
    private var waitBuzzerResultJob: Job? = null
    private var socketConnectStateJob: Job? = null
    private var detectNetworkStateJob: Job? = null
    private var startTimerJob: Job? = null
    private var buzzerResultPollingJob: Job? = null
    var isStop = false
        private set

    init {
        initCollection()
    }

    fun startBuzzer(): Boolean {
        if (!socketManager.isConnected()) {
            return false
        }
        startTimerJob?.cancel()
        startSocketCollection()
        stopwatch.reset()
        isStop = false
        startTimerJob = coroutineScope.launch(Dispatchers.IO) {
            stopwatch.start()
            while (stopwatch.isRunning) {
                _updateUIFlow.emit(BuzzerUIEvent.UpdateTime(stopwatch.elapsedMs().millionSecondToStopwatch()))
                delay(STOPWATCH_INTERVAL_TIME)
            }
        }
        socketManager.emit(
            EmittedEvent.START_RACE,
            SetLessonIdSocketMessage(classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId).toJSONObject()
        )
        return true
    }

    fun stopBuzzer(): Boolean {
        if (!socketManager.isConnected()) {
            return false
        }
        isStop = true
        cancelCollection()
        socketManager.emit(
            EmittedEvent.END_RACE,
            SetLessonIdSocketMessage(classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId).toJSONObject()
        )
        return true
    }

    private fun initCollection() {
        detectNetworkStateJob = coroutineScope.launch(Dispatchers.IO) {
            networkManager.networkAvailabilityState.collect { hasNetwork ->
                Timber.d("[BuzzerWindowModel] receive network available state $hasNetwork: ")
                _updateUIFlow.emit(BuzzerUIEvent.NetworkAvailabilityState(hasNetwork))
            }
        }
    }

    private fun startSocketCollection() {
        waitBuzzerResultJob?.cancel()
        waitBuzzerResultJob = coroutineScope.launch(Dispatchers.IO) {
            socketManager.receivedEventDataFlow.distinctUntilChanged().collect { receivedEventData ->
                when (receivedEventData.event) {
                    SocketManager.ReceivedEvent.EVENT_STUDENT_ANSWERED -> {
                        receivedEventData.messageJsonObject?.let { jsonObject ->
                            Timber.d("[BuzzerWindowModel] receive EVENT_STUDENT_ANSWERED: $jsonObject")
                            val resultMessage: BuzzerResultSocketMessage = BuzzerResultSocketMessage.fromJSONObject(jsonObject)
                            val buzzerStudent = studentManager.getCurrentList()
                                .firstOrNull { it.studentId == resultMessage.studentId && it.isJoinedClass() }
                            buzzerStudent?.let {
                                _updateUIFlow.emit(BuzzerUIEvent.AnsweredStudentInfo(it))
                                Timber.d("[BuzzerWindowModel] emit AnsweredStudentInfo: $it")
                                cancelCollection()
                            }?: run {
                                // if not found student in student CurrentList, let time keep going don't do any action.
                                Timber.d("[BuzzerWindowModel] not found student in student CurrentList")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        socketConnectStateJob?.cancel()
        socketConnectStateJob = coroutineScope.launch(Dispatchers.IO) {
            socketManager.connectionStateSharedFlow.collect { connectionState ->
                Timber.d("[BuzzerWindowModel] connectionStateSharedFlow: $connectionState")
                classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId.takeIf { it.isNotEmpty() }?.let { lessonId ->
                    when (socketManager.isConnected()) {
                        true -> {
                            buzzerResultPollingJob?.cancel()
                            getBuzzerResult(lessonId)
                        }
                        false -> {
                            buzzerResultPollingJob?.cancel()
                            buzzerResultPollingJob = coroutineScope.repeatWithDelay(5000) {
                                getBuzzerResult(lessonId)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cancelCollection() {
        stopwatch.reset()
        startTimerJob?.cancel()
        waitBuzzerResultJob?.cancel()
        socketConnectStateJob?.cancel()
        buzzerResultPollingJob?.cancel()
    }

    // socket disconnect, to check student answered or not
    private fun getBuzzerResult(lessonId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            when (val apiResponse = apiService.getBuzzerResult(accountManager.getBearerToken(), lessonId)) {
                is ApiResponse.Success -> {
                    startTimerJob?.cancel()
                    stopwatch.reset()
                    val buzzerStudent = studentManager.getCurrentList()
                        .firstOrNull { it.studentId == apiResponse.data.buzzerResultData.studentId && it.isJoinedClass() }
                    Timber.d("[BuzzerWindowModel] getBuzzerResult buzzerStudent: $buzzerStudent")
                    buzzerStudent?.let {
                        _updateUIFlow.emit(BuzzerUIEvent.AnsweredStudentInfo(it))
                        cancelCollection()
                    }
                }
                else -> {}
            }
        }
    }

    override fun onCleared() {
        stopwatch.reset()
        coroutineScope.cancel()
    }

    sealed class BuzzerUIEvent {
        data class UpdateTime(val time: String) : BuzzerUIEvent()
        data class AnsweredStudentInfo(val studentInfo: StudentInfo) : BuzzerUIEvent()
        data class NetworkAvailabilityState(val available: Boolean) : BuzzerUIEvent()
    }
}