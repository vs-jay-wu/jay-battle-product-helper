package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.api.BatchQuizApiService
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.socket.BatchQuizzesStudentSubmittedSocketMessage
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.BatchQuizManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.manager.StudentManager.StudentChangeReason
import com.viewsonic.classswift.utils.TimeUtils
import com.viewsonic.classswift.utils.extension.milliSecondToTimerUnit
import com.viewsonic.classswift.utils.extension.startTimerInMilliSec
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class BatchQuizStartWindowModel(
    private val apiService: BatchQuizApiService,
    private val socketManager: SocketManager,
    private val studentManager: StudentManager,
    private val classroomManager: ClassroomManager,
    private val batchQuizManager: BatchQuizManager,
    private val accountManager: AccountManager
) : IWindowModel {

    private val _updateUiEventFlow = MutableSharedFlow<BatchQuizStartingUiEvent>(extraBufferCapacity = 1)
    val updateUiEventFlow = _updateUiEventFlow.asSharedFlow()
    private val _uiStateFlow = MutableStateFlow(BatchQuizStartingUiState())
    val uiStateFlow = _uiStateFlow.asStateFlow()
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var socketConnectStateJob: Job? = null
    private var receivedStudentAnswerJob: Job? = null
    private var studentChangeReasonJob: Job? = null
    private var startTimerJob: Job? = null
    // Thread-safe collections (no duplicates)
    private val joinStudentIdSet: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val answerStudentIdSet: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private var startTimeInMillis = System.currentTimeMillis()
    init {
        // Initialize the join snapshot before any collector starts mutating the sets.
        updateJoinStudentIdList()
        _uiStateFlow.value = BatchQuizStartingUiState(answerStudentIdSet.size, joinStudentIdSet.size)
        initCollection()
    }

    private fun initCollection() {
        socketConnectStateJob = coroutineScope.launch(Dispatchers.IO) {
            socketManager.connectionStateSharedFlow.collect {
                when (socketManager.isConnected()) {
                    true -> {
                        //todo cancel pulling get student join list api
                    }
                    false -> {
                        //todo pulling get student submitted answer api
                    }
                }
            }
        }
        receivedStudentAnswerJob = coroutineScope.launch(Dispatchers.IO) {
            socketManager.receivedEventDataFlow.distinctUntilChanged().collect { receivedEventData ->
                when (receivedEventData.event) {
                    SocketManager.ReceivedEvent.EVENT_BATCH_QUIZZES_STUDENT_SUBMITTED -> {
                        receivedEventData.messageJsonObject?.let { jsonObject ->
                            Timber.d("[BatchQuizStartWindowModel] receive EVENT_BATCH_QUIZZES_STUDENT_SUBMITTED: $jsonObject")
                            val studentSubmittedMessage: BatchQuizzesStudentSubmittedSocketMessage =
                                BatchQuizzesStudentSubmittedSocketMessage.fromJSONObject(jsonObject)
                            if (joinStudentIdSet.contains(studentSubmittedMessage.studentId) && studentSubmittedMessage.batchQuizzesId == batchQuizManager.batchQuizzesId) {
                                answerStudentIdSet.add(studentSubmittedMessage.studentId)
                                sendSubmittedStateEvent()
                            }
                        }
                    }
                    else -> return@collect
                }
            }
        }

        // For student join or leave
        studentChangeReasonJob?.cancel()
        studentChangeReasonJob = coroutineScope.launch(Dispatchers.IO) {
            studentManager.studentChangeReasonFlow.collect { studentChangeReason ->
                if (studentChangeReason is StudentChangeReason.SocketReconnected) {
                    updateJoinStudentIdList()
                    checkSubmittedStudentList()
                    return@collect
                }
                val newStudent = when (studentChangeReason) {
                    is StudentChangeReason.ChooseSeat -> studentChangeReason.newStudent
                    is StudentChangeReason.RejoinSeat -> studentChangeReason.newStudent
                    is StudentChangeReason.ReleaseSeat -> {
                        // remove answered student id when student leave class
                        if (answerStudentIdSet.contains(studentChangeReason.oldStudent.studentId)) {
                            answerStudentIdSet.remove(studentChangeReason.oldStudent.studentId)
                        }
                        if (joinStudentIdSet.contains(studentChangeReason.oldStudent.studentId)) {
                            joinStudentIdSet.remove(studentChangeReason.oldStudent.studentId)
                        }
                        studentChangeReason.newStudent
                    }
                    is StudentChangeReason.SetStudentName -> studentChangeReason.newStudent
                    else -> return@collect
                }
                if (newStudent.isJoinedClass()) joinStudentIdSet.add(newStudent.studentId)
                sendSubmittedStateEvent()
            }
        }
    }

    fun cancelBatchQuiz() {
        coroutineScope.launch {
            _updateUiEventFlow.emit(BatchQuizStartingUiEvent.CancelQuizResult(batchQuizManager.cancelBatchQuiz()))
        }
    }

    fun finishAndDiscloseBatchQuiz() {
        coroutineScope.launch {
            if (!batchQuizManager.finishBatchQuiz()) {
                _updateUiEventFlow.emit(BatchQuizStartingUiEvent.DiscloseQuizResult(false))
                return@launch
            }
            val response = apiService.discloseQuiz(
                accountManager.getBearerToken(),
                classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
                batchQuizManager.batchQuizzesId
            )
            when (response) {
                is ApiResponse.Success -> {
                    batchQuizManager.setSkipResultPointUpdateInCurrentWindow(false)
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.BATCH_QUIZ_ENDED)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.BATCH_QUIZ_ID, batchQuizManager.batchQuizzesId)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.QUIZ_DETAIL_LIST, batchQuizManager.amplitudeQuizDetailJsonArray.toString())
                        .send()
                    _updateUiEventFlow.emit(BatchQuizStartingUiEvent.DiscloseQuizResult(true))
                }
                else ->  _updateUiEventFlow.emit(BatchQuizStartingUiEvent.DiscloseQuizResult(false))
            }
        }
    }

    private fun sendSubmittedStateEvent() {
        _uiStateFlow.value = BatchQuizStartingUiState(answerStudentIdSet.size, joinStudentIdSet.size)
    }

    private fun updateJoinStudentIdList() {
        val attendantStudents: List<StudentInfo> = studentManager.getCurrentAttendantList()
        joinStudentIdSet.clear()
        joinStudentIdSet.addAll(attendantStudents.map { it.studentId }.toMutableList())
    }

    private fun checkSubmittedStudentList() {
        val joinedSnapshot = joinStudentIdSet.toHashSet()
        val answeredSnapshot = answerStudentIdSet.toHashSet()
        answeredSnapshot.retainAll(joinedSnapshot)
        answeredSnapshot.clear()
        joinStudentIdSet.addAll(answeredSnapshot)
        sendSubmittedStateEvent()
    }

    fun startTimer() {
        if (startTimerJob?.isActive != true) {
            var previousTimeString = ""
            startTimeInMillis = if (batchQuizManager.startTimeInMillis == 0L) System.currentTimeMillis() else batchQuizManager.startTimeInMillis
            val timeDiffInMillis = TimeUtils.getTimeDiffFromCurrentTimeInMillis(startTimeInMillis)
            startTimerJob?.cancel()
            startTimerJob = coroutineScope.startTimerInMilliSec(
                startTimeInMillis,
                timeDiffInMillis,
                onTick = { tickMillisecond ->
                    val timeString = tickMillisecond.milliSecondToTimerUnit()
                    if (previousTimeString != timeString) {
                        previousTimeString = timeString
                        _updateUiEventFlow.emit(BatchQuizStartingUiEvent.UpdateTime(timeString))
                    }
                }
            )
        }
    }

    override fun onCleared() {
        coroutineScope.cancel()
    }

    sealed class BatchQuizStartingUiEvent {
        data class UpdateTime(val time: String) : BatchQuizStartingUiEvent()
        data class CancelQuizResult(val result: Boolean) : BatchQuizStartingUiEvent()
        data class DiscloseQuizResult(val result: Boolean) : BatchQuizStartingUiEvent()
    }

    data class BatchQuizStartingUiState(
        val submittedCount: Int = 0,
        val joinCount: Int = 0
    )
}
