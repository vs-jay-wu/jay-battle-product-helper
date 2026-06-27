package com.viewsonic.classswift.ui.widgetmodel.task

import com.viewsonic.classswift.data.task.TaskInfo
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.coordinator.PushTaskCoordinator
import com.viewsonic.classswift.data.task.TaskApiResult
import com.viewsonic.classswift.data.task.UrlPreviewInfo
import com.viewsonic.classswift.ui.widgetmodel.task.state.PushRespondUiEvent
import com.viewsonic.classswift.ui.widgetmodel.task.state.PushRespondUiState
import com.viewsonic.classswift.uimanager.QuizUiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

class ContentTaskWidgetModel(
    private val pushTaskCoordinator: PushTaskCoordinator,
    private val quizUiManager: QuizUiManager
) {

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val _uiStateFlow = MutableStateFlow<PushRespondUiState>(PushRespondUiState.Idle)

    private val _errorFlow = MutableSharedFlow<PushRespondUiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val errorFlow = _errorFlow.asSharedFlow()
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        observeTaskList()
    }

    private fun observeTaskList() {
        coroutineScope.launch(Dispatchers.Default) {
            pushTaskCoordinator.taskListFlow.collect { data ->
                _uiStateFlow.emit(PushRespondUiState.TaskListUpdate(data = data))
            }
        }
    }

    fun createScreenShotContent() {
        pushTaskCoordinator.startScreenshot(
            onSuccess = {
                //Do nothing here.
            },
            onFailed = {
                coroutineScope.launch(Dispatchers.Default) {
                    _errorFlow.emit(PushRespondUiEvent.ScreenshotFail)
                }
            }
        )
    }

    fun deleteItem(data: TaskInfo) {
        pushTaskCoordinator.deleteItem(data = data)
    }

    fun deletedSelectedItem() {
        pushTaskCoordinator.deletedSelectedItem()
    }

    fun updateItemSelectStatus(data: TaskInfo) {
        pushTaskCoordinator.updateItemSelectStatus(data = data)
    }

    fun updateItemImageUploadSuccess(data: TaskInfo) {
        pushTaskCoordinator.updateItemImageUploadSuccess(data = data)
    }

    fun getCurrentItemCount(): Int {
        return pushTaskCoordinator.getCurrentItemCount()
    }

    fun unselectAllItemSelect() {
        pushTaskCoordinator.unselectAllItem()
    }

    fun unselectTasks(data: List<TaskInfo>) {
        pushTaskCoordinator.unselectTasks(data = data)
    }

    fun getSelectedCount(): Int {
        return pushTaskCoordinator.getSelectedItemCount()
    }

    fun addLinkTask(data: UrlPreviewInfo) {
        pushTaskCoordinator.createLinkTask(data = data)
    }

    fun pushTasksToStudent() {
        coroutineScope.launch {
            val selectedTasks = pushTaskCoordinator.getSelectedTasks().asReversed()
            Timber.d("push task : $selectedTasks")
            if (selectedTasks.isNotEmpty()) {
                val result = pushTaskCoordinator.pushTasks(selectedTasks)
                checkPushTaskResult(result = result)
            }
        }
    }

    private suspend fun checkPushTaskResult(result: List<TaskApiResult<TaskInfo>>) {

        if (result.isEmpty()) return

        val successfullyCount = result.count { it.isSuccess }

        val successTasks: List<TaskInfo> = result
            .filter { it.isSuccess }
            .map { it.data }

        val failedCount = result.size - successfullyCount

        if (isEndQuizError(result = result)) {
            _errorFlow.emit(
                PushRespondUiEvent.EndQuizFail
            )
            return
        }

        if (failedCount == 0) {
            _uiStateFlow.emit(
                PushRespondUiState.PushTaskCompleted(
                    stateId = UUID.randomUUID().toString(),
                    data = result
                )
            )
        } else {
            _errorFlow.emit(
                PushRespondUiEvent.PushTaskFail(
                    successfullyCount = successfullyCount,
                    failedCount = failedCount,
                    data = result,
                    successTasks = successTasks
                )
            )
        }
    }

    private fun isEndQuizError(result: List<TaskApiResult<TaskInfo>>): Boolean {
        result.forEach { taskResult ->
            taskResult.errorBody?.type?.let {
                return true
            }
        }
        return false
    }
}