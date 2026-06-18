package com.viewsonic.classswift.ui.windowmodel.tool.spinner

import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.socket.SelectStudentSocketMessage
import com.viewsonic.classswift.data.spinner.CandidateStudentInfo
import com.viewsonic.classswift.data.spinner.SpinnerStudentInfo
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.utils.LanguageUtils
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class SpinnerWindowModel(
    private val socketManager: SocketManager,
    private val studentManager: StudentManager,
    private val networkManager: NetworkManager
) : IWindowModel {

    private val url = BuildConfig.SPINNER_URL + "?lang=${LanguageUtils.webLanguageCode}"
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val _uiStateFlow = MutableStateFlow<SpinnerUiState>(SpinnerUiState.Idle)
    private val _uiEventFlow = MutableSharedFlow<SpinnerUiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val uiEventFlow = _uiEventFlow.asSharedFlow()
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        observeNetworkStatus()
    }

    fun getCurrentStudentList(): SpinnerStudentInfo {
        val studentItems = studentManager.getCurrentAttendantList().map {
            CandidateStudentInfo(
                studentId = it.studentId,
                seatNumber = it.displaySeatNumber,
                name = it.displayName
            )
        }
        return SpinnerStudentInfo(data = studentItems)
    }

    fun sendSelectedStudentEvent(studentId: String) {
        Timber.d("sendSelectedStudentEvent student id : $studentId")
        socketManager.emit(
            SocketManager.EmittedEvent.SELECT_STUDENT,
            SelectStudentSocketMessage(studentId).toJSONObject()
        )
    }

    fun sendSpinnerClickedAmplitudeEvent() {
        AmplitudeEventBuilder(AmplitudeConstant.EventName.SPINNER_CLICKED)
            .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
            .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
            .send()
    }

    fun sendSpinnerRemoveClickedAmplitudeEvent() {
        AmplitudeEventBuilder(AmplitudeConstant.EventName.SPINNER_REMOVED_CLICKED)
            .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
            .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
            .send()
    }

    fun getUrl(): String {
        return url
    }

    private fun observeNetworkStatus() {
        coroutineScope.launch {
            networkManager.delayInformNetworkAvailabilityState.collect { isNetworkConnected ->
                Timber.d("Network Status: $isNetworkConnected")
                _uiEventFlow.emit(
                    SpinnerUiEvent.NetworkStatusChange(isNetworkConnected = isNetworkConnected)
                )
            }
        }
    }

    override fun onCleared() {
        //Do nothing
    }
}