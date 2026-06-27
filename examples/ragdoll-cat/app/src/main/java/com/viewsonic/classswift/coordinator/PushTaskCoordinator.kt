package com.viewsonic.classswift.coordinator


import android.content.Context
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.CreateTaskBody
import com.viewsonic.classswift.api.body.LinkMeta
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.task.TaskApiResult
import com.viewsonic.classswift.data.task.TaskInfo
import com.viewsonic.classswift.data.task.UrlPreviewInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.ScreenshotManager
import com.viewsonic.classswift.ui.widget.task.enums.TaskAssignType
import com.viewsonic.classswift.ui.widget.task.enums.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

class PushTaskCoordinator(
    private val applicationContext: Context,
    private val screenshotManager: ScreenshotManager,
    private val classroomManager: ClassroomManager,
    private val accountManager: AccountManager,
    private var taskApiService: TaskApiService
) {

    private var lessonId = classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var screenshotJob: Job? = null

    // Add a default "Create Task" item if the content list is initially empty.
    private val _taskListFlow = MutableStateFlow<List<TaskInfo>>(listOf(TaskInfo.TaskCreate))
    val taskListFlow = _taskListFlow.asStateFlow()

    init {
        Timber.d("Lesson ID = $lessonId")
    }

    private fun startScreenshotCollecting() {
        // Starts collecting screenshot URIs from the screenshotManager's Flow.
        // Once a screenshot is received, it triggers content creation and then stops further collection.
        // This is meant to run only once per trigger to avoid repeated processing.
        screenshotJob = coroutineScope.launch(Dispatchers.Default) {
            screenshotManager.screenshotDataFlow.collect { screenshotUri ->
                Timber.d("Screenshot success screen uri : $screenshotUri ")
                createContentTask(imageUri = screenshotUri)
                stopScreenshotCollecting()
            }
        }
    }

    private fun stopScreenshotCollecting() {
        // Cancels the ongoing screenshot collection coroutine if it's active.
        // This ensures that only one screenshot is processed per trigger.
        screenshotJob?.cancel()
    }

    fun startScreenshot(onSuccess: () -> Unit, onFailed: () -> Unit) {
        startScreenshotCollecting()
        screenshotManager.startCaptureScreenshot(
            screenshotSource = AmplitudeConstant.EventProperties.Value.TASK,
            onSuccess = onSuccess,
            onFailed = onFailed,
            onCancel = { screenshotJob?.cancel() }
        )
    }

    fun deleteItem(data: TaskInfo) {
        _taskListFlow.update { it - data }
    }

    fun deletedSelectedItem() {
        _taskListFlow.update { currentList ->
            currentList.filterNot {
                it is TaskInfo.EditableTask && it.isSelected
            }
        }
    }

    fun updateItemSelectStatus(data: TaskInfo) {
        when (data) {
            is TaskInfo.Content -> updateContentTaskSelectStatus(data = data)
            is TaskInfo.Link -> updateLinkTaskSelectStatus(data = data)
            else -> Unit
        }
    }

    fun unselectTasks(data: List<TaskInfo>) {
        coroutineScope.launch(Dispatchers.IO) {
            val unselectIds = data.mapNotNull {
                when (it) {
                    is TaskInfo.Content -> it.id
                    is TaskInfo.Link -> it.id
                    else -> null
                }
            }.toSet()

            val updatedList = _taskListFlow.value.map { task ->
                when (task) {
                    is TaskInfo.Content -> {
                        if (unselectIds.contains(task.id)) {
                            task.copy(isSelected = false)
                        } else task
                    }

                    is TaskInfo.Link -> {
                        if (unselectIds.contains(task.id)) {
                            task.copy(isSelected = false)
                        } else task
                    }

                    else -> task
                }
            }

            //If all items are unselected, set isEditable = false for all items.
            val noSelected = updatedList.none { it is TaskInfo.EditableTask && it.isSelected }
            val finalList = if (noSelected) {
                updatedList.map {
                    if (it is TaskInfo.EditableTask) it.copyWithEditable(false) else it
                }
            } else {
                updatedList
            }

            _taskListFlow.value = finalList
        }
    }

    fun updateItemImageUploadSuccess(data: TaskInfo) {
        when (data) {
            is TaskInfo.Content -> updateItemImageUploadSuccess(data = data)
            else -> Unit
        }
    }

    fun getCurrentItemCount(): Int {
        //All item count, include upload error item
        return _taskListFlow.value.size
    }

    fun getSelectedItemCount(): Int {
        val selectedCount = _taskListFlow.value.count {
            it is TaskInfo.EditableTask && it.isSelected
        }
        return selectedCount
    }

    fun getSelectedTasks(): List<TaskInfo> {
        return _taskListFlow.value.filter {
            it is TaskInfo.EditableTask && it.isSelected
        }
    }

    fun unselectAllItem() {
        coroutineScope.launch {
            val currentList = _taskListFlow.value
            val updatedList = currentList.map { task ->

                when (task) {
                    is TaskInfo.Content -> task.copy(
                        isSelected = false,
                        isEditable = false
                    )

                    is TaskInfo.Link -> task.copy(
                        isSelected = false,
                        isEditable = false
                    )

                    else -> task
                }
            }

            _taskListFlow.value = updatedList
        }
    }

    private fun createContentTask(imageUri: String) {
        val uuId = UUID.randomUUID().toString()

        val selectedItemCount = _taskListFlow.value.filter {
            it is TaskInfo.EditableTask && it.isSelected
        }.size

        _taskListFlow.update { currentList ->

            val isEditable = selectedItemCount > 0

            val newTask = TaskInfo.Content(
                id = uuId,
                lessonId = lessonId,
                screenshotImgUrl = imageUri,
                isSelected = false,
                isEditable = isEditable,
                imagePreSignUrl = "",
                isUploadImageSuccess = false
            )

            //new content always insert at index = 1
            val updateList = when {
                currentList.size == 1 -> currentList + newTask
                else -> currentList.subList(0, 1) + newTask +
                        currentList.subList(1, currentList.size)
            }

            updateList.map { task ->
                when (task) {
                    is TaskInfo.Content -> task.copy(isEditable = isEditable)
                    is TaskInfo.Link -> task.copy(isEditable = isEditable)
                    else -> task
                }
            }
        }
    }

    fun createLinkTask(data: UrlPreviewInfo) {
        val uuId = UUID.randomUUID().toString()

        _taskListFlow.update { currentList ->

            val newTask = TaskInfo.Link(
                id = uuId,
                lessonId = lessonId,
                title = data.title,
                description = data.description,
                imageUrl = data.imageUrl,
                siteName = data.siteName,
                isSelected = true,
                isEditable = true,
                url = data.url,
                isValid = data.isValid
            )

            //new content always insert at index = 1
            val updateList = when {
                currentList.size == 1 -> currentList + newTask
                else -> currentList.subList(0, 1) + newTask +
                        currentList.subList(1, currentList.size)
            }

            updateList.map { task ->
                when (task) {
                    is TaskInfo.Content -> task.copy(isEditable = true)
                    is TaskInfo.Link -> task.copy(isEditable = true)
                    else -> task
                }
            }
        }
    }

    private fun updateContentTaskSelectStatus(data: TaskInfo.Content) {
        coroutineScope.launch(Dispatchers.IO) {
            val updatedList = _taskListFlow.value.map { taskInfo ->

                when {
                    // It's a Content task and the Id matches
                    taskInfo is TaskInfo.Content && taskInfo.id == data.id -> {
                        if (data.isSelected) {
                            taskInfo.copy(
                                isSelected = true,
                                isEditable = true,
                                isUploadImageSuccess = data.isUploadImageSuccess,
                                screenshotImgUrl = data.screenshotImgUrl
                            )
                        } else {
                            taskInfo.copy(isSelected = false)
                        }
                    }

                    // For any EditableTask, if data.isSelected is true, set it to editable
                    data.isSelected && taskInfo is TaskInfo.EditableTask -> {
                        taskInfo.copyWithEditable(true)
                    }

                    else -> taskInfo
                }
            }

            //If all items are unselected, set isEditable = false for all items.
            val noSelected = updatedList.none { it is TaskInfo.EditableTask && it.isSelected }
            val finalList = if (noSelected) {
                updatedList.map { task ->
                    when (task) {
                        is TaskInfo.EditableTask -> task.copyWithEditable(false)
                        else -> task
                    }
                }
            } else updatedList

            _taskListFlow.value = finalList
        }
    }

    private fun updateLinkTaskSelectStatus(data: TaskInfo.Link) {
        coroutineScope.launch(Dispatchers.IO) {
            val updatedList = _taskListFlow.value.map { taskInfo ->
                when {
                    // It's a Link task and the ID matches : copy based on isSelected
                    taskInfo is TaskInfo.Link && taskInfo.id == data.id -> {
                        if (data.isSelected) {
                            taskInfo.copy(
                                isSelected = true,
                                isEditable = true
                            )
                        } else {
                            taskInfo.copy(isSelected = false)
                        }
                    }

                    // Any other EditableTask: if data.isSelected is true, set isEditable to true
                    data.isSelected && taskInfo is TaskInfo.EditableTask -> {
                        taskInfo.copyWithEditable(true)
                    }

                    else -> taskInfo
                }
            }

            //If all items are unselected, set isEditable = false for all items.
            val noSelected = updatedList.none { it is TaskInfo.EditableTask && it.isSelected }
            val finalList = if (noSelected) {
                updatedList.map { task ->
                    when (task) {
                        is TaskInfo.EditableTask -> task.copyWithEditable(false)
                        else -> task
                    }
                }
            } else updatedList

            _taskListFlow.value = finalList
        }
    }

    private fun updateItemImageUploadSuccess(data: TaskInfo.Content) {
        _taskListFlow.update { currentList ->
            currentList.map { task ->
                if (task !is TaskInfo.Content) return@map task
                if (task.id == data.id) {
                    task.copy(
                        isSelected = data.isSelected,
                        isEditable = data.isEditable,
                        isUploadImageSuccess = data.isUploadImageSuccess,
                        imagePreSignUrl = data.imagePreSignUrl
                    )
                } else {
                    task
                }
            }
        }
    }

    suspend fun pushTasks(tasks: List<TaskInfo>): List<TaskApiResult<TaskInfo>> {
        val resultList = mutableListOf<TaskApiResult<TaskInfo>>()

        for (taskInfo in tasks) {
            val requestBody = createRequestBody(data = taskInfo)
            Timber.d("Create task request body: ${requestBody?.toString()}")

            if (requestBody == null) {
                resultList.add(
                    TaskApiResult(
                        data = taskInfo,
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
                    TaskApiResult(data = taskInfo, isSuccess = true)
                }

                is ApiResponse.Rfc7807Failure -> {
                    TaskApiResult(
                        data = taskInfo,
                        isSuccess = false,
                        errMsg = response.error.detail,
                        errorBody = response.error
                    )
                }

                is ApiResponse.NetworkDisconnected -> {
                    TaskApiResult(
                        data = taskInfo,
                        isSuccess = false,
                        errMsg = applicationContext.getString(
                            R.string.connection_network_error_disconnect
                        )
                    )
                }

                else -> {
                    TaskApiResult(
                        data = taskInfo,
                        isSuccess = false
                    )
                }
            }

            resultList.add(result)
        }

        return resultList
    }

    private fun createRequestBody(data: TaskInfo): CreateTaskBody? {
        return when (data) {
            is TaskInfo.Content -> {
                CreateTaskBody(
                    assign = TaskAssignType.ALL.code,
                    imageUrl = data.imagePreSignUrl,
                    linkUrl = "",
                    taskType = TaskType.CONTENT.code,
                    seatNumberList = emptyList(),
                )
            }

            is TaskInfo.Link -> {

                val linkMeta = if (data.isValid
                ) {
                    LinkMeta(
                        title = data.title,
                        description = data.description,
                        siteName = data.siteName,
                        image = data.imageUrl
                    )
                } else {
                    null
                }

                val imageUrl = if (data.isValid) {
                    data.imageUrl
                } else {
                    null
                }

                CreateTaskBody(
                    assign = TaskAssignType.ALL.code,
                    imageUrl = imageUrl,
                    linkUrl = data.url,
                    taskType = TaskType.LINK.code,
                    seatNumberList = emptyList(),
                    linkMeta = linkMeta
                )
            }

            else -> null
        }
    }
}
