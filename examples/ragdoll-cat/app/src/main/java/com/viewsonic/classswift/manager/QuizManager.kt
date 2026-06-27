package com.viewsonic.classswift.manager

import android.content.Context
import com.viewsonic.classswift.api.LessonApiService
import com.viewsonic.classswift.api.QuizApiService
import com.viewsonic.classswift.api.body.CreateQuizBody
import com.viewsonic.classswift.api.body.DiscloseQuizBody
import com.viewsonic.classswift.api.body.UpdateMultipleStudentPointsBody
import com.viewsonic.classswift.api.body.UpdateQuizStatusBody
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.api.response.CreateQuizResponse
import com.viewsonic.classswift.api.response.DiscloseQuizResponse
import com.viewsonic.classswift.api.response.UnclosedQuizResponse
import com.viewsonic.classswift.api.response.UpdateQuizStatusResponse
import com.viewsonic.classswift.api.response.data.QuizAnswerData
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.datastore.QuizDataStore
import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.info.StudentQuizzingInfo
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizStatus
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.socket.quiz.SubmitQuizListAnswerSocketMessage
import com.viewsonic.classswift.data.socket.quiz.SubmitQuizStringAnswerSocketMessage
import com.viewsonic.classswift.data.socket.quiz.data.AnswerData
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.data.state.SelectionOptionType
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.manager.StudentManager.StudentChangeReason
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.windowmodel.quiz.MultipleChoiceWindowModel.Companion.DEFAULT_OPTION_COUNT
import com.viewsonic.classswift.ui.windowmodel.quiz.MultipleChoiceWindowModel.Companion.DEFAULT_OPTION_TYPE
import com.viewsonic.classswift.ui.windowmodel.quiz.MultipleChoiceWindowModel.Companion.DEFAULT_SELECTION_TYPE
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.utils.extension.localizedContext
import com.viewsonic.classswift.utils.extension.repeatWithDelay
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber

class QuizManager(
    private val context: Context,
    private val csWindowManager: CSWindowManager,
    private val socketManager : SocketManager,
    private val accountManager: AccountManager,
    private val quizApiService: QuizApiService,
    private val lessonApiService: LessonApiService,
    private val classroomManager: ClassroomManager,
    private val studentManager: StudentManager,
    private val quizDataStore: QuizDataStore
) {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var socketDataJob: Job? = null
    private var quizResultPollingJob: Job? = null
    private var socketConnectStateJob: Job? = null
    private var studentChangeReasonJob: Job? = null
    private var ongoingJob: Job? = null
    private var seatEventCollectJob: Job? = null
    private var seatEventDebounceJob: Job? = null

    // todo refactor later, ui data shouldn't in manager class
    private val _quizzingUiState = MutableStateFlow(QuizzingUiState())
    val quizzingUiState: StateFlow<QuizzingUiState> = _quizzingUiState.asStateFlow()

    private val _refreshFailedFlow = MutableSharedFlow<Unit>()
    val refreshFailedFlow: SharedFlow<Unit> = _refreshFailedFlow.asSharedFlow()

    var quizId: String = ""
        private set
    var quizImageKey: String = ""
    var quizStatus: QuizStatus = QuizStatus.UNSPECIFIED
    var quizStartTimeInMillis: Long = 0

    val currentOngoingQuizId: String?
        get() = quizId.takeIf { it.isNotEmpty() }

    var quizResultInfoList: List<QuizAnswerResultInfo> = emptyList()
        private set

    fun clearCurrentQuizInfo() {
        quizId = ""
        quizImageKey = ""
        quizStatus = QuizStatus.UNSPECIFIED
        quizStartTimeInMillis = 0
    }

    fun checkQuizCategory(windowTag: WindowTag): QuizCategory {
        return when (windowTag) {
            WindowTag.WINDOW_TEXT_TRUE_FALSE_START_QUIZ,
            WindowTag.TRUE_FALSE_EDIT_QUIZ,
            WindowTag.TRUE_FALSE_START_QUIZ -> {
                QuizCategory.TRUE_FALSE
            }
            WindowTag.WINDOW_TEXT_MULTIPLE_CHOICE_START_QUIZ,
            WindowTag.MULTIPLE_CHOICE_EDIT_QUIZ,
            WindowTag.MULTIPLE_CHOICE_START_QUIZ -> {
                QuizCategory.MULTIPLE_SELECTION
            }
            WindowTag.WINDOW_TEXT_SHORT_ANSWER_START_QUIZ,
            WindowTag.SHORT_ANSWER_EDIT_QUIZ,
            WindowTag.MVB_SHORT_ANSWER_EDIT_QUIZ,
            WindowTag.MVB_SHORT_ANSWER_START_QUIZ,
            WindowTag.SHORT_ANSWER_START_QUIZ -> {
                QuizCategory.SHORT_ANSWER
            }
            WindowTag.AUDIO_EDIT_QUIZ,
            WindowTag.AUDIO_START_QUIZ -> {
                QuizCategory.AUDIO
            }
            WindowTag.POLL_EDIT_QUIZ,
            WindowTag.POLL_START_QUIZ -> {
                QuizCategory.POLL
            }
            WindowTag.BATCH_START_QUIZ,
            WindowTag.BATCH_QUIZ_RESULT -> {
                QuizCategory.BATCH_QUIZ
            }
            else -> {
                // TODO: add other case for QuizGenerator
                QuizCategory.UNSPECIFIED
            }
        }
    }
    // endregion

    // region For all Quiz Stages and Flow operations
    fun initCollection() {
        initSocketConnectionObservation()
        initSocketDataObservation()
        initStudentChangeReasonObservation()
        initSeatEventObservation()
    }

    /** Socket 連線狀態 → 連上時補套答題；斷線時 5 秒輪詢補套答題。 */
    private fun initSocketConnectionObservation() {
        socketConnectStateJob?.cancel()
        socketConnectStateJob = coroutineScope.launch(Dispatchers.IO) {
            socketManager.connectionStateSharedFlow.collect {
                when (socketManager.isConnected()) {
                    true -> {
                        quizResultPollingJob?.cancel()
                        updateLatestQuizResults()
                    }
                    false -> {
                        quizResultPollingJob?.cancel()
                        quizResultPollingJob = coroutineScope.repeatWithDelay(5000) {
                            updateLatestQuizResults()
                        }
                    }
                }
            }
        }
    }

    /** Socket 資料事件 → 處理學生答題 EVENT_SUBMIT_QUIZ。 */
    private fun initSocketDataObservation() {
        socketDataJob?.cancel()
        socketDataJob = coroutineScope.launch(Dispatchers.IO) {
            socketManager.receivedEventDataFlow.distinctUntilChanged().collect { receivedEventData ->
                Timber.d("[onReceived] : ${receivedEventData.event} -> ${receivedEventData.messageJsonObject}")
                when(receivedEventData.event) {
                    SocketManager.ReceivedEvent.EVENT_SUBMIT_QUIZ -> {
                        // Receive the event from Socket.IO indicating that the student has submitted an answer.
                        receivedEventData.messageJsonObject?.let { jsonObject ->
                            updateStudentAnswer(jsonObject)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * StudentChangeReason flow → SocketReconnected 走 [handleSocketReconnectionEvent]，
     * DISCLOSE_ANSWER 階段的 ReleaseSeat 標記學生離席（status 由
     * [StudentQuizzingInfo.fromStudentInfo] 直接以 ReleaseSeat 後 newStudent.status=INACTIVE 反映）。
     *
     * 註：QUIZZING 階段的座位變動改由 [initSeatEventObservation] 統一從後端拿，
     * 不再在這裡做 local mutation。QUIZ_RESULTS 階段 freeze 不處理。
     */
    private fun initStudentChangeReasonObservation() {
        studentChangeReasonJob?.cancel()
        studentChangeReasonJob = coroutineScope.launch(Dispatchers.IO) {
            studentManager.studentChangeReasonFlow.collect { studentChangeReason ->
                if (studentChangeReason is StudentChangeReason.SocketReconnected) {
                    handleSocketReconnectionEvent(studentChangeReason.newStudents)
                    return@collect
                }

                val currentQuizzingUiState = _quizzingUiState.value
                Timber.d("quizzingUiState: ${currentQuizzingUiState.quizState}")

                if (currentQuizzingUiState.quizState != QuizState.DISCLOSE_ANSWER) return@collect
                if (studentChangeReason !is StudentChangeReason.ReleaseSeat) return@collect

                val currentList = currentQuizzingUiState.studentQuizzingInfoList.toMutableList()
                val index = currentList.indexOfFirst { it.serialNumber == studentChangeReason.newStudent.serialNumber }
                if (index >= 0) {
                    currentList[index] = StudentQuizzingInfo.fromStudentInfo(context.localizedContext(), studentChangeReason.newStudent)
                }
                _quizzingUiState.update {
                    it.copy(
                        attendanceCount = getAttendanceCount(currentList),
                        answerCount = getAnswerCount(currentList),
                        studentQuizzingInfoList = currentList
                    )
                }
            }
        }
    }

    /**
     * VSFT-8612: 仿 [JoinClassWindowModel] 的座位事件後端同步機制。
     * 座位 socket event → [SEAT_EVENT_DEBOUNCE_MS] debounce → fetch (occupied_only=true) → merge。
     * 僅在 QUIZZING 階段觸發；DISCLOSE_ANSWER / QUIZ_RESULTS freeze。
     */
    private fun initSeatEventObservation() {
        seatEventCollectJob?.cancel()
        seatEventCollectJob = coroutineScope.launch(Dispatchers.IO) {
            studentManager.studentSeatEventFlow.collect {
                if (_quizzingUiState.value.quizState != QuizState.QUIZZING) return@collect
                seatEventDebounceJob?.cancel()
                seatEventDebounceJob = coroutineScope.launch(Dispatchers.IO) {
                    delay(SEAT_EVENT_DEBOUNCE_MS)
                    fetchAndMergeStudentList()
                }
            }
        }
    }

    /**
     * VSFT-8612: 仿 [JoinClassWindowModel.fetchStudentInfoList] 的後端同步機制。
     * 後端 (occupied_only=true) 為 source of truth：取得最新已佔位學生 → merge 進 quizzing list →
     * （選擇性）套用 unclosedQuiz 答題資料。所有 QUIZZING 階段的 list 變動入口統一走此函式。
     *
     * Caller：[setStudentQuizzingList]、seat event 訂閱、[refreshOngoingQuizState]、
     * [handleSocketReconnectionEvent] QUIZZING 分支。
     *
     * 失敗處理（PR review feedback）：
     * - **fetch 失敗仍走 merge**：使用 [StudentManager.getCurrentList] cache（由 socket events
     *   in-place 即時維護）作為 fallback backendList，確保 cold-start UI 不空白並反映
     *   JoinClass 階段已加入的學生。Caller 仍收到 `false`，可選擇 emit failure。
     * - **merge atomic in update {}**：CAS loop 內計算，避免 fetch 期間 socket 答案被覆寫。
     * - **answer fetch 失敗 silent**：list 已成功 → 不影響 caller success。下次 socket
     *   polling 會補上答題（[initSocketConnectionObservation] 連線狀態觸發
     *   [updateLatestQuizResults]，斷線時 5 秒輪詢）。
     *
     * @param applyQuizAnswers 是否在 fetch + merge 後額外打 unclosedQuiz API 補上答題狀態。
     *   Refresh / 重連用 true（保證最新答題）；座位變動 / 畫面載入用 false（答題由 socket 即時更新）。
     * @return true = list fetch 成功；false = list fetch 失敗（但已 fallback cache merge）。
     */
    private suspend fun fetchAndMergeStudentList(applyQuizAnswers: Boolean = false): Boolean {
        if (_quizzingUiState.value.quizState != QuizState.QUIZZING) return false
        val lessonId = classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId
        if (lessonId.isEmpty()) return false

        val fetchSuccess = studentManager.fetchStudentInfoList(lessonId, occupiedOnly = true)
        val backendList = studentManager.getCurrentList()

        // Atomic merge in CAS loop — 避免 T0 讀 snapshot 到 T2 寫之間被 socket
        // EVENT_SUBMIT_QUIZ 插隊覆寫答題；fetch 失敗時 backendList 為 cache，仍是有用的同步結果。
        _quizzingUiState.update { state ->
            val merged = mergeQuizzingListWithBackend(
                currentList = state.studentQuizzingInfoList,
                backendList = backendList,
                builder = { StudentQuizzingInfo.fromStudentInfo(context.localizedContext(), it) }
            )
            state.copy(
                attendanceCount = getAttendanceCount(merged),
                answerCount = getAnswerCount(merged),
                studentQuizzingInfoList = merged
            )
        }

        if (!fetchSuccess) return false

        if (applyQuizAnswers) {
            val answerSuccess = updateLatestQuizResults()
            if (!answerSuccess) {
                Timber.w("[fetchAndMergeStudentList] updateLatestQuizResults failed; will be retried by polling")
            }
        }
        return true
    }

    private suspend fun handleOngoingEvent(newAttendanceList: List<StudentInfo>) {
        // 無需判斷階段，更新到最新即可
        updateToLatestStatus(newAttendanceList)
    }

    private suspend fun handleSocketReconnectionEvent(newAttendanceList: List<StudentInfo>) {
        val currentQuizState = quizzingUiState.value.quizState
        if (currentQuizState == QuizState.QUIZ_RESULTS) {
            return
        }

        when (currentQuizState) {
            QuizState.QUIZZING -> {
                // VSFT-8612: 走後端 source-of-truth (occupied_only=true) + 套答題，
                // 與 Refresh 統一行為，不再倚賴 studentManager.getCurrentList() 的可能過舊 cache。
                fetchAndMergeStudentList(applyQuizAnswers = true)
            }
            QuizState.DISCLOSE_ANSWER -> {
                val currentList = quizzingUiState.value.studentQuizzingInfoList.toMutableList()
                // 只移除離席的學生(包含作答資料)
                newAttendanceList.forEachIndexed { index, studentInfo ->
                    if (studentInfo.status == StudentInfo.Status.INACTIVE) {
                        currentList[index] = currentList[index].copy(
                            displayName = studentInfo.getActualDisplayName(context.localizedContext()),
                            studentId = "",
                            status = StudentInfo.Status.INACTIVE,
                            answerDataList = mutableListOf(),
                            canShowAnswer = false
                        )
                    }
                }

                _quizzingUiState.update {
                    it.copy(
                        answerCount = getAnswerCount(currentList),
                        attendanceCount = getAttendanceCount(currentList),
                        studentQuizzingInfoList = currentList
                    )
                }
            }
            else -> {}
        }
    }

    /**
     * 從後端 [getUnclosedQuiz] 拿最新答題並套用到目前 quizzing list。
     * Caller：socket 連線狀態變動、[fetchAndMergeStudentList] 在 applyQuizAnswers=true 時。
     *
     * @return true = 後端有回 unclosedQuiz 且套用成功；false = API 失敗或無 ongoing quiz。
     */
    private suspend fun updateLatestQuizResults(): Boolean {
        val response = getUnclosedQuiz() ?: return false
        val currentQuizInfoList = _quizzingUiState.value.studentQuizzingInfoList.toMutableList()
        val quizResultsList = response.unclosedQuizData.quizResults
        for (quizResult in quizResultsList) {
            val index = currentQuizInfoList.indexOfFirst { it.studentId == quizResult.studentId }
            if (index != -1) {
                when (quizResult.quizAnswerData) {
                    is QuizAnswerData.Numbers -> {
                        quizResult.quizAnswerData.list.takeIf { it.isNotEmpty() }?.let { answerIdList ->
                            val answerDataList = answerIdList.map { answerId ->
                                AnswerData(answerId, "")
                            }
                            currentQuizInfoList[index].answerDataList.clear()
                            currentQuizInfoList[index].answerDataList.addAll(answerDataList)
                        }
                    }
                    is QuizAnswerData.Text -> {
                        quizResult.quizAnswerData.content.takeIf { it.isNotEmpty() }?.let { content ->
                            currentQuizInfoList[index] = currentQuizInfoList[index].copy(
                                answerStringData = content
                            )
                        }
                    }
                }
            }
        }

        _quizzingUiState.update {
            it.copy(
                answerCount = getAnswerCount(currentQuizInfoList),
                attendanceCount = getAttendanceCount(currentQuizInfoList),
                studentQuizzingInfoList = currentQuizInfoList
            )
        }
        return true
    }

    private suspend fun updateToLatestStatus(newAttendanceList: List<StudentInfo>): Boolean {
        val unclosedQuiz = getUnclosedQuiz() ?: return false
        // 更新全部學生清單：若有學生在斷線時退出座位或教室，則不會包含在 newAttendanceList 中
        val newStudentQuizzingInfoList = newAttendanceList.map {
            StudentQuizzingInfo.fromStudentInfo(context.localizedContext(), it)
        }.toMutableList()

        // 更新在 newAttendanceList 中的學生作答資料
        val quizResultsList = unclosedQuiz.unclosedQuizData.quizResults
        for (quizResult in quizResultsList) {
            val index = newStudentQuizzingInfoList.indexOfFirst { it.studentId == quizResult.studentId }
            if (index != -1) {
                when (quizResult.quizAnswerData) {
                    is QuizAnswerData.Numbers -> {
                        quizResult.quizAnswerData.list.takeIf { it.isNotEmpty() }?.let { answerIdList ->
                            val answerDataList = answerIdList.map { answerId ->
                                AnswerData(answerId, "")
                            }
                            newStudentQuizzingInfoList[index].answerDataList.clear()
                            newStudentQuizzingInfoList[index].answerDataList.addAll(answerDataList)
                        }
                    }
                    is QuizAnswerData.Text -> {
                        quizResult.quizAnswerData.content.takeIf { it.isNotEmpty() }?.let { content ->
                            newStudentQuizzingInfoList[index] = newStudentQuizzingInfoList[index].copy(
                                answerStringData = content
                            )
                        }
                    }
                }
            }
        }

        _quizzingUiState.update {
            it.copy(
                answerCount = getAnswerCount(newStudentQuizzingInfoList),
                attendanceCount = getAttendanceCount(newStudentQuizzingInfoList),
                studentQuizzingInfoList = newStudentQuizzingInfoList
            )
        }
        return true
    }

    private fun updateStudentAnswer(socketMessage: JSONObject) {
        if (_quizzingUiState.value.quizState != QuizState.QUIZZING) {
            return
        }
        val currentList = quizzingUiState.value.studentQuizzingInfoList.toMutableList()

        when (QuizSharedUiInfo.quizType) {
            QuizType.TRUE_FALSE,
            QuizType.SINGLE_SELECT,
            QuizType.MULTIPLE_SELECT,
            QuizType.SINGLE_POLL,
            QuizType.MULTIPLE_POLL -> {
                val typedSocketMessage = SubmitQuizListAnswerSocketMessage.fromJSONObject(socketMessage)
                val index = currentList.indexOfFirst { it.studentId == typedSocketMessage.studentId }
                Timber.d("[onReceived] updateStudentAnswer - index: ${index}, studentId: ${typedSocketMessage.studentId}")
                if (index >= 0) {
                    val oldStudent = currentList[index]
                    currentList[index] = oldStudent.copy(
                        answerDataList = typedSocketMessage.answerData.toMutableList(),
                        answerStringData = "",
                        canShowAnswer = false
                    )
                }
            }
            QuizType.SHORT_ANSWER,
            QuizType.RECORD, -> {
                val typedSocketMessage = SubmitQuizStringAnswerSocketMessage.fromJSONObject(socketMessage)
                val index = currentList.indexOfFirst { it.studentId == typedSocketMessage.studentId }
                Timber.d("[onReceived] updateStudentAnswer - index: ${index}, studentId: ${typedSocketMessage.studentId}")
                if (index >= 0) {
                    val oldStudent = currentList[index]
                    currentList[index] = oldStudent.copy(
                        answerDataList = mutableListOf(),
                        answerStringData = typedSocketMessage.answerData,
                        canShowAnswer = false
                    )
                }
            }
            else -> {}
        }

        _quizzingUiState.update {
            it.copy(
                answerCount = getAnswerCount(currentList),
                studentQuizzingInfoList = currentList
            )
        }
    }

    fun releaseCollection() {
        socketDataJob?.cancel()
        studentChangeReasonJob?.cancel()
        quizResultPollingJob?.cancel()
        socketConnectStateJob?.cancel()
        seatEventCollectJob?.cancel()
        seatEventDebounceJob?.cancel()
    }


    // when create quiz success, to save multiple option info
    fun saveMultipleOptionInfos() {
        Timber.d("[startOngoingProcess]: saveMultipleOptionInfos")
        coroutineScope.launch(Dispatchers.IO) {
            quizDataStore.setOptionCount(QuizSharedUiInfo.quizOptionCount)
            quizDataStore.setOptionType(QuizSharedUiInfo.quizOptionType.toString())
            quizDataStore.setSelectionType(QuizSharedUiInfo.singleOrMultipleSelectionType.toString())
        }
    }

    // leave class should reset multiple choice option info in data store
    fun resetMultipleOptionInfos() {
        Timber.d("[startOngoingProcess]: resetMultipleOptionInfos")
        coroutineScope.launch(Dispatchers.IO) {
            QuizSharedUiInfo.quizOptionType = DEFAULT_OPTION_TYPE
            QuizSharedUiInfo.quizOptionCount = DEFAULT_OPTION_COUNT
            QuizSharedUiInfo.singleOrMultipleSelectionType = DEFAULT_SELECTION_TYPE
            quizDataStore.setOptionCount(DEFAULT_OPTION_COUNT)
            quizDataStore.setOptionType(DEFAULT_OPTION_TYPE.toString())
            quizDataStore.setSelectionType(DEFAULT_SELECTION_TYPE.toString())
        }
    }

    suspend fun updateQuizStatus(type: UpdateQuizStatusType): UpdateQuizStatusResponse? = withContext(Dispatchers.IO) {
        when (val response = quizApiService.updateQuizStatus(accountManager.getBearerToken(), quizId, UpdateQuizStatusBody(type))) {
            is ApiResponse.Success -> {
                quizStatus = response.data.quizData.status
                when (type) {
                    UpdateQuizStatusType.FINISH -> {
                        when (QuizSharedUiInfo.quizType) {
                            QuizType.SINGLE_POLL,
                            QuizType.MULTIPLE_POLL,
                            QuizType.SHORT_ANSWER,
                            QuizType.RECORD-> {
                                AmplitudeEventBuilder(AmplitudeConstant.EventName.QUIZ_END)
                                    .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                                    .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                                    .appendEventProperty(AmplitudeFactory.EventPropertyType.QUIZ_DATA)
                                    .send()
                            }
                            else -> {}
                        }
                    }
                    UpdateQuizStatusType.CANCEL,
                    UpdateQuizStatusType.CLOSE -> {
                        // Clear quizId to indicate that the quiz flow has completed.
                        clearCurrentQuizInfo()
                    }
                }
                return@withContext response.data
            }
            else -> {
                return@withContext null
            }
        }
    }

    private suspend fun getUnclosedQuiz(): UnclosedQuizResponse? = withContext(Dispatchers.IO) {
        val response = quizApiService.unclosedQuiz(
            accountManager.getBearerToken(),
            classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId)

        when (response) {
            is ApiResponse.Success -> response.data
            else -> {
                return@withContext null
            }
        }
    }

    private fun getAttendanceCount(studentQuizzingInfoList: List<StudentQuizzingInfo>): Int {
        return studentQuizzingInfoList.count { it.status == StudentInfo.Status.ACTIVE }
    }

    private fun getAnswerCount(studentQuizzingInfoList: List<StudentQuizzingInfo>): Int {
        return studentQuizzingInfoList.count { it.answerDataList.isNotEmpty() || it.answerStringData.isNotEmpty()}
    }

    fun changeQuizState(quizState: QuizState) {
        Timber.d("[changeQuizState]: change UI state to ${quizState.name}")
        _quizzingUiState.update {
            it.copy(
                quizState = quizState
            )
        }
    }

    private fun setDiscloseAnswerData(optionList: List<QuizOption>) {
        val mapped = optionList.map { DiscloseQuizResponse.Data.fromOption(it) }
        val discloseAnswerData = if (mapped.any { it.isAnswer }) {
            mapped.filter { it.isAnswer }
        } else {
            mapped.filter { it.isAiAnswer }
        }
        _quizzingUiState.update {
            it.copy(
                discloseAnswerData = discloseAnswerData
            )
        }
    }
    // endregion

    // region EditQuiz stage
    suspend fun createQuiz(createQuizBody: CreateQuizBody): CreateQuizResponse? = withContext(Dispatchers.IO) {
        val apiResponse = quizApiService.createQuiz(
            accountManager.getBearerToken(),
            classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
            createQuizBody)

        when (apiResponse) {
            is ApiResponse.Success -> {
                return@withContext apiResponse.data.apply {
                    quizId = this.createQuizData.quizId
                    quizStatus = this.createQuizData.quiz.status
                    quizImageKey = this.createQuizData.quiz.imgKey
                    quizStartTimeInMillis = this.createQuizData.quiz.startTime.toLong() * 1000L
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.QUIZ_START)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.QUIZ_DATA)
                        .send()
                }
            }
            is ApiResponse.HttpFailure -> {
                // Sonar S7610: raw errorBody may contain PII / tokens; log only structured fields.
                Timber.e(
                    "[createQuiz] HttpFailure code=${apiResponse.responseCode} " +
                        "quiz_type=${createQuizBody.quizType} option_type=${createQuizBody.optionType} " +
                        "source_type=${createQuizBody.sourceType}",
                )
                return@withContext null
            }
            is ApiResponse.Rfc7807Failure -> {
                Timber.e(
                    "[createQuiz] Rfc7807Failure code=${apiResponse.responseCode} " +
                        "quiz_type=${createQuizBody.quizType} error=${apiResponse.error}",
                )
                return@withContext null
            }
            is ApiResponse.ExceptionFailure -> {
                Timber.e(apiResponse.exception, "[createQuiz] ExceptionFailure quiz_type=${createQuizBody.quizType}")
                return@withContext null
            }
            is ApiResponse.NetworkDisconnected -> {
                Timber.w("[createQuiz] NetworkDisconnected quiz_type=${createQuizBody.quizType}")
                return@withContext null
            }
        }
    }

    /**
     * Creates a new quiz, automatically cancelling any stale server-side quiz that blocks creation.
     *
     * When [createQuiz] fails, we call [getUnclosedQuiz] to check whether an orphaned quiz (e.g.
     * closed offline) is the cause. If so, cancel it and retry once.
     */
    suspend fun createQuizCancellingOngoingIfNeeded(createQuizBody: CreateQuizBody): CreateQuizResponse? {
        createQuiz(createQuizBody)?.let { return it }

        val staleQuizId = getUnclosedQuiz()?.unclosedQuizData?.quizId
        if (staleQuizId.isNullOrBlank()) return null

        Timber.d("[createQuizCancellingOngoingIfNeeded] cancelling stale quiz: $staleQuizId")
        quizId = staleQuizId
        val cancelResult = try {
            updateQuizStatus(UpdateQuizStatusType.CANCEL)
        } catch (t: Throwable) {
            quizId = ""
            throw t
        }
        if (cancelResult == null) {
            Timber.w("[createQuizCancellingOngoingIfNeeded] cancel failed for quizId: $staleQuizId")
            quizId = ""
            return null
        }

        return createQuiz(createQuizBody)
    }

    // endregion

    // region Quizzing stage
    fun setStudentQuizzingList() {
        if (QuizSharedUiInfo.isOngoing) {
            val currentStudentInfoList = studentManager.getCurrentList()
            ongoingJob?.cancel()
            ongoingJob = coroutineScope.launch(Dispatchers.IO) {
                handleOngoingEvent(currentStudentInfoList)
            }
        } else {
            // VSFT-8612: 取代原本 getCurrentList() snapshot，改用後端 occupied_only=true。
            // 確保 Quiz 開始當下清單即為後端最新「已入座」名單。
            ongoingJob?.cancel()
            ongoingJob = coroutineScope.launch(Dispatchers.IO) {
                fetchAndMergeStudentList()
            }
        }
    }

    /**
     * Re-sync student list & answer states from server (Refresh button in MVB Quizzing UI).
     * VSFT-8612: 走 [fetchAndMergeStudentList](applyQuizAnswers=true)，跟 socket seat event /
     * socket 重連走同一條後端 source-of-truth 路徑。Emits to [refreshFailedFlow] when failed.
     *
     * 非 QUIZZING 階段視為 no-op（不算 failure）— 避免 fetchAndMergeStudentList 內部的
     * state check 把「不在 QUIZZING」誤觸發 refreshFailedFlow 顯示錯誤 toast。
     */
    fun refreshOngoingQuizState() {
        if (_quizzingUiState.value.quizState != QuizState.QUIZZING) {
            Timber.d("[refreshOngoingQuizState] skip: not in QUIZZING state")
            return
        }
        ongoingJob?.cancel()
        ongoingJob = coroutineScope.launch(Dispatchers.IO) {
            val success = fetchAndMergeStudentList(applyQuizAnswers = true)
            if (!success) {
                _refreshFailedFlow.emit(Unit)
            }
        }
    }

    fun updateStudentQuizAnsweringVisibility(quizAnsweringInfo: QuizAnsweringInfo) {
        val currentList = quizzingUiState.value.studentQuizzingInfoList.toMutableList()
        if (quizAnsweringInfo.serialNumber >= 0) {
            val index = currentList.indexOfFirst { it.serialNumber == quizAnsweringInfo.serialNumber }
            Timber.d("[updateStudentQuizAnsweringStatus] index: ${index}, display: ${quizAnsweringInfo.displayName}, canShowAnswer: ${quizAnsweringInfo.canShowAnswer}")
            if (index >= 0) {
                val oldStudent = currentList[index]
                currentList[index] = oldStudent.copy(
                    canShowAnswer = !oldStudent.canShowAnswer
                )

                _quizzingUiState.update {
                    it.copy(
                        studentQuizzingInfoList = currentList
                    )
                }
            }
        }
    }

    fun updateStudentAudioAnsweringVisibility(audioAnswerInfo: AudioAnswerInfo) {
        val currentList = quizzingUiState.value.studentQuizzingInfoList.toMutableList()
        if (audioAnswerInfo.serialNumber >= 0) {
            val index = currentList.indexOfFirst { it.serialNumber == audioAnswerInfo.serialNumber }
            Timber.d("[updateStudentQuizAnsweringStatus] index: ${index}, display: ${audioAnswerInfo.displayName}, canShowAnswer: ${audioAnswerInfo.canShowAnswer}")
            if (index >= 0) {
                val oldStudent = currentList[index]
                currentList[index] = oldStudent.copy(
                    canShowAnswer = !oldStudent.canShowAnswer
                )

                _quizzingUiState.update {
                    it.copy(
                        studentQuizzingInfoList = currentList
                    )
                }
            }
        }
    }
    // endregion

    // region DiscloseAnswer stage
    suspend fun discloseAnswer(correctOptionsId: List<Int>) : Boolean = withContext(Dispatchers.IO) {
        Timber.d("[discloseAnswer]: request discloseAnswer API")
        callDiscloseAnswerAPI(correctOptionsId, QuizSharedUiInfo.quizType)?.let { response ->
            _quizzingUiState.update {
                it.copy(
                    quizState = QuizState.QUIZ_RESULTS,
                    discloseAnswerData = response.discloseData
                )
            }
            when (QuizSharedUiInfo.quizType) {
                QuizType.TRUE_FALSE -> {
                    getTrueFalseResultInfos()
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.QUIZ_END)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.QUIZ_DATA)
                        .send()
                }
                QuizType.SINGLE_SELECT,
                QuizType.MULTIPLE_SELECT -> {
                    getMultipleChoiceResultInfos(QuizSharedUiInfo.quizOptionType)
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.QUIZ_END)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.QUIZ_DATA)
                        .send()
                }
                QuizType.SINGLE_POLL,
                QuizType.MULTIPLE_POLL -> {
                    getPollAnswerResultInfos(QuizSharedUiInfo.quizOptionType)
                }
                else -> {}
            }
            true
        } ?: run {
            false
        }
    }

    private suspend fun callDiscloseAnswerAPI(correctOptions: List<Int>, quizType: QuizType): DiscloseQuizResponse? = withContext(Dispatchers.IO) {
        when (val response = quizApiService.discloseQuiz(accountManager.getBearerToken(), quizId, DiscloseQuizBody(correctOptions, quizType))) {
            is ApiResponse.Success -> {
                return@withContext response.data
            }
            else -> {
                return@withContext null
            }
        }
    }

    // endregion

    // region QuizResults stage
    suspend fun updateStudentsPoint(correctAnswerStudentIdList: List<String>, points: Int): Boolean = withContext(Dispatchers.IO) {
        Timber.d("[updateStudentsPoint]: add point($points) for correct students")
        val isSuccessful = updateMultipleStudentsPoint(correctAnswerStudentIdList, points)
        if (isSuccessful) {
            studentManager.fetchStudentInfoList(
                classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
                occupiedOnly = false
            )
        }
        isSuccessful
    }

    private suspend fun updateMultipleStudentsPoint(studentIds: List<String>, points: Int): Boolean = withContext(Dispatchers.IO) {
        val response = lessonApiService.updateMultipleStudentsPoint(
            accountManager.getBearerToken(),
            classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
            UpdateMultipleStudentPointsBody(studentIds, points)
        )
        when (response) {
            is ApiResponse.Success -> {
                return@withContext true
            }
            else -> {
                return@withContext false
            }
        }
    }

    fun resetRelatedDataInUiState() {
        _quizzingUiState.update {
            it.copy(
                attendanceCount = 0,
                answerCount = 0,
                discloseAnswerData = emptyList(),
                studentQuizzingInfoList = emptyList()
            )
        }
        quizResultInfoList = emptyList()
    }
    // endregion

    fun getTrueFalseResultInfos(): List<QuizAnswerResultInfo> {
        val correctOptionId = quizzingUiState.value.discloseAnswerData[0].optionId
        quizResultInfoList = quizzingUiState.value.studentQuizzingInfoList.map {
            QuizAnswerResultInfo.fromStudentTrueFalseQuizzingInfo(it, correctOptionId)
        }
        return quizResultInfoList
    }

    fun getMultipleChoiceResultInfos(multipleQuizOptionType: QuizOptionType): List<QuizAnswerResultInfo> {
        val studentQuizAnsweringInfoList = quizzingUiState.value.studentQuizzingInfoList.map {
            var answeringState = AnsweringState.ABSENT
            if (it.status == StudentInfo.Status.ACTIVE) {
                answeringState = AnsweringState.NOT_ANSWER
            }
            if (it.answerDataList.isNotEmpty()) {
                answeringState = AnsweringState.ANSWERED
            }
            QuizAnsweringInfo.fromStudentMultipleChoiceQuizzingInfo(
                it,
                QuizSharedUiInfo.quizOptionType,
                QuizSharedUiInfo.quizType,
                answeringState,
                it.answerDataList
            )
        }
        quizResultInfoList = studentQuizAnsweringInfoList.map {
            QuizAnswerResultInfo.fromStudentMultipleChoiceAnsweringInfo(it, quizzingUiState.value.discloseAnswerData, multipleQuizOptionType)
        }
        return quizResultInfoList
    }

    fun getShortAnswerResultInfos(): List<QuizAnswerResultInfo> {
        val studentQuizAnsweringInfoList = _quizzingUiState.value.studentQuizzingInfoList.map {
            var answeringState = AnsweringState.ABSENT
            if (it.status == StudentInfo.Status.ACTIVE) {
                answeringState = AnsweringState.NOT_ANSWER
            }
            if (it.answerStringData.isNotEmpty()) {
                answeringState = AnsweringState.ANSWERED
            }
            QuizAnsweringInfo.fromStudentShortAnswerQuizzingInfo(
                it,
                answeringState
            )
        }
        quizResultInfoList = studentQuizAnsweringInfoList.map {
            QuizAnswerResultInfo.fromStudentShortAnswerAnsweringInfo(it)
        }
        return quizResultInfoList
    }

    // for quiz random draw
    fun setAudioAnswerResultInfos(answerList: List<AudioAnswerInfo>) {
        quizResultInfoList = answerList.map { AudioAnswerInfo.toQuizAnswerResultInfo(it) }
    }

    fun setBatchAnswerResultInfos(batchQuizResults: List<QuizAnswerResultInfo>) {
        quizResultInfoList = batchQuizResults
    }

    fun getPollAnswerResultInfos(multipleQuizOptionType: QuizOptionType): List<QuizAnswerResultInfo> {
        val studentQuizAnsweringInfoList = _quizzingUiState.value.studentQuizzingInfoList.map {
            var answeringState = AnsweringState.ABSENT
            if (it.status == StudentInfo.Status.ACTIVE) {
                answeringState = AnsweringState.NOT_ANSWER
            }
            if (it.answerDataList.isNotEmpty()) {
                answeringState = AnsweringState.ANSWERED
            }
            QuizAnsweringInfo.fromStudentPollQuizzingInfo(
                it,
                QuizSharedUiInfo.quizOptionType,
                QuizSharedUiInfo.quizType,
                answeringState,
                it.answerDataList
            )
        }
        quizResultInfoList = studentQuizAnsweringInfoList.map {
            QuizAnswerResultInfo.fromStudentPollAnswerAnsweringInfo(it, multipleQuizOptionType)
        }
        return quizResultInfoList
    }



    data class QuizzingUiState(
        val quizState: QuizState = QuizState.QUIZZING,
        val attendanceCount: Int = 0,
        val answerCount: Int = 0,
        val discloseAnswerData: List<DiscloseQuizResponse.Data> = emptyList(),
        val studentQuizzingInfoList: List<StudentQuizzingInfo> = emptyList()
    )

    enum class QuizCategory {
        UNSPECIFIED, TRUE_FALSE, MULTIPLE_SELECTION, POLL, SHORT_ANSWER, AUDIO, QUIZ_GENERATOR, BATCH_QUIZ
    }

    companion object {
        private const val SEAT_EVENT_DEBOUNCE_MS = 300L

        /**
         * VSFT-8612: pure merge logic for [fetchAndMergeStudentList]。
         *
         * 後端 (occupied_only=true) 為 source of truth：以 [backendList] rebuild quizzing 清單。
         * 對齊 [refreshOngoingQuizState] / [handleSocketReconnectionEvent] 統一語意。
         *
         * - 仍在 backend：以 [builder] 重建 StudentQuizzingInfo，並 attach 既有
         *   answerDataList / answerStringData / canShowAnswer
         * - 既有清單沒有的新學生：以 [builder] 建立新項（VSFT-8612 核心：Quiz 開始後加入）
         * - 既有但 backend 未回傳（已離席）：直接從結果中移除，不留 INACTIVE 殘影
         *
         * 結果依 serialNumber 升冪排序。
         */
        internal fun mergeQuizzingListWithBackend(
            currentList: List<StudentQuizzingInfo>,
            backendList: List<StudentInfo>,
            builder: (StudentInfo) -> StudentQuizzingInfo
        ): List<StudentQuizzingInfo> {
            val currentBySerial = currentList.associateBy { it.serialNumber }
            return backendList.map { backend ->
                val existing = currentBySerial[backend.serialNumber]
                if (existing != null) {
                    builder(backend).apply {
                        answerDataList.addAll(existing.answerDataList)
                        answerStringData = existing.answerStringData
                        canShowAnswer = existing.canShowAnswer
                    }
                } else {
                    builder(backend)
                }
            }.sortedBy { it.serialNumber }
        }
    }
}
