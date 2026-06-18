package com.viewsonic.classswift.coordinator

import android.content.Context
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.CreateTaskBody
import com.viewsonic.classswift.api.response.UpdateTaskResult
import com.viewsonic.classswift.api.response.toTaskResultInfo
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.constant.AppConstants
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.records.RecordListInfo
import com.viewsonic.classswift.data.records.TaskListUpdateInfo
import com.viewsonic.classswift.data.socket.task.TaskResponseSocketMessage
import com.viewsonic.classswift.data.task.HasTaskInProgressInfo
import com.viewsonic.classswift.data.task.LinkMetaInfo
import com.viewsonic.classswift.data.task.RecordEventInfo
import com.viewsonic.classswift.data.task.RemoteTaskInfo
import com.viewsonic.classswift.data.task.TaskApiResult
import com.viewsonic.classswift.data.task.TaskRecordInfo
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.data.task.TaskStatusInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.manager.SocketManager.ConnectionState
import com.viewsonic.classswift.manager.SocketManager.ReceivedEvent
import com.viewsonic.classswift.manager.SocketManager.ReceivedEventData
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.manager.StudentManager.StudentChangeReason
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.ui.widget.task.enums.RecordType
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus
import com.viewsonic.classswift.ui.widget.task.enums.TaskAssignType
import com.viewsonic.classswift.ui.widget.task.enums.TaskStatus
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import kotlin.String

class RecordsCoordinator(
    private val applicationContext: Context,
    private val classroomManager: ClassroomManager,
    private val studentManager: StudentManager,
    private var taskApiService: TaskApiService,
    private val socketManager: SocketManager,
    private val accountManager: AccountManager
) {
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var currentSelectedTaskId: String? = null
    private val lessonId = classroomManager.classroomDataStateFlow.value
        .selectedClassroomInfo.lessonId

    private val seatMaxCount = classroomManager.classroomDataStateFlow.value
        .selectedClassroomInfo.maxStudentCount

    private val _recordListFlow = MutableStateFlow(TaskListUpdateInfo())
    val recordListFlow = _recordListFlow.asStateFlow()

    private val _recordResultFlow = MutableStateFlow(RecordResultState(TaskStatusInfo(), emptyList()))

    val recordResultFlow = _recordResultFlow.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RecordEventInfo>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var pollerJob: Job? = null
    private var currentFocusTaskId: String? = null
    private var isLabelAsMarkMode: Boolean = false
    private var isPushRecordMode: Boolean = false

    init {
        Timber.d("Lesson ID = $lessonId")
        observeData()
    }

    private fun observeData() {
        coroutineScope.launch {
            socketManager.receivedEventDataFlow.collect { data ->
                handleSocketEvent(data = data)
            }
        }

        coroutineScope.launch {
            socketManager.connectionStateSharedFlow.collect { data ->
                Timber.d("socket connect state update : $data")
                handleSocketConnectionStatusChange(status = data)
            }
        }

        coroutineScope.launch {
            studentManager.studentChangeReasonFlow.collect { studentChangeReason ->
                if (studentChangeReason is StudentChangeReason.SocketReconnected) {
                    handleStudentJoinStatusWhenSocketDisconnect()
                    return@collect
                }
                when (studentChangeReason) {
                    is StudentChangeReason.RejoinSeat -> {
                        Timber.tag("StudentChanged").d("StudentChangeReason.RejoinSeat")
                        handleSetStudentName(studentChangeReason.newStudent)
                    }

                    is StudentChangeReason.SetStudentName-> {
                        Timber.tag("StudentChanged").d("StudentChangeReason.SetStudentName")
                        handleSetStudentName(studentChangeReason.newStudent)
                    }
                    is StudentChangeReason.ReleaseSeat -> {
                        Timber.tag("StudentChanged").d("StudentChangeReason.ReleaseSeat")
                        handleReleaseSeat(studentChangeReason.newStudent)
                    }
                    else -> {}
                }
            }
        }

    }

    private fun handleSocketEvent(data: ReceivedEventData) {
        Timber.d("Socket Event Receive: ${data.event}")
        Timber.d("Socket Event Body: ${data.messageJsonObject.toString()}")

        when (data.event) {
            ReceivedEvent.EVENT_TASK_RESPONSE -> {
                data.messageJsonObject?.let { jsonObject ->
                    handleTaskResponseSocketEvent(data = jsonObject)
                }
            }
            else -> Unit
        }
    }

    fun isLabelAsMarkMode(): Boolean {
        return isLabelAsMarkMode
    }

    fun isPushRecordMode(): Boolean {
        return isPushRecordMode
    }

    fun getCurrentFocusId(): String? {
        return currentFocusTaskId
    }

    fun getInProgressTask(): List<String> {
        return _recordListFlow.value.data
            .filterIsInstance<RecordListInfo.TaskItem>()
            .filter { it.taskStatus == TaskStatus.IN_PROGRESS }
            .map { it.id }
    }

    fun getStudentsWithPositivePointsByGroupId(groupId: Int): List<String> {
        val currentStudentList = studentManager.getCurrentList()
        return currentStudentList.filter {
            it.points > 0 && it.groupId == groupId
        }.map { it.studentId }
    }

    suspend fun endTask(tasks: List<String>) {
        Timber.d("Close Task Start = $tasks")

        var successCount = 0
        var failedCount = 0

        var updateList = _recordListFlow.value.data

        tasks.forEach { taskId ->
            val response = taskApiService.closeTask(
                token = accountManager.getBearerToken(),
                taskId = taskId
            )
            Timber.d("Close task response: $response")

            when (response) {
                is ApiResponse.Success -> {
                    val closedTaskId = response.data.result.id
                    val newStatus = TaskStatus.fromString(response.data.result.status)
                    val newTaskType = RecordType.fromString(response.data.result.taskType)
                    val newEndTime = response.data.result.endTime

                    updateList = updateList.map { item ->
                        if (item is RecordListInfo.TaskItem && item.id == closedTaskId) {
                            item.copy(
                                taskType = newTaskType,
                                taskStatus = newStatus,
                                endTime = newEndTime
                            )
                        } else item
                    }
                    successCount++
                }

                is ApiResponse.Rfc7807Failure -> {
                    failedCount++
                    Timber.d("Close task failed: ${response.error}")
                }

                is ApiResponse.NetworkDisconnected -> {
                    failedCount++
                    Timber.d("Network disconnected for task $taskId")
                }

                else -> {
                    failedCount++
                }
            }
        }

        val inProgressList = updateList.filterIsInstance<RecordListInfo.TaskItem>()
            .filter { it.taskStatus == TaskStatus.IN_PROGRESS }
            .sortedByDescending { it.sequenceNumber }

        val endedList = updateList.filterIsInstance<RecordListInfo.TaskItem>()
            .filter { it.taskStatus == TaskStatus.CLOSED }
            .sortedByDescending { it.endTime }

        val finalList = buildList {
            if (inProgressList.isNotEmpty()) {
                add(RecordListInfo.Header(RecordListInfo.HeaderType.RECEIVING))
                addAll(inProgressList)
            }
            if (endedList.isNotEmpty()) {
                add(RecordListInfo.Header(RecordListInfo.HeaderType.TASKENDED))
                addAll(endedList)
            }
        }

        if (inProgressList.isEmpty()) {
            unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.PUSH_AND_RESPOND_TASK)
        }

        val taskListUpdateInfo = TaskListUpdateInfo(
            id = UUID.randomUUID().toString(),
            isEndTask = true,
            data = setItemSelected(finalList)
        )

        _recordListFlow.emit(taskListUpdateInfo)

        val event = if (successCount > 0 && failedCount == 0) {
            RecordEventInfo.EndTaskSuccess(successCount)
        } else {
            RecordEventInfo.EndTaskFailed(successCount, failedCount)
        }
        _eventFlow.emit(event)
    }

    fun getSelectedPushRecords(): List<TaskResultInfo> {
        return _recordResultFlow.value.recordList.filter {
            it is TaskResultInfo.SelectableResult && it.isSelected && it.isPushable
        }
    }

    fun getCurrentSubmittedStudentList(taskId: String): List<String> {
        val currentTaskId = _recordResultFlow.value.taskInfo.taskId
        if (currentTaskId != taskId) {
            return emptyList()
        }

        val targetStudentIds: List<String> = _recordResultFlow.value.recordList
            .filter { taskResult ->
                when (taskResult) {

                    is TaskResultInfo.Content -> {
                        taskResult.triggerType == SubmitStatus.RESPONSE ||
                                taskResult.triggerType == SubmitStatus.GRADED
                    }

                    is TaskResultInfo.Link -> taskResult.linkIsOpened
                    else -> false
                }
            }
            .map { it.studentId }
        return targetStudentIds
    }

    suspend fun pushTasks(records: List<TaskResultInfo>): List<TaskApiResult<TaskResultInfo>> {
        val resultList = mutableListOf<TaskApiResult<TaskResultInfo>>()

        for (taskRecordInfo in records) {
            val requestBody = createPushRecordRequestBody(data = taskRecordInfo)
            Timber.d("Create task request body: ${requestBody?.toString()}")

            if (requestBody == null) {
                resultList.add(
                    TaskApiResult(
                        data = taskRecordInfo,
                        isSuccess = false,
                        errMsg = applicationContext.getString(
                            R.string.connection_error_invalid_request
                        )
                    )
                )
                continue
            }

            val response = taskApiService.createTask(
                token = accountManager.getBearerToken(),
                lessonId = lessonId,
                body = requestBody
            )

            Timber.d("Create task response: $response")

            val result = when (response) {
                is ApiResponse.Success -> {
                    TaskApiResult(data = taskRecordInfo, isSuccess = true)
                }

                is ApiResponse.Rfc7807Failure -> {
                    TaskApiResult(
                        data = taskRecordInfo,
                        isSuccess = false,
                        errMsg = response.error.detail,
                        errorBody = response.error
                    )
                }

                is ApiResponse.NetworkDisconnected -> {
                    TaskApiResult(
                        data = taskRecordInfo,
                        isSuccess = false,
                        errMsg = applicationContext.getString(
                            R.string.connection_network_error_disconnect
                        )
                    )
                }

                else -> {
                    TaskApiResult(
                        data = taskRecordInfo,
                        isSuccess = false,
                    )
                }
            }

            resultList.add(result)
        }

        return resultList
    }


    fun getSelectedReadyToMarkResult(taskId: String): List<TaskResultInfo.Content> {
        val (currentTaskStatus, resultList) = _recordResultFlow.value

        if (currentTaskStatus.taskId != taskId) return emptyList()
        return resultList.filterIsInstance<TaskResultInfo.Content>()
            .filter { it.isSelected }
    }

    fun updateLabelAsMarkedMode(isEnable: Boolean, isNeedUpdateData: Boolean) {
        coroutineScope.launch {
            isLabelAsMarkMode = isEnable

            if (isNeedUpdateData) {
                val currentList = _recordResultFlow.value.recordList

                val updatedList = currentList.map { item ->
                    if (item is TaskResultInfo.SelectableResult) {
                        item.copyWithEditableAndSelected(isEditable = isEnable, isSelected = false)
                    } else {
                        item
                    }
                }
                _recordResultFlow.update { it.copy(recordList = updatedList.toList()) }
            }
        }
    }

    fun updatePushRecordsMode(isEnable: Boolean, isNeedUpdateData: Boolean) {
        coroutineScope.launch {
            isPushRecordMode = isEnable

            if (isNeedUpdateData) {
                val currentList = _recordResultFlow.value.recordList

                val updatedList = currentList.map { item ->
                    if (item is TaskResultInfo.SelectableResult) {
                        item.copyWithPushable(
                            isEditable = isEnable,
                            isSelected = false,
                            isPushable = true
                        )
                    } else {
                        item
                    }
                }
                _recordResultFlow.update { it.copy(recordList = updatedList.toList()) }
            }
        }
    }

    fun updateItemSelectStatus(data: TaskResultInfo.Content) {
        Timber.d("updateItemSelectStatus : $data")

        coroutineScope.launch(Dispatchers.IO) {
            val updatedList = _recordResultFlow.value.recordList.map { taskResultInfo ->

                when {
                    taskResultInfo is TaskResultInfo.Content &&
                            taskResultInfo.studentId == data.studentId -> {
                        if (data.isSelected) {
                            taskResultInfo.copy(
                                isSelected = true,
                            )
                        } else {
                            taskResultInfo.copy(
                                isSelected = false
                            )
                        }
                    }

                    else -> taskResultInfo
                }
            }
            _recordResultFlow.update { it.copy( recordList = updatedList.toList()) }
        }
    }

    fun getSelectedItemCount(): Int {
        val selectedCount = _recordResultFlow.value.recordList.count {
            it is TaskResultInfo.SelectableResult && it.isSelected
        }
        return selectedCount
    }

    fun updateAllSelectStatus(isSelected: Boolean) {
        coroutineScope.launch {
            val updatedList: List<TaskResultInfo> = _recordResultFlow.value.recordList.map { item ->
                if (item is TaskResultInfo.Content && item.triggerType == SubmitStatus.RESPONSE) {
                    item.copy(isSelected = isSelected)
                } else {
                    item
                }
            }.toList()
            _recordResultFlow.update { it.copy(recordList = updatedList) }
        }
    }

    private suspend fun getTasks(filter: String): TaskApiResult<List<RemoteTaskInfo>?> {
        val response = taskApiService.getTask(
            token = accountManager.getBearerToken(),
            lessonId = lessonId,
            filter = filter
        )

        when (response) {
            is ApiResponse.Success -> {
                Timber.d("Get task success: ${response.data.data}")
                val remoteTaskList = mutableListOf<RemoteTaskInfo>()
                response.data.data.forEach { item ->
                    val data = RemoteTaskInfo(
                        taskId = item.taskId,
                        assign = item.assign,
                        submittedCount = item.submittedCount,
                        totalCount = item.totalCount,
                        submittedRate = item.submittedRate,
                        seq = item.sequence,
                        imgUrl = item.imgUrl,
                        linkUrl = item.linkUrl,
                        linkOpenedCount = item.linkOpenedCount,
                        linkTotalCount = item.linkTotalCount,
                        linkOpenedRate = item.linkOpenedRate,
                        linkMeta = item.linkMeta,
                        endTime = item.endTime
                    )
                    remoteTaskList.add(data)
                }
                return TaskApiResult(
                    data = remoteTaskList,
                    isSuccess = true
                )

            }

            is ApiResponse.Rfc7807Failure -> {
                return TaskApiResult(
                    data = null,
                    isSuccess = false,
                    errMsg = response.error.detail,
                    errorBody = response.error
                )
            }

            is ApiResponse.NetworkDisconnected -> {
                return TaskApiResult(
                    data = null,
                    isSuccess = false,
                    errMsg = applicationContext.getString(
                        R.string.connection_network_error_disconnect
                    )
                )
            }

            else -> {
                return TaskApiResult(
                    data = null,
                    isSuccess = false,
                )
            }
        }
    }

    suspend fun updateMarkedResult(data: List<UpdateTaskResult>) {
        val (currentTaskStatus, currentResults) = _recordResultFlow.value

        if (data.any { it.taskId == currentTaskStatus.taskId }) {
            val updatedResults = currentResults.map { oldResult ->
                when (oldResult) {
                    is TaskResultInfo.Content -> {
                        val newItem = data.find {
                            it.studentId == oldResult.studentId &&
                                    it.taskId == currentTaskStatus.taskId
                        }

                        if (newItem != null) {
                            val isNeedUpdateGroupId = newItem.groupId.isNotEmpty()
                            oldResult.copy(
                                imgUrl = newItem.imgUrl,
                                triggerType = SubmitStatus.fromCode(newItem.triggerType),
                                version = newItem.version,
                                groupId = if (isNeedUpdateGroupId) newItem.groupId else oldResult.groupId,
                                isSelected = false
                            )
                        } else {
                            oldResult
                        }
                    }

                    is TaskResultInfo.Link -> {
                        oldResult
                    }

                    else -> {
                        oldResult
                    }
                }
            }
            _recordResultFlow.emit(RecordResultState(currentTaskStatus, updatedResults))
        }
    }

    private suspend fun getTaskById(taskId: String): TaskApiResult<TaskRecordInfo?> {
        val response = taskApiService.getTaskById(
            token = accountManager.getBearerToken(),
            lessonId = lessonId,
            taskId = taskId
        )

        Timber.d(
            "get task by id response: $response"
        )

        when (response) {
            is ApiResponse.Success -> {
                Timber.d(
                    "getTaskById: ${response.data.data}"
                )
                Timber.d(
                    "getTaskById: ${response.data.data.taskResults}"
                )

                response.data.data.let {
                    val taskResults = mutableListOf<TaskResultInfo>()
                    val type = if (it.linkUrl.isNotEmpty()) {
                        RecordType.LINK
                    } else {
                        RecordType.CONTENT
                    }
                    val taskOriginalImgUrl = it.imgUrl

                    if (type == RecordType.CONTENT) {
                        it.taskResults.forEach { taskResult ->
                            Timber.d("TaskResult => $taskResult")
                            val taskResultInfo = TaskResultInfo.Content.create(
                                taskId = taskId,
                                displayName = taskResult.displayName,
                                studentId = taskResult.studentId,
                                triggerType = taskResult.triggerType,
                                seatNumber = taskResult.seatNumber,
                                imgUrl = taskResult.imgUrl.ifEmpty { taskOriginalImgUrl },
                                serialNumber = taskResult.serialNumber,
                                groupId = taskResult.groupId,
                                status = taskResult.status,
                                version = taskResult.version
                            )
                            taskResults.add(taskResultInfo)
                        }
                    } else {
                        it.taskResults.forEach { taskResult ->
                            Timber.d("TaskResult => $taskResult")
                            val taskResultInfo = TaskResultInfo.Link.create(
                                taskId = taskId,
                                displayName = taskResult.displayName,
                                linkMeta = LinkMetaInfo(
                                    title = it.linkMeta.title,
                                    description = it.linkMeta.description,
                                    siteName = it.linkMeta.siteName,
                                    image = it.linkMeta.image
                                ),
                                linkIsOpened = taskResult.linkIsOpened,
                                linkUrl = it.linkUrl,
                                studentId = taskResult.studentId,
                                seatNumber = taskResult.seatNumber,
                                serialNumber = taskResult.serialNumber,
                                groupId = taskResult.groupId,
                                status = taskResult.status,
                                version = taskResult.version
                            )
                            taskResults.add(taskResultInfo)
                        }
                    }

                    val data = TaskRecordInfo(
                        taskId = it.taskId,
                        assign = it.assign,
                        status = TaskStatus.fromString(it.status),
                        seq = it.sequence,
                        createdAt = it.createdAt,
                        linkUrl = it.linkUrl,
                        linkMeta = it.linkMeta,
                        imgUrl = it.imgUrl,
                        taskResults = taskResults
                    )

                    return TaskApiResult(
                        data = data,
                        isSuccess = true
                    )
                }
            }

            is ApiResponse.Rfc7807Failure -> {
                return TaskApiResult(
                    data = null,
                    isSuccess = false,
                    errMsg = response.error.detail,
                    errorBody = response.error
                )
            }

            is ApiResponse.NetworkDisconnected -> {
                return TaskApiResult(
                    data = null,
                    isSuccess = false,
                    errMsg = applicationContext.getString(
                        R.string.connection_network_error_disconnect
                    )
                )
            }

            is ApiResponse.ExceptionFailure -> {
                Timber.e("getTaskById exception failure: ${response.exception.toString()}")

                return TaskApiResult(
                    data = null,
                    isSuccess = false
                )
            }

            else -> {
                return TaskApiResult(
                    data = null,
                    isSuccess = false,
                )
            }
        }
    }

    suspend fun hasTaskInProgress(): HasTaskInProgressInfo = withContext(Dispatchers.IO) {

        try {
            // According to the backend engineers, currently only "summary" is
            // accepted as a valid value for the "filter" parameter.
            val result = getTasks(filter = "summary")

            if (!result.isSuccess) {
                return@withContext HasTaskInProgressInfo(
                    hasTaskInProgress = false,
                    isUnexpectedState = true
                )
            }

            //Sometimes the API returns a response where a task is still in progress but the data is empty,
            // so we need to add this extra check.
            val taskList = result.data ?: return@withContext HasTaskInProgressInfo(
                hasTaskInProgress = false,
                isUnexpectedState = false
            )

            val results = mutableListOf<Boolean>()

            for (task in taskList) {
                try {
                    val getTaskByIdResult = getTaskById(task.taskId)

                    val status = getTaskByIdResult.data?.status

                    if (!getTaskByIdResult.isSuccess) {
                        return@withContext HasTaskInProgressInfo(
                            hasTaskInProgress = false,
                            isUnexpectedState = true
                        )
                    }

                    val isInProgress = status == TaskStatus.IN_PROGRESS

                    results.add(isInProgress)

                    if (isInProgress) {
                        return@withContext HasTaskInProgressInfo(
                            hasTaskInProgress = true,
                            isUnexpectedState = false
                        )
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Error checking task ${task.taskId}")
                    return@withContext HasTaskInProgressInfo(
                        hasTaskInProgress = false,
                        isUnexpectedState = true
                    )
                }
            }

            val hasTaskInProgress = results.any { it }

            return@withContext HasTaskInProgressInfo(
                hasTaskInProgress = hasTaskInProgress,
                isUnexpectedState = false
            )
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error in hasTaskInProgress")
            return@withContext HasTaskInProgressInfo(
                hasTaskInProgress = false,
                isUnexpectedState = true
            )
        }
    }

    suspend fun getRecordList() = withContext(Dispatchers.IO) {

        try {
            // According to the backend engineers, currently only "summary" is
            // accepted as a valid value for the "filter" parameter.
            val result = getTasks(filter = "summary")

            if (!result.isSuccess) {
                _eventFlow.emit(RecordEventInfo.GetTasksFailed)
                return@withContext
            }

            val taskList = result.data ?: emptyList()

            val recordList = mutableListOf<RecordListInfo>()
            val inProgressList = mutableListOf<RecordListInfo.TaskItem>()
            val endedList = mutableListOf<RecordListInfo.TaskItem>()

            val semaphore = Semaphore(permits = 5)

            val deferredResults = taskList.map { task ->

                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        try {
                            val getTaskByIdResult = getTaskById(task.taskId)
                            Timber.d("getRecordList #getTaskById: $getTaskByIdResult")

                            if (getTaskByIdResult.isSuccess) {
                                getTaskByIdResult.data?.let {

                                    val type = if (it.linkUrl.isNotEmpty()) {
                                        RecordType.LINK
                                    } else {
                                        RecordType.CONTENT
                                    }

                                    val item = RecordListInfo.TaskItem(
                                        id = it.taskId,
                                        isSelected = false,
                                        sequenceNumber = it.seq,
                                        taskType = type,
                                        taskStatus = it.status,
                                        createAt = it.createdAt,
                                        endTime = task.endTime
                                    )

                                    when (it.status) {
                                        TaskStatus.IN_PROGRESS -> inProgressList.add(item)
                                        TaskStatus.CLOSED -> endedList.add(item)
                                        else -> Unit
                                    }
                                }
                            } else {
                                _eventFlow.emit(RecordEventInfo.GetTasksFailed)
                                return@async
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error checking task ${task.taskId}")
                        }
                    }
                }
            }

            deferredResults.awaitAll()

            recordList.apply {
                if (inProgressList.isNotEmpty()) {
                    add(RecordListInfo.Header(type = RecordListInfo.HeaderType.RECEIVING))
                    inProgressList.sortByDescending { it.sequenceNumber }
                    addAll(inProgressList)
                }
                if (endedList.isNotEmpty()) {
                    add(RecordListInfo.Header(type = RecordListInfo.HeaderType.TASKENDED))
                    endedList.sortByDescending { it.endTime }
                    addAll(endedList)
                }
            }

            val taskListUpdateInfo = TaskListUpdateInfo(
                id = UUID.randomUUID().toString(),
                isEndTask = false,
                data = setItemSelected(data = recordList.toList())
            )
            _recordListFlow.emit(taskListUpdateInfo)

        } catch (e: Exception) {
            Timber.e(e, "Unexpected error in getRecordList")
            val taskListUpdateInfo = TaskListUpdateInfo(
                id = UUID.randomUUID().toString(),
                isEndTask = false,
                data = setItemSelected(data = emptyList())
            )
            _recordListFlow.emit(taskListUpdateInfo)
        }
    }

    suspend fun getTaskRecordByTaskId(taskId: String) = withContext(Dispatchers.IO) {
        Timber.d("TaskRecordByTaskId : $taskId")

        val taskItem = _recordListFlow.value.data
            .filterIsInstance<RecordListInfo.TaskItem>()
            .find { it.id == taskId }

        taskItem?.let {
            currentFocusTaskId = it.id

            try {
                val result = getTaskById(taskId = it.id)

                if (result.isSuccess) {

                    result.data?.let { data ->
                        val taskStatusInfo = TaskStatusInfo(
                            infoId = UUID.randomUUID().toString(),
                            taskId = data.taskId,
                            assign = data.assign,
                            status = data.status,
                            sequenceNumber = data.seq,
                            createAt = data.createdAt,
                            recordType = if (data.linkUrl.isNotEmpty()) {
                                RecordType.LINK
                            } else {
                                RecordType.CONTENT
                            }
                        )

                        data.taskResults.let { resultList ->
                            val syncResult = createTaskResult(originalData = resultList)

                            val finalList: List<TaskResultInfo> = if (isLabelAsMarkMode) {
                                handleLabelAsMarkedStatus(originalData = syncResult)
                            } else {
                                syncResult
                            }

                            val data = RecordResultState(taskStatusInfo, finalList.map { result -> result })
                            _recordResultFlow.emit(data.copy())
                        }
                    }
                } else {
                    _eventFlow.emit(RecordEventInfo.GetRecordByTaskIdFailed)
                }
            } catch (_: Exception) {
                _eventFlow.emit(RecordEventInfo.GetRecordByTaskIdFailed)
            }
        }
    }

    private fun handleLabelAsMarkedStatus(
        originalData: List<TaskResultInfo>
    ): List<TaskResultInfo> {

        val currentSelectableByStudentId: Map<String, TaskResultInfo.SelectableResult> =
            _recordResultFlow.value.recordList
                .mapNotNull {
                    when (it) {
                        is TaskResultInfo.Content -> it
                        is TaskResultInfo.Guest -> it
                        else -> null
                    }
                }
                .associateBy { it.studentId }


        return originalData.map { newItem ->
            when (newItem) {
                is TaskResultInfo.Content,
                is TaskResultInfo.Guest -> {
                    val old = currentSelectableByStudentId[newItem.studentId]
                    if (old != null) {
                        newItem.copyWithEditableAndSelected(
                            isEditable = old.isEditable,
                            isSelected = old.isSelected
                        )
                    } else {
                        newItem.copyWithEditableAndSelected(
                            isEditable = true,
                            isSelected = newItem.isSelected
                        )
                    }
                }
                is TaskResultInfo.ApiFail,
                is TaskResultInfo.Link -> newItem
            }
        }
    }

    private fun setItemSelected(data: List<RecordListInfo>): List<RecordListInfo> {
        val selectedId = currentSelectedTaskId ?: data.firstOrNull {
            it is RecordListInfo.TaskItem
        }?.let {
            (it as RecordListInfo.TaskItem).id
        }

        return data.map { item ->
            if (item is RecordListInfo.TaskItem) {
                item.copy(isSelected = item.id == selectedId)
            } else {
                item
            }
        }.toList()
    }

    private fun createTaskResult(originalData: List<TaskResultInfo>): List<TaskResultInfo> {
        val studentList = studentManager.getCurrentList()
        Timber.d("Origin Data List : $originalData")
        Timber.d("Origin Student List : $studentList")

        val filteredResults = originalData.filter { result ->
            studentList.any { it.isJoinedClass() && it.studentId == result.studentId }
        }

        if (filteredResults.isEmpty()) {
            return studentList.map { studentInfo ->
                createDefaultGuestItem(studentInfo)
            }
        }

        val seatMap = studentList.associateBy { it.studentId }

        val updatedList = filteredResults.map { task ->
            val seatInfo = seatMap[task.studentId] ?: return@map task
            when (task) {
                is TaskResultInfo.Content -> task.copy(
                    serialNumber = seatInfo.serialNumber,
                    groupId = seatInfo.groupId.toString(),
                    version = task.version
                )

                is TaskResultInfo.Link -> task.copy(
                    serialNumber = seatInfo.serialNumber,
                    groupId = seatInfo.groupId.toString()
                )

                is TaskResultInfo.Guest -> task.copy(
                    serialNumber = seatInfo.serialNumber,
                    groupId = seatInfo.groupId.toString()
                )

                is TaskResultInfo.ApiFail -> task.copy(
                    serialNumber = seatInfo.serialNumber,
                    groupId = seatInfo.groupId.toString()
                )
            }
        }

        val seats = updatedList.toMutableList()
        val existingResults = updatedList.associateBy { it.serialNumber }

        // Find the missing serialNumbers
        val missingSerialNumbers = (1..seatMaxCount)
            .filter { serialNum -> !existingResults.containsKey(serialNum) }

        // Add Guest entries to fill in
        missingSerialNumbers.forEach { serialNum ->
            val seatInfo = studentList.find { it.serialNumber == serialNum }
            if (seatInfo != null) {
                seats.add(
                    createDefaultGuestItem(seatInfo)
                )
            }
        }
        seats.sortBy { it.serialNumber }

        return seats
    }

    private fun handleTaskResponseSocketEvent(data: JSONObject) {
        coroutineScope.launch {
            try {
                val message = TaskResponseSocketMessage.fromJSONObject(data)
                val taskStatusInfo = _recordResultFlow.value.taskInfo
                if (message.taskId != taskStatusInfo.taskId) return@launch

                val studentId = message.studentId
                val imageUrl = message.imgUrl
                val version = message.version

                val currentResults = _recordResultFlow.value.recordList.toMutableList()
                val index = currentResults.indexOfFirst { it.studentId == studentId }

                if (index != -1) {
                    val oldData = currentResults[index]
                    val updated = when (oldData) {
                        is TaskResultInfo.Content -> oldData.copy(
                            imgUrl = imageUrl.ifBlank { oldData.imgUrl },
                            version = if (version != 0) version else oldData.version,
                            triggerType = SubmitStatus.RESPONSE
                        )

                        is TaskResultInfo.Link -> oldData.copy(
                            linkIsOpened = true
                        )

                        else -> oldData
                    }
                    currentResults[index] = updated
                }
                _recordResultFlow.update {
                    it.copy(recordList = currentResults)
                }
            } catch (e: Exception) {
                Timber.e("Failed to parse TaskResponseMessage : ${e.message}")
                return@launch
            }
        }
    }

    private fun handleStudentJoinStatusWhenSocketDisconnect() {
        coroutineScope.launch {
            // workaround: backend data update is not real-time,
            // client has no control over sync latency, so we temporarily wait here
            delay(2000L)
            val taskStatusInfo = _recordResultFlow.value.taskInfo
            getTaskRecordByTaskId(taskId = taskStatusInfo.taskId)
        }
    }

    private fun handleReleaseSeat(newStudent: StudentInfo) {
        Timber.tag("StudentChanged").d("handleReleaseSeat : $newStudent")
        coroutineScope.launch {
            val updateList = _recordResultFlow.value.recordList.map { item ->
                if (item.serialNumber == newStudent.serialNumber)
                    // create new guest info to replace original record.
                    createDefaultGuestItem(newStudent)
                else item
            }
            Timber.tag("StudentChanged").d("handleReleaseSeat updateList : $updateList")
            _recordResultFlow.update { it.copy(recordList = updateList) }
        }
    }

    fun getStudentTaskResult(studentId: String) {
        studentManager.getCurrentList().first { it.studentId == studentId }.let { studentInfo ->
            handleSetStudentName(studentInfo)
        }
    }

    private fun handleSetStudentName(newStudent: StudentInfo) {
        Timber.tag("StudentChanged").d("handleSetStudentName : $newStudent")
        coroutineScope.launch {
            currentFocusTaskId?.let { taskId ->
                val response = (taskApiService.getStudentTasks(lessonId = lessonId, studentId = newStudent.studentId, taskId = taskId))
                when (response) {
                    is ApiResponse.Success -> {
                        try {
                            val newRecord = response.data.firstOrNull { it.taskId == taskId }
                            val updateList = _recordResultFlow.value.recordList.map { item ->
                                // using seatNumber to add record because original seat doesn't have student.
                                if (item.serialNumber == newStudent.serialNumber) {
                                    newRecord?.let {
                                        newRecord.toTaskResultInfo(newStudent)
                                    } ?: run {
                                        createDefaultGuestItem(newStudent)
                                    }
                                } else item
                            }
                            Timber.tag("StudentChanged").d("handleSetStudentName updateList : $updateList")
                            _recordResultFlow.update { it.copy(recordList = updateList) }
                        } catch (e: Exception) {
                            Timber.tag("StudentChanged").d("handleSetStudentName exception : $e")
                            _recordResultFlow.update { it.copy(recordList =  updateRecordWithApiFailedData( newStudent)) }
                        }
                    }
                    else -> {
                        Timber.tag("StudentChanged").d("handleSetStudentName api failed")
                        _recordResultFlow.update { it.copy(recordList =  updateRecordWithApiFailedData( newStudent)) }
                    }
                }
            }
        }
    }

    private fun updateRecordWithApiFailedData(studentInfo: StudentInfo): List<TaskResultInfo> {
        return _recordResultFlow.value.recordList.map { item ->
            if (item.serialNumber == studentInfo.serialNumber) {
                TaskResultInfo.ApiFail(
                    taskId = item.taskId,
                    studentId = studentInfo.studentId,
                    groupId = studentInfo.groupId.toString(),
                    seatNumber = studentInfo.displaySeatNumber,
                    serialNumber = studentInfo.serialNumber,
                    displayName = studentInfo.displayName
                    )
            } else {
                item
            }
        }
    }

    private fun handleSocketConnectionStatusChange(status: ConnectionState) {
        when (status) {
            is ConnectionState.Connected -> {
                stopFetchTaskResultPoller()

                //refresh data after socket connect
                coroutineScope.launch {
                    currentFocusTaskId?.let {
                        getTaskRecordByTaskId(it)
                    }
                }
            }

            is ConnectionState.Disconnected -> {
                startFetchTaskResultPoller()
            }

            else -> Unit
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun startFetchTaskResultPoller() {
        if (pollerJob?.isActive == true) return

        pollerJob = coroutineScope.launch {
            val ticker = ticker(
                delayMillis = AppConstants.SOCKET_DISCONNECT_POLLING_INTERVAL,
                initialDelayMillis = 0
            )

            try {
                for (event in ticker) {
                    currentFocusTaskId?.let {
                        getTaskRecordByTaskId(it)
                    }
                }
            } finally {
                ticker.cancel()
            }
        }
    }

    private fun stopFetchTaskResultPoller() {
        pollerJob?.cancel()
        pollerJob = null
    }

    private fun createPushRecordRequestBody(data: TaskResultInfo): CreateTaskBody? {
        return when (data) {
            is TaskResultInfo.Content -> {

                CreateTaskBody(
                    assign = TaskAssignType.ALL.code,
                    imageUrl = data.imgUrl,
                    linkUrl = "",
                    taskType = RecordType.CONTENT.code,
                    seatNumberList = emptyList(),
                )
            }

            else -> null
        }
    }

    private fun createDefaultGuestItem(studentInfo: StudentInfo): TaskResultInfo.Guest {
        return TaskResultInfo.Guest(
            seatNumber = studentInfo.displaySeatNumber,
            serialNumber = studentInfo.serialNumber,
            displayName = studentInfo.defaultDisplaySeatNumber,
            studentId = studentInfo.studentId,
            groupId = studentInfo.groupId.toString()
        )
    }
}

data class RecordResultState(
    val taskInfo: TaskStatusInfo = TaskStatusInfo(),
    val recordList: List<TaskResultInfo> = emptyList()
)