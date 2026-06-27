package com.viewsonic.classswift.coordinator

import com.viewsonic.classswift.api.body.UpdateTaskResultBody
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.ui.widgetmodel.records.state.MarkUpdateState
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.ui.widget.task.enums.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID

class RecordMarkHandler(
    private var taskApiService: TaskApiService,
    private val accountManager: AccountManager
) {
    private val _markUpdateResultFlow = MutableStateFlow<MarkUpdateState>(MarkUpdateState.Idle)
    val markUpdateResultFlow = _markUpdateResultFlow.asStateFlow()

    suspend fun updateContentTaskResult(
        taskId: String,
        recordResults: List<TaskResultInfo.Content>,
        isMultiMark: Boolean = false
    ) {
        val taskResultBodies = recordResults.map { resultInfo ->
            Timber.d("Update task result student name: ${resultInfo.displayName}")

            UpdateTaskResultBody(
                studentId = resultInfo.studentId,
                taskType = TaskType.CONTENT.code,
                imgUrl = resultInfo.imgUrl,
                taskResultType = resultInfo.triggerType.code,
                version = resultInfo.version
            )
        }.toList()
        Timber.d("Update task result request body: ${taskResultBodies.toString()}")

        if (taskResultBodies.isEmpty()) {
            return
        }

        val response = taskApiService.updateTaskResult(
            token = accountManager.getBearerToken(),
            taskId = taskId,
            body = taskResultBodies
        )

        Timber.d("Update task result response: ${response.toString()}")

        when (response) {
            is ApiResponse.Success -> {
                Timber.d("Update task success List: ${response.data.success}")
                Timber.d("Update task failed List: ${response.data.failed}")
                val successList = response.data.success
                val failedList = response.data.failed

                if (isMultiMark) {
                    _markUpdateResultFlow.emit(
                        MarkUpdateState.MultiMarkSuccess(
                            id = UUID.randomUUID().toString(),
                            success = successList,
                            failed = failedList
                        )
                    )
                } else {
                    _markUpdateResultFlow.emit(
                        MarkUpdateState.SingleMarkSuccess(
                            id = UUID.randomUUID().toString(),
                            success = successList.firstOrNull(),
                            failed = failedList.firstOrNull()
                        )
                    )
                }
            }

            is ApiResponse.Rfc7807Failure -> {
                Timber.d("Update task failure: ${response.error}")
                _markUpdateResultFlow.emit(
                    MarkUpdateState.Failed(
                        id = UUID.randomUUID().toString(),
                        isMultiMark = isMultiMark,
                        errorMessage = response.error.title
                    )
                )
            }

            is ApiResponse.NetworkDisconnected -> {
                _markUpdateResultFlow.emit(
                    MarkUpdateState.Failed(
                        id = UUID.randomUUID().toString(),
                        isMultiMark = isMultiMark,
                        errorMessage = "NetworkDisconnected"
                    )
                )
            }

            else -> {
                _markUpdateResultFlow.emit(
                    MarkUpdateState.Failed(
                        id = UUID.randomUUID().toString(),
                        isMultiMark = isMultiMark,
                        errorMessage = ""
                    )
                )
            }
        }
    }
}