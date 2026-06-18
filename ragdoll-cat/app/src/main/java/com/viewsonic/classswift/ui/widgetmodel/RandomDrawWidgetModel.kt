package com.viewsonic.classswift.ui.widgetmodel

import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.data.socket.SelectStudentSocketMessage
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class RandomDrawWidgetModel(
    private val socketManager: SocketManager,
    private val studentManager: StudentManager,
    private val quizManager: QuizManager,
    private val windowManager: CSWindowManager
) {

    private val _hasAttendedUiState = MutableStateFlow<RandomDrawUiState>(RandomDrawUiState.Idle)
    val hasAttendedUiState: StateFlow<RandomDrawUiState> = _hasAttendedUiState
    private val _pickupStudentUiState = MutableSharedFlow<PickedStudent>()
    val pickupStudentUiState: SharedFlow<PickedStudent> = _pickupStudentUiState

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var collectionStudentListJob: Job? = null

    private var pickType: PickAnswerType = PickAnswerType.CORRECT
    private var studentAnsweredInfoList: List<QuizAnswerResultInfo> = emptyList()
    private var randomDrawAnswerInfos: List<QuizAnswerResultInfo> = emptyList()

    private var quizWindowTag: WindowTag = WindowTag.TRUE_FALSE_START_QUIZ
    var isCorrectType: Boolean = true

    init {
        initCollect()
    }

    private fun initCollect() {
        collectionStudentListJob = coroutineScope.launch(Dispatchers.IO) {
            studentManager.studentInfoListFlow.collect { studentInfoList ->
                val hasJoined = studentInfoList.any { it.isJoinedClass() }
                //new student joined class, should update list.
                if (hasJoined) {
                    setPickTypeParticipants()
                } else {
                    _hasAttendedUiState.value = RandomDrawUiState.HasAttendStudent(false)
                }
            }
        }
    }

    fun syncStudentAnsweredInfos() {
        studentAnsweredInfoList = quizManager.quizResultInfoList
        setPickTypeParticipants()
    }

    fun setPickAnswerType(type: PickAnswerType) {
        pickType = type
        setPickTypeParticipants()
    }

    fun sendSocketEvent(info :QuizAnswerResultInfo) {
        Timber.d("send selected student info by socket, student ID: ${info.studentId}")
        socketManager.emit(
            SocketManager.EmittedEvent.SELECT_STUDENT,
            SelectStudentSocketMessage(info.studentId).toJSONObject()
        )
    }

    private fun setPickTypeParticipants() {
        Timber.d("studentAnsweredInfoList counts: ${studentAnsweredInfoList.size}")
        var pickAnswerInfos: List<QuizAnswerResultInfo> = emptyList()
        when (pickType) {
            PickAnswerType.CORRECT -> {
                pickAnswerInfos = studentAnsweredInfoList.filter { it.answerResultState == AnswerResultState.CORRECT }
            }

            PickAnswerType.INCORRECT -> {
                pickAnswerInfos = studentAnsweredInfoList.filter { it.answerResultState == AnswerResultState.INCORRECT }
            }

            PickAnswerType.NO_ANSWERED -> {
                pickAnswerInfos = studentAnsweredInfoList.filter { it.answerResultState == AnswerResultState.NO_ANSWER }
            }

            PickAnswerType.ANSWERED -> {
                pickAnswerInfos = studentAnsweredInfoList.filter { it.answerResultState == AnswerResultState.ANSWERED}
            }

            PickAnswerType.ALL -> {
                pickAnswerInfos = studentAnsweredInfoList.filter { it.answerResultState != AnswerResultState.ABSENT }
            }
        }
        randomDrawAnswerInfos = pickAnswerInfos.filter { info ->
            studentManager.getCurrentList().any { it.isJoinedClass() && it.studentId == info.studentId }
        }
        Timber.d("randomDrawAnswerInfos counts: ${randomDrawAnswerInfos.size}")
        _hasAttendedUiState.value = RandomDrawUiState.HasAttendStudent(randomDrawAnswerInfos.isNotEmpty())
    }

    fun isSocketConnected() = socketManager.isConnected()

    suspend fun pickStudent() {
        if (randomDrawAnswerInfos.isEmpty()) {
            _pickupStudentUiState.emit(PickedStudent(null))
        } else {
            _pickupStudentUiState.emit(PickedStudent(randomDrawAnswerInfos.random()))
        }
    }

    fun setTag(tag: WindowTag) {
        quizWindowTag = tag
    }

    fun bringQuizWindowToFront() {
        coroutineScope.launch(Dispatchers.Main) {
            windowManager.bringWindowToTop(quizWindowTag)
        }
    }

    fun onCleared() {
        collectionStudentListJob?.cancel()
        studentAnsweredInfoList = emptyList()
        randomDrawAnswerInfos = emptyList()
    }

    sealed class RandomDrawUiState {
        data object Idle : RandomDrawUiState()
        data class HasAttendStudent(val value: Boolean) : RandomDrawUiState()
    }

    data class PickedStudent(val info: QuizAnswerResultInfo?)

    enum class PickAnswerType {
        CORRECT,
        INCORRECT,
        ANSWERED,
        NO_ANSWERED,
        ALL
    }
}