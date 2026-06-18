package com.viewsonic.classswift.ui.widgetmodel.records

import com.viewsonic.classswift.api.response.UpdateTaskResult
import com.viewsonic.classswift.coordinator.RecordMarkHandler
import com.viewsonic.classswift.coordinator.RecordResultState
import com.viewsonic.classswift.coordinator.RecordsCoordinator
import com.viewsonic.classswift.data.task.TaskApiResult
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.handler.TimerHandler
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SoundEffectManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus
import com.viewsonic.classswift.ui.widgetmodel.records.state.MarkUpdateState
import com.viewsonic.classswift.ui.widgetmodel.records.state.RecordUiEvent
import com.viewsonic.classswift.ui.widgetmodel.records.state.RecordsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

class RecordsTaskWidgetModel(
    private val recordsCoordinator: RecordsCoordinator,
    private val recordMarkHandler: RecordMarkHandler,
    private val classroomManager: ClassroomManager,
    private val timerHandler: TimerHandler,
    private val studentManager: StudentManager,
    private val soundEffectManager: SoundEffectManager
) {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val lessonId = classroomManager.classroomDataStateFlow.value
        .selectedClassroomInfo.lessonId

    private val _uiStateFlow = MutableStateFlow<RecordsUiState>(RecordsUiState.Idle)
    private val _eventFlow = MutableSharedFlow<RecordUiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _timerFlow = MutableSharedFlow<Long>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var focusTimerTaskId: String = ""
    private var addPointResourceId: Int = 0
    private var subtractPointResourceId: Int = 0

    val eventFlow = _eventFlow.asSharedFlow()
    val uiStateFlow = _uiStateFlow.asStateFlow()
    val timerFlow = _timerFlow.asSharedFlow()

    init {
        observeData()
    }

    fun getRecordList() {
        coroutineScope.launch {
            recordsCoordinator.getRecordList()
        }
    }

    fun getTaskResultById(taskId: String) {
        Timber.d("getTaskResultById: $taskId")
        coroutineScope.launch {
            recordsCoordinator.getTaskRecordByTaskId(taskId = taskId)
        }
    }

    fun getStudentTaskResult(studentId: String) {
        Timber.d("getStudentTaskResult: $studentId")
        recordsCoordinator.getStudentTaskResult(studentId = studentId)
    }

    fun getCurrentResultById(taskId: String): RecordResultState? {
        val currentResult = recordsCoordinator.recordResultFlow.value

        currentResult.let {
            return if (it.taskInfo.taskId == taskId) currentResult else null
        }
    }

    fun sendMarkedResult(data: TaskResultInfo.Content) {
        coroutineScope.launch {
            data.taskId
                .takeIf { it.isNotEmpty() }
                ?.let { taskId ->
                    recordMarkHandler.updateContentTaskResult(
                        taskId = taskId,
                        recordResults = listOf(data)
                    )
                }
        }
    }

    fun markSelectedResults() {
        coroutineScope.launch {
            val taskId = recordsCoordinator.getCurrentFocusId() ?: return@launch

            val selectedResults = recordsCoordinator
                .getSelectedReadyToMarkResult(taskId)
                .takeIf { it.isNotEmpty() }
                ?.map { result ->
                    result.copy(
                        version = result.version + 1,
                        triggerType = SubmitStatus.GRADED
                    )
                } ?: return@launch

            recordMarkHandler.updateContentTaskResult(
                taskId = taskId,
                recordResults = selectedResults,
                isMultiMark = true
            )
        }
    }

    fun updateTaskResults(data: List<UpdateTaskResult>) {
        coroutineScope.launch {
            recordsCoordinator.updateMarkedResult(data = data)
        }
    }

    fun enableLabelAsMarkedMode(isEnable: Boolean, isNeedUpdateData: Boolean) {
        recordsCoordinator.updateLabelAsMarkedMode(
            isEnable = isEnable,
            isNeedUpdateData = isNeedUpdateData
        )
    }

    fun enablePushRecordMode(isEnable: Boolean, isNeedUpdateData: Boolean) {
        recordsCoordinator.updatePushRecordsMode(
            isEnable = isEnable,
            isNeedUpdateData = isNeedUpdateData
        )
    }

    fun isLabelAsMarkMode(): Boolean {
        return recordsCoordinator.isLabelAsMarkMode()
    }

    fun isPushRecordMode(): Boolean {
        return recordsCoordinator.isPushRecordMode()
    }

    fun updateItemSelectStatus(data: TaskResultInfo) {
        if (data is TaskResultInfo.Content) {
            recordsCoordinator.updateItemSelectStatus(data = data)
        }
    }

    fun getSelectedCount(): Int {
        return recordsCoordinator.getSelectedItemCount()
    }

    fun selectAllToMarked() {
        recordsCoordinator.updateAllSelectStatus(
            isSelected = true
        )
    }

    fun clearAllSelectLabelMarkedItem() {
        recordsCoordinator.updateAllSelectStatus(
            isSelected = false
        )
    }

    fun endTask(tasks: List<String>) {
        coroutineScope.launch {
            recordsCoordinator.endTask(tasks = tasks)
        }
    }

    fun endAllTask() {
        coroutineScope.launch {
            val inProgressTasks = recordsCoordinator.getInProgressTask()
            if (inProgressTasks.isNotEmpty()) {
                recordsCoordinator.endTask(tasks = inProgressTasks)
            }
        }
    }

    fun pushRecordToAll() {
        coroutineScope.launch {
            if (!recordsCoordinator.isPushRecordMode()) return@launch
            if (recordsCoordinator.isLabelAsMarkMode()) return@launch

            val selectedRecords = recordsCoordinator.getSelectedPushRecords()
            Timber.d("push record : $selectedRecords")
            if (selectedRecords.isNotEmpty()) {
                val result = recordsCoordinator.pushTasks(selectedRecords)
                checkPushTaskResult(result = result)
            }
        }
    }

    fun startTimer(taskId: String, taskCreateTime: Long) {
        if (focusTimerTaskId == taskId) return
        focusTimerTaskId = taskId
        timerHandler.startTimer(createAtSeconds = taskCreateTime)
    }

    fun stopTimerByTaskId(taskId: String) {
        if (taskId == focusTimerTaskId) {
            focusTimerTaskId = ""
            timerHandler.stopTimer()
        }
    }

    fun stopTimer() {
        timerHandler.stopTimer()
    }

    fun submittedAddPoint(taskId: String) {

        coroutineScope.launch {
            val submittedStudents = recordsCoordinator.getCurrentSubmittedStudentList(
                taskId = taskId
            )

            studentManager.increaseGroupPointByOnePoint(
                lessonId = lessonId,
                studentIdList = submittedStudents
            )
        }
    }

    fun addPointByStudentId(studentId: String) {
        if (studentId.isEmpty()) return
        if (lessonId.isEmpty()) return

        coroutineScope.launch {
            studentManager.increaseSpecificStudentPointByOnePoint(
                lessonId = lessonId,
                studentId = studentId
            )
        }
    }

    fun addGroupPoint(groupStudentIds: List<String>) {
        if (groupStudentIds.isEmpty()) return
        if (lessonId.isEmpty()) return
        coroutineScope.launch {
            Timber.d("Group Add Point: $groupStudentIds")
            studentManager.increaseGroupPointByOnePoint(
                lessonId = lessonId,
                studentIdList = groupStudentIds
            )
        }
    }

    fun subtractGroupPoint(groupStudentCount: Int, groupId: Int) {
        if (groupStudentCount < 1) return
        if (lessonId.isEmpty()) return
        coroutineScope.launch {

            val studentList = recordsCoordinator.getStudentsWithPositivePointsByGroupId(
                groupId = groupId
            )

            Timber.d("Group Add Point: $studentList")

            studentManager.decreaseGroupPointByOnePoint(
                lessonId = lessonId,
                studentIdList = studentList
            )
        }
    }

    fun preloadPointChangeSoundEffect(
        addPointResId: Int,
        subtractPointResId: Int
    ) {
        addPointResourceId = addPointResId
        subtractPointResourceId = subtractPointResId
        soundEffectManager.preload(addPointResId)
        soundEffectManager.preload(subtractPointResId)
    }

    fun playAddPointSoundEffect() {
        soundEffectManager.play(addPointResourceId)
    }

    fun playSubtractPointSoundEffect() {
        soundEffectManager.play(subtractPointResourceId)
    }

    private fun observeData() {
        with(recordsCoordinator) {
            coroutineScope.launch {
                recordListFlow.collect { data ->
                    _uiStateFlow.emit(
                        RecordsUiState.RecordListUpdate(
                            id = UUID.randomUUID().toString(),
                            data = data
                        )
                    )
                }
            }

            coroutineScope.launch {
                recordResultFlow.collect { data ->
                    _uiStateFlow.emit(
                        RecordsUiState.RecordResultUpdate(
                            //first: TaskStatusInfo,
                            //second: Record results
                            id = UUID.randomUUID().toString(),
                            taskStatusInfo = data.taskInfo,
                            data = data.recordList
                        )
                    )
                }
            }

            coroutineScope.launch {
                eventFlow.collect { data ->
                    _eventFlow.emit(
                        RecordUiEvent.RecordEventUpdate(data = data)
                    )
                }
            }
        }

        coroutineScope.launch {
            recordMarkHandler.markUpdateResultFlow.collect { state ->
                handleMarkedUpdateResult(state = state)
            }
        }

        coroutineScope.launch {
            timerHandler.timerEventFlow.collect { data ->
                _timerFlow.emit(data)
            }
        }
    }

    fun getRecordsCoordinator(): RecordsCoordinator {
        return recordsCoordinator
    }

    private suspend fun handleMarkedUpdateResult(state: MarkUpdateState) {
        when (state) {
            is MarkUpdateState.SingleMarkSuccess -> {
                _uiStateFlow.emit(
                    RecordsUiState.SingleMarkUpdate(
                        id = UUID.randomUUID().toString(),
                        success = state.success,
                        failed = state.failed
                    )
                )
            }

            is MarkUpdateState.MultiMarkSuccess -> {
                _uiStateFlow.emit(
                    RecordsUiState.MultiMarkUpdate(
                        id = UUID.randomUUID().toString(),
                        success = state.success,
                        failed = state.failed
                    )
                )
            }

            is MarkUpdateState.Failed -> {
                _eventFlow.emit(
                    RecordUiEvent.MarkUpdateFailed(
                        id = UUID.randomUUID().toString(),
                        errorMessage = state.errorMessage,
                        isMultiMark = state.isMultiMark
                    )
                )
            }

            else -> Unit
        }
    }

    private suspend fun checkPushTaskResult(result: List<TaskApiResult<TaskResultInfo>>) {

        if (result.isEmpty()) return

        val successfullyCount = result.count { it.isSuccess }
        val failedCount = result.size - successfullyCount

        if (failedCount == 0) {
            _uiStateFlow.emit(
                RecordsUiState.PushRecordCompleted(
                    id = UUID.randomUUID().toString(),
                    data = result
                )
            )
        } else {
            _eventFlow.emit(
                RecordUiEvent.PushRecordFail(
                    successfullyCount = successfullyCount,
                    failedCount = failedCount,
                    data = result
                )
            )
        }
    }
}