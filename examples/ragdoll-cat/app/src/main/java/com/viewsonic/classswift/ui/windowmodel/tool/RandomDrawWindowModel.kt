package com.viewsonic.classswift.ui.windowmodel.tool

import com.viewsonic.classswift.constant.AppConstants.ONE_SEC_DELAY
import com.viewsonic.classswift.constant.AppConstants.THREE_SEC_DELAY
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.socket.SelectStudentSocketMessage
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber


class RandomDrawWindowModel(
    private val socketManager: SocketManager,
    private val studentManager: StudentManager,
    private val networkManager: NetworkManager
) :
    IWindowModel {
    private val _uiState = MutableStateFlow<RandomDrawUiState>(RandomDrawUiState.InitUi)
    val uiState: StateFlow<RandomDrawUiState> = _uiState.asStateFlow()
    private val _sendSocketEventError = MutableSharedFlow<Unit>()
    val sendSocketEventError = _sendSocketEventError.asSharedFlow()
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var selectStudentJob: Job? = null
    private var showNoParticipantJob: Job? = null
    private var collectNetworkStateJob: Job? = null

    var selectedStudentInfo: StudentInfo? = null
        private set

    //After play animation 0.8sec, send selected student event.
    private val sendEventTime = 800L

    init {
        initCollection()
    }

    private fun initCollection() {
        collectNetworkStateJob = coroutineScope.launch {
            networkManager.networkAvailabilityState.collect { hasNetwork ->
                if (hasNetwork) {
                    _uiState.emit(RandomDrawUiState.HasNetWork)
                } else {
                    selectStudentJob?.cancel()
                    _uiState.emit(RandomDrawUiState.ShowNoNetworkView)
                }
            }
        }
    }

    fun selectStudent() {
        val studentList = ArrayList<StudentInfo>()
        studentManager.getCurrentList().forEach { info ->
            if (info.isJoinedClass()) studentList.add(info)
        }
        selectedStudentInfo = studentList.randomOrNull()
        selectedStudentInfo?.let {
            selectStudentJob?.cancel()
            selectStudentJob = coroutineScope.launch {
                _uiState.emit(RandomDrawUiState.PlayDiceAnimation)
                delay(sendEventTime)
                if (sendSelectStudentEvent()) {
                    Timber.tag("RandomDrawEvent").d("sendSelectStudentEvent result is true, emit ShowStudentInfo")
                    delay(ONE_SEC_DELAY - sendEventTime)
                    _uiState.emit(RandomDrawUiState.ShowStudentInfo)
                } else {
                    Timber.tag("RandomDrawEvent").d("sendSelectStudentEvent result is false, emit sendSocketEventError")
                    _sendSocketEventError.emit(Unit)
                    _uiState.emit(RandomDrawUiState.InitUi)
                }
            }
        } ?: run {
            showNoParticipantJob?.cancel()
            showNoParticipantJob = coroutineScope.launch {
                _uiState.emit(RandomDrawUiState.NoAttendedStudent)
                delay(THREE_SEC_DELAY)
                _uiState.emit(RandomDrawUiState.InitUi)
            }
        }
    }

    private fun sendSelectStudentEvent(): Boolean {
        selectedStudentInfo?.let { info ->
            return socketManager.emit(
                SocketManager.EmittedEvent.SELECT_STUDENT,
                SelectStudentSocketMessage(info.studentId).toJSONObject()
            )
        } ?: run {
            return false
        }
    }

    override fun onCleared() {
        selectStudentJob?.cancel()
        collectNetworkStateJob?.cancel()
        selectedStudentInfo = null
    }

    sealed class RandomDrawUiState {
        data object InitUi : RandomDrawUiState()
        data object PlayDiceAnimation : RandomDrawUiState()
        data object ShowStudentInfo : RandomDrawUiState()
        data object NoAttendedStudent : RandomDrawUiState()
        data object ShowNoNetworkView : RandomDrawUiState()
        data object HasNetWork : RandomDrawUiState()
    }
}