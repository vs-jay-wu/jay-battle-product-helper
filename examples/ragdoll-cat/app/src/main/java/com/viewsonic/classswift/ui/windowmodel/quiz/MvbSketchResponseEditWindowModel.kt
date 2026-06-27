package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.BatchCreateTasksBody
import com.viewsonic.classswift.api.body.CreateTaskBody
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.coordinator.UploadFileHandler
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.ui.widget.task.enums.TaskAssignType
import com.viewsonic.classswift.ui.widget.task.enums.TaskType
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class MvbSketchResponseEditWindowModel(
    private val taskApiService: TaskApiService,
    private val accountManager: AccountManager,
    private val classroomManager: ClassroomManager,
    private val uploadFileHandler: UploadFileHandler,
    private val networkManager: NetworkManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    coroutineScopeOverride: CoroutineScope? = null,
) : IWindowModel {

    private val scope: CoroutineScope = coroutineScopeOverride ?: CoroutineManager.getScope(this)

    sealed class UploadState {
        object Idle : UploadState()
        data class Loading(val uri: String) : UploadState()
        data class Success(val previewImageUrl: String) : UploadState()
        data class Failed(val cause: Throwable? = null) : UploadState()
    }

    data class UiState(
        val uploadState: UploadState = UploadState.Idle,
        val isDispatchInFlight: Boolean = false,
    )

    sealed class Event {
        object OpenStartWindow : Event()
        data class ShowErrorToast(val messageResId: Int) : Event()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private var uploadObserveJob: Job? = null
    private var dispatchJob: Job? = null
    private var currentUri: String = ""

    init {
        observeUploadResult()
    }

    fun startUpload(imageUri: String) {
        currentUri = imageUri
        _uiState.update { it.copy(uploadState = UploadState.Loading(imageUri)) }
        uploadFileHandler.fetchPreSignedUrl(
            classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
            imageUri,
        )
    }

    fun retryUpload() {
        if (currentUri.isBlank()) {
            Timber.w("[MvbSketchResponseEditWindowModel] retryUpload skipped — no cached URI")
            return
        }
        startUpload(currentUri)
    }

    fun startQuestion() {
        if (_uiState.value.isDispatchInFlight) return
        if (_uiState.value.uploadState !is UploadState.Success) return
        if (!networkManager.networkAvailabilityState.value) {
            emitEvent(Event.ShowErrorToast(R.string.mvb_network_disconnect_toast))
            return
        }
        _uiState.update { it.copy(isDispatchInFlight = true) }
        dispatchJob?.cancel()
        dispatchJob = scope.launch(ioDispatcher) {
            try {
                val response = taskApiService.batchCreateTasks(
                    token = accountManager.getBearerToken(),
                    lessonId = classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
                    body = BatchCreateTasksBody(
                        tasks = listOf(
                            CreateTaskBody(
                                assign = TaskAssignType.ALL.code,
                                imageUrl = uploadFileHandler.awsPreSignedUrl.s3GetUrl,
                                linkUrl = "",
                                taskType = TaskType.CONTENT.code,
                                seatNumberList = emptyList(),
                            ),
                        ),
                    ),
                )
                when (response) {
                    is ApiResponse.Success -> {
                        QuizSharedUiInfo.updateQuizType(QuizType.SKETCH_RESPONSE)
                        emitEvent(Event.OpenStartWindow)
                    }
                    else -> {
                        Timber.e("[startQuestion] batchCreateTasks failed: $response")
                        _uiState.update { it.copy(isDispatchInFlight = false) }
                        emitEvent(Event.ShowErrorToast(R.string.quiz_error_msg_start_quiz))
                    }
                }
            } catch (t: Throwable) {
                Timber.e(t, "[startQuestion] batchCreateTasks exception")
                _uiState.update { it.copy(isDispatchInFlight = false) }
                emitEvent(Event.ShowErrorToast(R.string.quiz_error_msg_start_quiz))
            }
        }
    }

    /**
     * Cancels in-flight work. Called by the Window right before it dismisses. Does
     * NOT clear `QuizSharedUiInfo.screenshotImageUri` because the downstream
     * `MvbSketchResponseStartWindow` reads it for its preview area; mission close
     * (unclosedMissionUiManager) is the canonical cleanup hook for the shared URI.
     */
    fun cleanupBeforeClose() {
        dispatchJob?.cancel()
        currentUri = ""
    }

    private fun observeUploadResult() {
        uploadObserveJob?.cancel()
        uploadObserveJob = scope.launch(ioDispatcher) {
            uploadFileHandler.uploadImageSharedFlow.collect { isSuccess ->
                if (isSuccess) {
                    _uiState.update {
                        it.copy(uploadState = UploadState.Success(uploadFileHandler.awsPreSignedUrl.s3GetUrl))
                    }
                } else {
                    _uiState.update { it.copy(uploadState = UploadState.Failed()) }
                }
            }
        }
    }

    private fun emitEvent(event: Event) {
        scope.launch { _events.emit(event) }
    }

    override fun onCleared() {
        Timber.d("[MvbSketchResponseEditWindowModel] onCleared")
        uploadObserveJob?.cancel()
        dispatchJob?.cancel()
        runCatching { scope.cancel() }
    }

}
