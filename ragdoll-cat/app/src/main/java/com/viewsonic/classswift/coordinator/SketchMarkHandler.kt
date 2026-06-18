package com.viewsonic.classswift.coordinator

import com.viewsonic.classswift.api.body.UpdateTaskResultBody
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.data.quiz.SketchMarkUpdateState
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.ui.widget.task.enums.TaskType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.UUID

/**
 * Sketch Response「Save and hand back」批改送出 handler。
 *
 * 從 [RecordMarkHandler] 拷貝後簡化：
 * - 砍 `isMultiMark` 邏輯（Sketch 結果頁只走 single-mark）
 * - 輸出 [SketchMarkUpdateState] 取代 `MarkUpdateState`
 * - 共用 `updateTaskResult` API（後端 task by task API 仍存在）
 *
 * Result flow 使用 [MutableSharedFlow]（replay=0, buffer=1, DROP_OLDEST）而非 StateFlow，
 * 避免 Window re-attach 時 redeliver 上一次 emit 造成重複 dismiss / toast。
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8454">VSFT-8454</a>
 */
class SketchMarkHandler(
    private val taskApiService: TaskApiService,
    private val accountManager: AccountManager
) {
    private val _markUpdateResultFlow = MutableSharedFlow<SketchMarkUpdateState>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val markUpdateResultFlow: SharedFlow<SketchMarkUpdateState> = _markUpdateResultFlow.asSharedFlow()

    /**
     * 送出單一學生作品的批改結果（含 hand back）。
     *
     * `taskResultType` 以 [TaskResultInfo.Content.triggerType] 為主；
     * Round 18 後流程已是 RESPONSE → updateTaskResult 設成 GRADED → re-fetch UI 顯示 Handed back。
     */
    suspend fun updateContentTaskResult(
        taskId: String,
        recordResult: TaskResultInfo.Content
    ) {
        Timber.d("Update task result student name: ${recordResult.displayName}")

        val body = UpdateTaskResultBody(
            studentId = recordResult.studentId,
            taskType = TaskType.CONTENT.code,
            imgUrl = recordResult.imgUrl,
            taskResultType = recordResult.triggerType.code,
            version = recordResult.version
        )
        Timber.d("Update task result request body: $body")

        val response = taskApiService.updateTaskResult(
            token = accountManager.getBearerToken(),
            taskId = taskId,
            body = listOf(body)
        )

        Timber.d("Update task result response: $response")

        when (response) {
            is ApiResponse.Success -> {
                Timber.d("Update task success List: ${response.data.success}")
                Timber.d("Update task failed List: ${response.data.failed}")
                _markUpdateResultFlow.emit(
                    SketchMarkUpdateState.Success(
                        id = UUID.randomUUID().toString(),
                        success = response.data.success.firstOrNull(),
                        failed = response.data.failed.firstOrNull()
                    )
                )
            }
            is ApiResponse.Rfc7807Failure -> {
                Timber.d("Update task failure: ${response.error}")
                emitFailure(SketchMarkUpdateState.FailureReason.ApiError(response.error.title))
            }
            is ApiResponse.NetworkDisconnected -> {
                emitFailure(SketchMarkUpdateState.FailureReason.NetworkDisconnected)
            }
            else -> {
                emitFailure(SketchMarkUpdateState.FailureReason.Unknown)
            }
        }
    }

    /** 4 條失敗 path 共用 — Sonar S4144 抽取。 */
    private suspend fun emitFailure(reason: SketchMarkUpdateState.FailureReason) {
        _markUpdateResultFlow.emit(
            SketchMarkUpdateState.Failed(
                id = UUID.randomUUID().toString(),
                reason = reason,
            )
        )
    }
}
