package com.viewsonic.classswift.ui.windowmodel.quiz

import android.content.Context
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.QuizManager.QuizzingUiState
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel.Companion.TRUE_OPTION_INDEX
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class QuizStartWindowModel(private val context: Context, private val quizManager: QuizManager, private val classroomManager: ClassroomManager) : IWindowModel {

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var updateStudentsPointJob: Job? = null
    private var discloseAnswerJob: Job? = null

    private val _quizzingUiState = MutableStateFlow(QuizzingUiState())
    val quizzingUiState: StateFlow<QuizzingUiState> = _quizzingUiState.asStateFlow()

    private val _updateStudentsPointErrorFlow = MutableSharedFlow<String>()
    val updateStudentsPointErrorFlow = _updateStudentsPointErrorFlow

    private val _discloseAnswerErrorFlow = MutableSharedFlow<String>()
    val discloseAnswerErrorFlow = _discloseAnswerErrorFlow

    val refreshFailedFlow = quizManager.refreshFailedFlow

    init {
        initCollection()
        quizManager.initCollection()
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            quizManager.quizzingUiState.collect { uiState ->
                _quizzingUiState.update { uiState }
            }
        }
    }

    fun setStudentQuizzingList() {
        quizManager.setStudentQuizzingList()
    }

    fun refreshOngoingQuizState() {
        quizManager.refreshOngoingQuizState()
    }

    private fun getCorrectAnswerStudentIdList(): List<String> {
        var studentIdList: List<String> = emptyList()
        when (QuizSharedUiInfo.quizType) {
            QuizType.TRUE_FALSE,
            QuizType.SINGLE_SELECT,
            QuizType.SINGLE_POLL -> {
                val currentList = _quizzingUiState.value.studentQuizzingInfoList
                val correctOptionId = _quizzingUiState.value.discloseAnswerData[0].optionId
                studentIdList = currentList.filter {
                    it.studentId.isNotEmpty() && it.answerDataList.isNotEmpty() && it.answerDataList[0].optionId == correctOptionId
                }.map {
                    it.studentId
                }
            }
            QuizType.MULTIPLE_SELECT,
            QuizType.MULTIPLE_POLL -> {
                // TODO: handle case for multiple select or poll
            }
            else -> {}
        }
        return studentIdList
    }


    @Synchronized
    fun updateStudentsPoint(correctAnswerStudentIdList: List<String> = getCorrectAnswerStudentIdList(), points: Int, isManually: Boolean = false) {
        if (updateStudentsPointJob?.isActive == true) {
            Timber.d("[updateStudentsPoint]: still processing")
            return
        }
        if (isManually) {
            classroomManager.updateHasAddedScoreManually(true)
        }
        updateStudentsPointJob = coroutineScope.launch(Dispatchers.IO) {
            if(!quizManager.updateStudentsPoint(correctAnswerStudentIdList, points))
            {
                _updateStudentsPointErrorFlow.emit(context.getString(R.string.student_list_error_msg_failed_to_add_point))
            }
        }
    }

    suspend fun updateQuizStatus(type: UpdateQuizStatusType): Boolean = withContext(Dispatchers.IO) {
        quizManager.updateQuizStatus(type)?.let {
            return@withContext true
        } ?:run {
            return@withContext false
        }
    }

    fun discloseAnswer(correctOptionsId: List<Int>) {
        discloseAnswerJob?.cancel()
        discloseAnswerJob = coroutineScope.launch(Dispatchers.IO) {
            if (!quizManager.discloseAnswer(correctOptionsId)) {
                _discloseAnswerErrorFlow.emit(context.getString(R.string.quiz_error_msg_close_quiz))
            }
        }
    }

    fun getTrueFalseResultInfos(): List<QuizAnswerResultInfo> {
        return quizManager.getTrueFalseResultInfos()
    }

    // region For TrueFalseQuiz operations
    /**
     * return Pair<trueNumber: Int, falseNumber: Int>
     */
    fun getTrueFalseNumberPair(): Pair<Int, Int> {
        var trueNumber = 0
        var falseNumber = 0

        val currentList = quizzingUiState.value.studentQuizzingInfoList
        currentList.forEach {
            if (it.answerDataList.isNotEmpty()) {
                if (it.answerDataList[0].optionId == TRUE_OPTION_INDEX) {
                    trueNumber++
                } else {
                    falseNumber++
                }
            }
        }
        return Pair(trueNumber, falseNumber)
    }

    fun getNoAnswerNumber(): Int {
        val attendanceCount = quizzingUiState.value.attendanceCount
        val answerCount = quizzingUiState.value.answerCount
        return attendanceCount - answerCount
    }
    // endregion

    // todo do refactor later
    fun updateStudentQuizAnsweringVisibility(quizAnsweringInfo: QuizAnsweringInfo) {
        quizManager.updateStudentQuizAnsweringVisibility(quizAnsweringInfo)
    }

    // todo do refactor later
    fun updateStudentAudioAnswerVisibility(audioAnswerInfo: AudioAnswerInfo) {
        quizManager.updateStudentAudioAnsweringVisibility(audioAnswerInfo)
    }

    // todo do refactor later
    fun changeQuizState(quizState: QuizState) {
        quizManager.changeQuizState(quizState)
    }

    fun resetQuizStartTime() {
        quizManager.quizStartTimeInMillis = 0
    }

    override fun onCleared() {
        quizManager.releaseCollection()
        QuizSharedUiInfo.screenshotImageUri = ""
        QuizSharedUiInfo.quizContent = ""
        QuizSharedUiInfo.quizOptionList.clear()
        quizManager.resetRelatedDataInUiState()
    }

}