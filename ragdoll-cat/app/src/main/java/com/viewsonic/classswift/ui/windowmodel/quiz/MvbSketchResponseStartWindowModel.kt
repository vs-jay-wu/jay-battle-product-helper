package com.viewsonic.classswift.ui.windowmodel.quiz

import androidx.annotation.VisibleForTesting
import com.viewsonic.classswift.api.response.BatchTasksLatestResponse
import com.viewsonic.classswift.api.response.UpdateTaskResult
import com.viewsonic.classswift.api.response.toSketchStudentCardInfos
import com.viewsonic.classswift.api.response.toSketchTaskInfo
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.constant.AppConstants
import com.viewsonic.classswift.coordinator.SketchMarkHandler
import com.viewsonic.classswift.coordinator.SketchTaskCoordinator
import com.viewsonic.classswift.data.enum.SketchAnswerStatus
import com.viewsonic.classswift.data.info.SketchStudentCardInfo
import com.viewsonic.classswift.data.info.SketchTaskInfo
import com.viewsonic.classswift.data.quiz.SketchEventInfo
import com.viewsonic.classswift.data.quiz.SketchMarkUpdateState
import com.viewsonic.classswift.data.quiz.SketchResultState
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.SketchState
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class MvbSketchResponseStartWindowModel(
    private val taskApiService: TaskApiService,
    private val networkManager: NetworkManager,
    private val tokenProvider: () -> String,
    private val lessonIdProvider: () -> String,
    private val coordinator: SketchTaskCoordinator,
    private val markHandler: SketchMarkHandler,
    coroutineScopeOverride: CoroutineScope? = null,
) : IWindowModel {

    data class UiState(
        val students: List<SketchStudentCardInfo> = emptyList(),
        val submittedCount: Int = 0,
        val totalCount: Int = 0,
        val elapsedSeconds: Int = 0,
        val isRefreshing: Boolean = false,
        val closeConfirmShown: Boolean = false,
        val taskInfo: SketchTaskInfo = SketchTaskInfo(),
    )

    sealed class UiEvent {
        /** Manual refresh hit a network problem (no connectivity). Auto-polling stays silent. */
        object NetworkDisconnected : UiEvent()

        /** Manual refresh reached backend but got non-success (HTTP error / exception). */
        object RefreshFailed : UiEvent()
    }

    interface Listener {
        fun onRequestCloseWindow()
    }

    private val coroutineScope: CoroutineScope = coroutineScopeOverride ?: CoroutineManager.getScope(this)

    // ─── Answering phase ──────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = EVENT_BUFFER_CAPACITY)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    /** ANSWERING → RESULT 單向切換。由 [collectAllAndMark] 觸發；Window 訂閱後呼叫 applyPanelVisibility。 */
    private val _sketchState = MutableStateFlow(SketchState.ANSWERING)
    val sketchState: StateFlow<SketchState> = _sketchState.asStateFlow()

    var listener: Listener? = null

    private var pollerJob: Job? = null
    private var timerJob: Job? = null

    // ─── Result phase ─────────────────────────────────────────────────────────

    /** 當前 task 的學生作品結果（Window 訂閱以渲染 Options bars + Overview 大數字 + Student cards）。 */
    val sketchResultFlow: StateFlow<SketchResultState> = coordinator.sketchResultFlow

    /** 一次性事件（如 fetch 失敗）。 */
    val resultEventFlow: SharedFlow<SketchEventInfo> = coordinator.eventFlow

    /** SketchMarkHandler 批改 / hand-back 結果（Window 訂閱以同步 UI）。 */
    val markUpdateResultFlow: SharedFlow<SketchMarkUpdateState> = markHandler.markUpdateResultFlow

    fun start() {
        startPolling()
        startTimer()
    }

    fun stop() {
        stopPolling()
        stopTimer()
    }

    private fun startPolling() {
        if (pollerJob?.isActive == true) return
        pollerJob = coroutineScope.launch {
            while (isActive) {
                fetchBatchOnce(isManualRefresh = false)
                delay(AppConstants.SKETCH_RESPONSE_POLLING_INTERVAL)
            }
        }
    }

    private fun stopPolling() {
        pollerJob?.cancel()
        pollerJob = null
    }

    fun refreshNow() {
        if (_uiState.value.isRefreshing) return
        if (!networkManager.networkAvailabilityState.value) {
            emitEvent(UiEvent.NetworkDisconnected)
            return
        }
        _uiState.update { it.copy(isRefreshing = true) }
        coroutineScope.launch {
            try {
                fetchBatchOnce(isManualRefresh = true)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
                // Restart the periodic ticker so the next auto-tick is +3s from this manual refresh.
                stopPolling()
                startPolling()
            }
        }
    }

    fun requestClose() {
        _uiState.update { it.copy(closeConfirmShown = true) }
    }

    fun cancelCloseConfirm() {
        _uiState.update { it.copy(closeConfirmShown = false) }
    }

    fun confirmClose() {
        stop()
        _uiState.update { it.copy(closeConfirmShown = false) }
        listener?.onRequestCloseWindow()
    }

    /**
     * 老師點「Collect all and mark」→ 停止 polling / timer，切換到 RESULT 狀態。
     * Window 訂閱 [sketchState] 後自行執行 panel 切換 + 載入結果頁資料（不再開新 Window）。
     */
    fun collectAllAndMark() {
        stop()
        _sketchState.value = SketchState.RESULT
    }

    private suspend fun fetchBatchOnce(isManualRefresh: Boolean) {
        try {
            val response = taskApiService.getBatchTasksLatest(
                token = tokenProvider(),
                lessonId = lessonIdProvider(),
            )
            when (response) {
                is ApiResponse.Success -> applyResponse(response.data)
                else -> {
                    Timber.w("[MvbSketchResponse] polling non-success: $response")
                    if (isManualRefresh) emitEvent(UiEvent.RefreshFailed)
                }
            }
        } catch (t: Throwable) {
            Timber.w(t, "[MvbSketchResponse] polling exception")
            if (isManualRefresh) emitEvent(UiEvent.RefreshFailed)
        }
    }

    @VisibleForTesting
    internal fun applyResponse(response: BatchTasksLatestResponse) {
        val students = response.toSketchStudentCardInfos()
        val taskInfo = response.toSketchTaskInfo()
        val submitted = students.count { it.status == SketchAnswerStatus.SUBMITTED }
        _uiState.update {
            it.copy(
                students = students,
                submittedCount = submitted,
                totalCount = students.size,
                taskInfo = taskInfo,
            )
        }
    }

    private fun emitEvent(event: UiEvent) {
        coroutineScope.launch { _events.emit(event) }
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = coroutineScope.launch {
            while (isActive) {
                delay(ONE_SECOND_MS)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // ─── Result phase methods ─────────────────────────────────────────────────

    /** 載入結果頁顯示用的單一 task 資料。Caller：[MvbSketchResponseStartWindow.transitionToResult]。 */
    suspend fun loadResult(taskId: String) {
        coordinator.loadResult(taskId)
    }

    /** 重新撈當前 task（user 點 Refresh button）。 */
    suspend fun refreshResult() {
        coordinator.refresh()
    }

    /**
     * SketchReviewWidget 批改完送出時呼叫。
     *
     * 流程：[SketchMarkHandler.updateContentTaskResult] → API success → emit [markUpdateResultFlow]，
     * Window 收到後呼 [applyMarkSuccess] 同步回 [sketchResultFlow]。
     */
    suspend fun saveAndHandBack(record: TaskResultInfo.Content) {
        markHandler.updateContentTaskResult(taskId = record.taskId, recordResult = record)
    }

    /** SketchMarkHandler 成功後同步到 [sketchResultFlow]。 */
    suspend fun applyMarkSuccess(success: UpdateTaskResult) {
        coordinator.updateMarkedResult(success)
    }

    fun getCurrentFocusTaskId(): String? = coordinator.getCurrentFocusId()

    /** 設定 batch task id（進入點帶入；transitional hook 無此值時為 null）。 */
    fun setBatchTaskId(id: String?) {
        coordinator.setBatchTaskId(id)
    }

    /**
     * 結果頁 [X] 關窗時呼叫，關閉整批 batch task。
     * batchTaskId 為 null（尚未帶入）時 coordinator 會 no-op 並回 false，不阻擋關窗。
     */
    suspend fun closeBatch(): Boolean = coordinator.closeBatch()

    /** SketchReviewWidget 內 ApiFail retry button 觸發，重撈該學生資料。 */
    fun getStudentTaskResult(studentId: String) {
        coordinator.getStudentTaskResult(studentId)
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCleared() {
        stop()
        CoroutineManager.cancelScope(coordinator)
        coroutineScope.cancel()
    }

    companion object {
        private const val ONE_SECOND_MS = 1000L

        /** refresh 連點時防止 event 遺失所需的 SharedFlow buffer 容量。 */
        private const val EVENT_BUFFER_CAPACITY = 4
    }
}
