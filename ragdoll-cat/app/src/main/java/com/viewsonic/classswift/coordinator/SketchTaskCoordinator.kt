package com.viewsonic.classswift.coordinator

import android.content.Context
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.response.UpdateTaskResult
import com.viewsonic.classswift.api.response.toTaskResultInfo
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.quiz.SketchEventInfo
import com.viewsonic.classswift.data.quiz.SketchResultState
import com.viewsonic.classswift.data.socket.task.TaskResponseSocketMessage
import com.viewsonic.classswift.data.task.LinkMetaInfo
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
import com.viewsonic.classswift.ui.widget.task.enums.RecordType
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus
import com.viewsonic.classswift.ui.widget.task.enums.TaskStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID

/**
 * Sketch Response 結果頁 (VSFT-8453 / 8454) 專屬 task lifecycle coordinator。
 *
 * 從 [RecordsCoordinator] 拷貝後砍：
 * - `_recordListFlow` / task list 概念（Sketch 是單批次結果頁）
 * - `unclosedMissionUiManager` 通知（Sketch「發回」≠「結束任務」）
 * - `isLabelAsMarkMode` / `isPushRecordMode` flag（Sketch 結果頁是唯讀，無批改 / push 模式）
 * - `endTask` / `pushTasks` / `getRecordList` / `hasTaskInProgress` / `getTasks(filter)`
 * - socket disconnect polling fallback（per Tech Lead：Sketch 不主動 polling）
 *
 * 保留：
 * - `getTaskById` + `createTaskResult` + `createDefaultGuestItem`（補 Absent）
 * - 學生離座 / 改名 / 重連 socket 訂閱（行為跟 PushRespond 一致）
 * - `EVENT_TASK_RESPONSE` socket handler：task 進結果頁時仍 IN_PROGRESS，學生可再次回覆 →
 *   即時翻回 Click to view 並更新 imgUrl/version（見 [handleTaskResponseSocketEvent]）
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8453">VSFT-8453</a>
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8454">VSFT-8454</a>
 */
class SketchTaskCoordinator(
    private val applicationContext: Context,
    private val classroomManager: ClassroomManager,
    private val studentManager: StudentManager,
    private val taskApiService: TaskApiService,
    private val socketManager: SocketManager,
    private val accountManager: AccountManager
) {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val lessonId = classroomManager.classroomDataStateFlow.value
        .selectedClassroomInfo.lessonId
    private val seatMaxCount = classroomManager.classroomDataStateFlow.value
        .selectedClassroomInfo.maxStudentCount

    private val _sketchResultFlow = MutableStateFlow(SketchResultState())
    val sketchResultFlow: StateFlow<SketchResultState> = _sketchResultFlow.asStateFlow()

    // extraBufferCapacity=1 防止 Window collector 尚未啟動時 emit 永遠 suspend（同 SketchMarkHandler 做法）。
    private val _eventFlow = MutableSharedFlow<SketchEventInfo>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val eventFlow: SharedFlow<SketchEventInfo> = _eventFlow.asSharedFlow()

    private var currentFocusTaskId: String? = null

    /**
     * 當前 batch task id（Sketch 派題用 batchCreateTasks 取得的整批 id）。
     * 由 [MvbSketchResponseStartWindow] 在進入結果頁時透過 [setBatchTaskId] 帶入；
     * 為 null 時 [closeBatch] 直接 no-op（不打 close API，仍可正常關窗）。
     */
    private var batchTaskId: String? = null

    init {
        Timber.d("SketchTaskCoordinator init, lessonId = $lessonId")
        observeData()
    }

    // ─── 對外 API ───────────────────────────────────────────────────────────

    fun getCurrentFocusId(): String? = currentFocusTaskId

    fun setBatchTaskId(id: String?) {
        batchTaskId = id
    }

    /**
     * 關閉整批 batch task（Sketch Response 結果頁 [X] 關窗時呼叫）。
     *
     * 取代舊 task-by-task [RecordsCoordinator] 的 `closeTask` 迴圈，改用後端新的
     * `closeBatchTask`（`PUT /batch_tasks/{batch_task_id}`）一次關整批。
     *
     * @return true 表示 (a) 成功關閉 batch，或 (b) batchTaskId 為 null（無需 close，直接關窗）；
     *   false 表示 API 呼叫失敗，Window 應顯示 error toast 並阻止關窗。
     */
    suspend fun closeBatch(): Boolean {
        val id = batchTaskId
        if (id.isNullOrEmpty()) {
            Timber.d("Sketch closeBatch skip: batchTaskId is null (entry 尚未帶入)")
            return true
        }
        return try {
            val response = taskApiService.closeBatchTask(
                token = accountManager.getBearerToken(),
                lessonId = lessonId,
                batchTaskId = id,
            )
            when (response) {
                is ApiResponse.Success -> {
                    Timber.d("Sketch closeBatch success: ${response.data.data.status}")
                    true
                }
                else -> {
                    Timber.w("Sketch closeBatch failed: $response")
                    false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Sketch closeBatch error")
            false
        }
    }

    /**
     * 載入單一 task 結果（Sketch Response 結果頁進入時呼叫）。
     *
     * Caller：[MvbSketchResponseStartWindow] 點「Collect all and mark」切換到結果頁時傳入 task_id。
     */
    suspend fun loadResult(taskId: String) {
        currentFocusTaskId = taskId
        fetchAndEmit(taskId)
    }

    /** 重新撈當前 task 結果（user 點 Refresh button）。 */
    suspend fun refresh() {
        currentFocusTaskId?.let { fetchAndEmit(it) }
    }

    /**
     * 將 [SketchMarkHandler] 批改成功後的結果，同步回 [sketchResultFlow]。
     *
     * 從 `RecordsCoordinator.updateMarkedResult(List<UpdateTaskResult>)` 拷貝後簡化：
     * Sketch 只走 single-mark，參數從 list 改為單一 [UpdateTaskResult]。
     */
    suspend fun updateMarkedResult(data: UpdateTaskResult) {
        if (data.taskId != _sketchResultFlow.value.taskInfo.taskId) return

        // 使用 update 而非 emit，跟同檔其他 mutation 一致。
        _sketchResultFlow.update { current ->
            val updated = current.recordList.map { old ->
                when (old) {
                    is TaskResultInfo.Content -> {
                        if (old.studentId == data.studentId) {
                            val isNeedUpdateGroupId = data.groupId.isNotEmpty()
                            old.copy(
                                imgUrl = data.imgUrl,
                                triggerType = SubmitStatus.fromCode(data.triggerType),
                                version = data.version,
                                groupId = if (isNeedUpdateGroupId) data.groupId else old.groupId,
                                isSelected = false,
                            )
                        } else old
                    }
                    else -> old
                }
            }
            current.copy(recordList = updated)
        }
    }

    /**
     * 主動觸發以 studentId 重新同步該學生資料（例：UI 上重試一個 ApiFail 學生）。
     *
     * Note: 使用 `firstOrNull` 而非 `first`，避免該學生已離座 / 還沒同步進 student list 時
     * 拋 `NoSuchElementException` crash 整個 widget retry 流程。
     */
    fun getStudentTaskResult(studentId: String) {
        studentManager.getCurrentList()
            .firstOrNull { it.studentId == studentId }
            ?.let { studentInfo -> handleSetStudentName(studentInfo) }
    }

    // ─── 內部訂閱 ───────────────────────────────────────────────────────────

    private fun observeData() {
        coroutineScope.launch {
            socketManager.receivedEventDataFlow.collect { data ->
                handleSocketEvent(data)
            }
        }

        coroutineScope.launch {
            socketManager.connectionStateSharedFlow.collect { state ->
                Timber.d("Sketch socket state: $state")
                handleSocketConnectionStatusChange(state)
            }
        }

        coroutineScope.launch {
            studentManager.studentChangeReasonFlow.collect { reason ->
                if (reason is StudentChangeReason.SocketReconnected) {
                    handleStudentJoinStatusWhenSocketDisconnect()
                    return@collect
                }
                when (reason) {
                    is StudentChangeReason.RejoinSeat -> {
                        Timber.tag("StudentChanged").d("Sketch RejoinSeat")
                        handleSetStudentName(reason.newStudent)
                    }
                    is StudentChangeReason.SetStudentName -> {
                        Timber.tag("StudentChanged").d("Sketch SetStudentName")
                        handleSetStudentName(reason.newStudent)
                    }
                    is StudentChangeReason.ReleaseSeat -> {
                        Timber.tag("StudentChanged").d("Sketch ReleaseSeat")
                        handleReleaseSeat(reason.newStudent)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleSocketEvent(data: ReceivedEventData) {
        when (data.event) {
            ReceivedEvent.EVENT_TASK_RESPONSE -> {
                data.messageJsonObject?.let { handleTaskResponseSocketEvent(it) }
            }
            else -> Unit
        }
    }

    /**
     * [VSFT-8453] 學生再次回覆時即時更新（方案 A，行為與 [RecordsCoordinator] 一致）。
     *
     * task 進結果頁時仍 `IN_PROGRESS`（close 移到 [closeBatch]／結果頁 [X]），學生可再次提交。
     * 收到 `EVENT_TASK_RESPONSE` 時更新該學生 record：
     * - `triggerType` 打回 [SubmitStatus.RESPONSE]（→ `SketchStudentStatus.CLICK_TO_VIEW`），讓老師
     *   批改後的「Handed back」自動翻回「Click to view」，免按 Refresh。
     * - 同步更新 `imgUrl` / `version` —— 否則老師再次點 Click to view 進 [SketchReviewWidget]
     *   仍會看到舊作品（`enterSketchReviewMode` 用的是 list 快取的 record）。
     *
     * 註：[SketchReviewWidget] 為滿版 overlay，老師批改當下看不到底下 list，故 list 縮圖即時換圖
     * 不會干擾正在進行的批改；老師批的是點開當下載入 canvas 的版本，存檔關閉後再點才吃到新版。
     */
    private fun handleTaskResponseSocketEvent(data: JSONObject) {
        coroutineScope.launch {
            try {
                val message = TaskResponseSocketMessage.fromJSONObject(data)
                if (message.taskId != _sketchResultFlow.value.taskInfo.taskId) return@launch

                val currentResults = _sketchResultFlow.value.recordList.toMutableList()
                val index = currentResults.indexOfFirst { it.studentId == message.studentId }
                if (index == -1) return@launch

                val updated = when (val oldData = currentResults[index]) {
                    is TaskResultInfo.Content -> oldData.copy(
                        imgUrl = message.imgUrl.ifBlank { oldData.imgUrl },
                        version = if (message.version != 0) message.version else oldData.version,
                        triggerType = SubmitStatus.RESPONSE,
                    )
                    // 防禦性分支：Sketch 派截圖題，實際不應出現 Link 類型；保留避免未來 task mix 時 crash。
                    is TaskResultInfo.Link -> oldData.copy(linkIsOpened = true)
                    else -> oldData
                }
                currentResults[index] = updated
                _sketchResultFlow.update { it.copy(recordList = currentResults) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Sketch handleTaskResponseSocketEvent failed")
            }
        }
    }

    // ─── API call ───────────────────────────────────────────────────────────

    private suspend fun fetchAndEmit(taskId: String) {
        Timber.d("Sketch fetchAndEmit taskId: $taskId")
        try {
            val result = getTaskById(taskId)
            if (!result.isSuccess) {
                _eventFlow.emit(SketchEventInfo.GetRecordByTaskIdFailed)
                return
            }
            result.data?.let { data ->
                val taskStatusInfo = TaskStatusInfo(
                    infoId = UUID.randomUUID().toString(),
                    taskId = data.taskId,
                    assign = data.assign,
                    status = data.status,
                    sequenceNumber = data.seq,
                    createAt = data.createdAt,
                    recordType = if (data.linkUrl.isNotEmpty()) RecordType.LINK else RecordType.CONTENT
                )
                val synced = createTaskResult(originalData = data.taskResults)
                _sketchResultFlow.emit(
                    SketchResultState(
                        taskInfo = taskStatusInfo,
                        recordList = synced,
                        taskImgUrl = data.imgUrl,
                    )
                )
            }
        } catch (e: CancellationException) {
            // 不要吞 coroutine cancellation
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Sketch fetchAndEmit error")
            _eventFlow.emit(SketchEventInfo.GetRecordByTaskIdFailed)
        }
    }

    /**
     * 拷貝自 `RecordsCoordinator.getTaskById`（行 564-689），邏輯不變。
     */
    private suspend fun getTaskById(taskId: String): TaskApiResult<TaskRecordInfo?> {
        val response = taskApiService.getTaskById(
            token = accountManager.getBearerToken(),
            lessonId = lessonId,
            taskId = taskId
        )

        Timber.d("Sketch getTaskById response: $response")

        when (response) {
            is ApiResponse.Success -> {
                response.data.data.let {
                    val taskResults = mutableListOf<TaskResultInfo>()
                    val type = if (it.linkUrl.isNotEmpty()) RecordType.LINK else RecordType.CONTENT
                    val taskOriginalImgUrl = it.imgUrl

                    if (type == RecordType.CONTENT) {
                        it.taskResults.forEach { taskResult ->
                            taskResults.add(
                                TaskResultInfo.Content.create(
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
                            )
                        }
                    } else {
                        it.taskResults.forEach { taskResult ->
                            taskResults.add(
                                TaskResultInfo.Link.create(
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
                            )
                        }
                    }

                    return TaskApiResult(
                        data = TaskRecordInfo(
                            taskId = it.taskId,
                            assign = it.assign,
                            status = TaskStatus.fromString(it.status),
                            seq = it.sequence,
                            createdAt = it.createdAt,
                            linkUrl = it.linkUrl,
                            linkMeta = it.linkMeta,
                            imgUrl = it.imgUrl,
                            taskResults = taskResults
                        ),
                        isSuccess = true
                    )
                }
            }

            is ApiResponse.Rfc7807Failure -> return TaskApiResult(
                data = null,
                isSuccess = false,
                errMsg = response.error.detail,
                errorBody = response.error
            )

            is ApiResponse.NetworkDisconnected -> return TaskApiResult(
                data = null,
                isSuccess = false,
                errMsg = applicationContext.getString(R.string.connection_network_error_disconnect)
            )

            is ApiResponse.ExceptionFailure -> {
                Timber.e(response.exception, "Sketch getTaskById exception failure")
                return TaskApiResult(data = null, isSuccess = false)
            }

            else -> return TaskApiResult(data = null, isSuccess = false)
        }
    }

    // ─── 補位 / mapping ────────────────────────────────────────────────────

    /**
     * 拷貝自 `RecordsCoordinator.createTaskResult`（行 966-1028），邏輯不變。
     * 把 API 回來的 [originalData] 用班級 [studentManager] 同步 seat / groupId，
     * 不在 student list 的位子用 [createDefaultGuestItem] 補成 [TaskResultInfo.Guest]（Absent）。
     */
    private fun createTaskResult(originalData: List<TaskResultInfo>): List<TaskResultInfo> {
        val studentList = studentManager.getCurrentList()
        Timber.d("Sketch createTaskResult origin: $originalData, students: $studentList")

        val filteredResults = originalData.filter { result ->
            studentList.any { it.isJoinedClass() && it.studentId == result.studentId }
        }

        if (filteredResults.isEmpty()) {
            if (originalData.isNotEmpty()) {
                // originalData has submissions but no joined students — likely transient during
                // socket reconnect. Preserve existing data rather than replacing with all-Absent,
                // since Sketch result page has only one manual Refresh and users would lose context.
                Timber.w("Sketch createTaskResult: filteredResults empty but originalData non-empty; preserving originalData (transient reconnect?)")
                return originalData
            }
            return studentList.map { studentInfo -> createDefaultGuestItem(studentInfo) }
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

        val missingSerialNumbers = (1..seatMaxCount)
            .filter { serialNum -> !existingResults.containsKey(serialNum) }

        missingSerialNumbers.forEach { serialNum ->
            val seatInfo = studentList.find { it.serialNumber == serialNum }
            if (seatInfo != null) {
                seats.add(createDefaultGuestItem(seatInfo))
            }
        }
        seats.sortBy { it.serialNumber }

        return seats
    }

    // ─── Socket / 學生變動 ─────────────────────────────────────────────────

    /**
     * 拷貝自 `RecordsCoordinator.handleReleaseSeat`（行 1081-1093）。
     * 學生離座 → 該位子的 record 換成 [TaskResultInfo.Guest]（顯示為 Absent）。
     */
    private fun handleReleaseSeat(newStudent: StudentInfo) {
        Timber.tag("StudentChanged").d("Sketch handleReleaseSeat: $newStudent")
        coroutineScope.launch {
            val updateList = _sketchResultFlow.value.recordList.map { item ->
                if (item.serialNumber == newStudent.serialNumber) {
                    createDefaultGuestItem(newStudent)
                } else item
            }
            _sketchResultFlow.update { it.copy(recordList = updateList) }
        }
    }

    /**
     * 拷貝自 `RecordsCoordinator.handleSetStudentName`（行 1101-1134）。
     * 學生改名 / 重新入座 → 用 `getStudentTasks` 取該學生最新 task result 替換。
     */
    private fun handleSetStudentName(newStudent: StudentInfo) {
        Timber.tag("StudentChanged").d("Sketch handleSetStudentName: $newStudent")
        coroutineScope.launch {
            currentFocusTaskId?.let { taskId ->
                val response = taskApiService.getStudentTasks(
                    lessonId = lessonId,
                    studentId = newStudent.studentId,
                    taskId = taskId
                )
                when (response) {
                    is ApiResponse.Success -> {
                        try {
                            val newRecord = response.data.firstOrNull { it.taskId == taskId }
                            val updateList = _sketchResultFlow.value.recordList.map { item ->
                                if (item.serialNumber == newStudent.serialNumber) {
                                    newRecord?.toTaskResultInfo(newStudent)
                                        ?: createDefaultGuestItem(newStudent)
                                } else item
                            }
                            _sketchResultFlow.update { it.copy(recordList = updateList) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.tag("StudentChanged").e(e, "Sketch handleSetStudentName exception")
                            _sketchResultFlow.update {
                                it.copy(recordList = updateRecordWithApiFailedData(newStudent))
                            }
                        }
                    }
                    else -> {
                        Timber.tag("StudentChanged").d("Sketch handleSetStudentName api failed")
                        _sketchResultFlow.update {
                            it.copy(recordList = updateRecordWithApiFailedData(newStudent))
                        }
                    }
                }
            }
        }
    }

    /**
     * 拷貝自 `RecordsCoordinator.updateRecordWithApiFailedData`（行 1136-1151）。
     */
    private fun updateRecordWithApiFailedData(studentInfo: StudentInfo): List<TaskResultInfo> {
        return _sketchResultFlow.value.recordList.map { item ->
            if (item.serialNumber == studentInfo.serialNumber) {
                TaskResultInfo.ApiFail(
                    taskId = item.taskId,
                    studentId = studentInfo.studentId,
                    groupId = studentInfo.groupId.toString(),
                    seatNumber = studentInfo.displaySeatNumber,
                    serialNumber = studentInfo.serialNumber,
                    displayName = studentInfo.displayName
                )
            } else item
        }
    }

    /**
     * 從 `RecordsCoordinator.handleSocketConnectionStatusChange`（行 1153-1172）改寫。
     *
     * 改動：
     * - **Sprint 20 不啟 polling fallback**（per Tech Lead 決策）
     * - `Connected` 時 refresh 當前 task
     */
    private fun handleSocketConnectionStatusChange(status: ConnectionState) {
        when (status) {
            is ConnectionState.Connected -> {
                coroutineScope.launch {
                    currentFocusTaskId?.let { fetchAndEmit(it) }
                }
            }
            is ConnectionState.Disconnected -> Unit // 不啟 polling
            else -> Unit
        }
    }

    /**
     * 拷貝自 `RecordsCoordinator.handleStudentJoinStatusWhenSocketDisconnect`（行 1071-1078）。
     * Socket 重連後 backend 有 sync latency，這裡延遲 2s 再 refresh。
     */
    private fun handleStudentJoinStatusWhenSocketDisconnect() {
        coroutineScope.launch {
            delay(2000L)
            val taskId = _sketchResultFlow.value.taskInfo.taskId
            if (taskId.isNotEmpty()) {
                fetchAndEmit(taskId)
            }
        }
    }

    /**
     * 拷貝自 `RecordsCoordinator.createDefaultGuestItem`（行 1218-1226）。
     */
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
