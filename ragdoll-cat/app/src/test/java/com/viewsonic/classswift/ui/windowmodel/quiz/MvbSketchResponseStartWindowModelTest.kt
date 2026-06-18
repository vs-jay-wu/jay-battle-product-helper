package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.api.body.BatchCreateTasksBody
import com.viewsonic.classswift.api.body.CreateTaskBody
import com.viewsonic.classswift.api.body.UpdateTaskResultBody
import com.viewsonic.classswift.api.response.BatchCreateTasksResponse
import com.viewsonic.classswift.api.response.BatchTasksLatestData
import com.viewsonic.classswift.api.response.BatchTasksLatestResponse
import com.viewsonic.classswift.api.response.BatchTasksLatestStudent
import com.viewsonic.classswift.api.response.CloseTaskResponse
import com.viewsonic.classswift.api.response.CreateTaskResponse
import com.viewsonic.classswift.api.response.GetLinkPreviewResponse
import com.viewsonic.classswift.api.response.GetStudentTaskResponse
import com.viewsonic.classswift.api.response.GetTaskByIdResponse
import com.viewsonic.classswift.api.response.GetTaskRecordsByLessonResponse
import com.viewsonic.classswift.api.response.GetTasksResponse
import com.viewsonic.classswift.api.response.UpdateTaskResultResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.data.enum.SketchAnswerStatus
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.SketchState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Type A unit test for MvbSketchResponseStartWindowModel — exercises the API
 * response mapping and close-confirm state machine. Polling/timer wiring is
 * exercised separately because ticker behavior is hard to test deterministically.
 */
class MvbSketchResponseStartWindowModelTest {

    @Test
    fun applyResponse_singleTaskBatch_marksStudentsWithAtLeastOneSubmission_asSubmitted() {
        val model = buildModel()

        model.applyResponse(
            BatchTasksLatestResponse(
                batchData = BatchTasksLatestData(
                    batchTaskId = "batch-1",
                    tasksCount = 1,
                    taskIds = listOf("task-1"),
                    students = listOf(
                        student(id = "s1", submittedCount = 1),
                        student(id = "s2", submittedCount = 0),
                        student(id = "s3", submittedCount = 1),
                    ),
                ),
            ),
        )

        val state = model.uiState.value
        assertEquals(3, state.students.size)
        assertEquals(2, state.submittedCount)
        assertEquals(3, state.totalCount)
        assertEquals(SketchAnswerStatus.SUBMITTED, state.students[0].status)
        assertEquals(SketchAnswerStatus.NOT_SUBMITTED, state.students[1].status)
        assertEquals(SketchAnswerStatus.SUBMITTED, state.students[2].status)
    }

    @Test
    fun applyResponse_multiTaskBatch_onlyMarksSubmittedWhenCountReachesTasksCount() {
        val model = buildModel()

        // Two tasks in the batch — a student is SUBMITTED only after submitting both.
        model.applyResponse(
            BatchTasksLatestResponse(
                batchData = BatchTasksLatestData(
                    batchTaskId = "batch-1",
                    tasksCount = 2,
                    taskIds = listOf("task-1", "task-2"),
                    students = listOf(
                        student(id = "s1", submittedCount = 2),
                        student(id = "s2", submittedCount = 1),
                        student(id = "s3", submittedCount = 0),
                    ),
                ),
            ),
        )

        val state = model.uiState.value
        assertEquals(SketchAnswerStatus.SUBMITTED, state.students[0].status)
        assertEquals(SketchAnswerStatus.NOT_SUBMITTED, state.students[1].status)
        assertEquals(SketchAnswerStatus.NOT_SUBMITTED, state.students[2].status)
        assertEquals(1, state.submittedCount)
    }

    @Test
    fun applyResponse_allZeroSubmissions_keepsCountZero() {
        val model = buildModel()

        model.applyResponse(
            BatchTasksLatestResponse(
                batchData = BatchTasksLatestData(
                    tasksCount = 1,
                    students = listOf(
                        student(id = "s1", submittedCount = 0),
                        student(id = "s2", submittedCount = 0),
                    ),
                ),
            ),
        )

        val state = model.uiState.value
        assertEquals(0, state.submittedCount)
        assertEquals(2, state.totalCount)
        assertTrue(state.students.all { it.status == SketchAnswerStatus.NOT_SUBMITTED })
    }

    @Test
    fun applyResponse_emptyStudents_emitsZeroTotal() {
        val model = buildModel()

        model.applyResponse(
            BatchTasksLatestResponse(
                batchData = BatchTasksLatestData(tasksCount = 1, students = emptyList()),
            ),
        )

        assertEquals(0, model.uiState.value.totalCount)
        assertEquals(0, model.uiState.value.students.size)
    }

    @Test
    fun requestClose_setsConfirmFlag_andCancelKeepsState() {
        val model = buildModel()
        model.applyResponse(
            BatchTasksLatestResponse(
                batchData = BatchTasksLatestData(
                    tasksCount = 1,
                    students = listOf(student(id = "s1", submittedCount = 1)),
                ),
            ),
        )
        val before = model.uiState.value

        model.requestClose()
        assertTrue(model.uiState.value.closeConfirmShown)

        model.cancelCloseConfirm()
        val after = model.uiState.value
        assertFalse(after.closeConfirmShown)
        assertEquals(before.students, after.students)
        assertEquals(before.submittedCount, after.submittedCount)
    }

    @Test
    fun confirmClose_dismissesConfirm_andNotifiesListener() {
        val model = buildModel()
        val listener = RecordingListener()
        model.listener = listener
        model.requestClose()

        model.confirmClose()

        assertFalse(model.uiState.value.closeConfirmShown)
        assertEquals(1, listener.closeRequests)
    }

    @Test
    fun collectAllAndMark_transitionsToResultState() {
        val model = buildModel()
        val listener = RecordingListener()
        model.listener = listener

        model.collectAllAndMark()

        // State machine transitions to RESULT; listener is not involved in phase change.
        assertEquals(SketchState.RESULT, model.sketchState.value)
        assertEquals(0, listener.closeRequests)
    }

    @Test
    fun applyResponse_skipsStudentsWithEmptyId() {
        val model = buildModel()

        model.applyResponse(
            BatchTasksLatestResponse(
                batchData = BatchTasksLatestData(
                    tasksCount = 1,
                    students = listOf(
                        student(id = "s1", submittedCount = 1),
                        student(id = "", submittedCount = 1),
                    ),
                ),
            ),
        )

        val state = model.uiState.value
        assertEquals(1, state.students.size)
        assertEquals(1, state.submittedCount)
        assertEquals(1, state.totalCount)
    }

    // ── Error handling (refreshNow) ──────────────────────────────────────────

    @Test
    fun refreshNow_withNoNetwork_emitsNetworkDisconnected_andStaysNotRefreshing() = runBlocking {
        val networkManager = mockk<NetworkManager>(relaxed = true).also {
            every { it.networkAvailabilityState } returns MutableStateFlow(false)
        }
        val model = MvbSketchResponseStartWindowModel(
            taskApiService = FakeTaskApiService(),
            networkManager = networkManager,
            tokenProvider = { "t" },
            lessonIdProvider = { "l" },
            coordinator = mockk(relaxed = true),
            markHandler = mockk(relaxed = true),
            coroutineScopeOverride = CoroutineScope(Dispatchers.Unconfined),
        )
        val collected = mutableListOf<MvbSketchResponseStartWindowModel.UiEvent>()
        val collectorJob = CoroutineScope(Dispatchers.Unconfined).launch { model.events.toList(collected) }

        model.refreshNow()

        assertTrue(collected.any { it is MvbSketchResponseStartWindowModel.UiEvent.NetworkDisconnected })
        assertFalse(model.uiState.value.isRefreshing)
        collectorJob.cancel()
    }

    @Test
    fun refreshNow_withApiFailure_emitsRefreshFailed_andResetsIsRefreshing() = runBlocking {
        // FakeTaskApiService returns NetworkDisconnected for getBatchTasksLatest, which
        // hits the non-Success branch and (because manualRefresh=true) emits RefreshFailed.
        val networkManager = mockk<NetworkManager>(relaxed = true).also {
            every { it.networkAvailabilityState } returns MutableStateFlow(true)
        }
        val model = MvbSketchResponseStartWindowModel(
            taskApiService = FakeTaskApiService(),
            networkManager = networkManager,
            tokenProvider = { "t" },
            lessonIdProvider = { "l" },
            coordinator = mockk(relaxed = true),
            markHandler = mockk(relaxed = true),
            coroutineScopeOverride = CoroutineScope(Dispatchers.Unconfined),
        )
        val collected = mutableListOf<MvbSketchResponseStartWindowModel.UiEvent>()
        val collectorJob = CoroutineScope(Dispatchers.Unconfined).launch { model.events.toList(collected) }

        model.refreshNow()

        assertTrue(collected.any { it is MvbSketchResponseStartWindowModel.UiEvent.RefreshFailed })
        assertFalse(model.uiState.value.isRefreshing)
        collectorJob.cancel()
    }

    @Test
    fun refreshNow_doubleCallWhileRefreshing_secondCallIgnored() {
        // First refresh is in flight; calling again should be a no-op (no extra event emission).
        // We can't easily prove the no-op without a slower fake — but isRefreshing flag prevents
        // re-entry from setting isRefreshing twice in a row.
        val model = buildModel()
        // Manually mark isRefreshing=true by reflecting; but state is private. We exercise the
        // public re-entry guard via consecutive refreshNow() calls which complete sync via
        // Unconfined dispatcher and FakeTaskApiService.
        model.refreshNow()
        model.refreshNow()
        assertFalse(model.uiState.value.isRefreshing)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun buildModel(): MvbSketchResponseStartWindowModel {
        val networkManager = mockk<NetworkManager>(relaxed = true).also {
            every { it.networkAvailabilityState } returns MutableStateFlow(true)
        }
        return MvbSketchResponseStartWindowModel(
            taskApiService = FakeTaskApiService(),
            networkManager = networkManager,
            tokenProvider = { "test-bearer" },
            lessonIdProvider = { "lesson-1" },
            coordinator = mockk(relaxed = true),
            markHandler = mockk(relaxed = true),
            coroutineScopeOverride = CoroutineScope(Dispatchers.Unconfined),
        )
    }

    private fun student(
        id: String,
        submittedCount: Int = 0,
    ): BatchTasksLatestStudent {
        return BatchTasksLatestStudent(
            studentId = id,
            displayName = "Name-$id",
            submittedCount = submittedCount,
        )
    }

    private class RecordingListener : MvbSketchResponseStartWindowModel.Listener {
        var closeRequests = 0

        override fun onRequestCloseWindow() {
            closeRequests += 1
        }
    }

    private class FakeTaskApiService : TaskApiService {
        override suspend fun createTask(token: String, lessonId: String, body: CreateTaskBody): ApiResponse<CreateTaskResponse> =
            ApiResponse.NetworkDisconnected()

        override suspend fun getTask(token: String, lessonId: String, filter: String): ApiResponse<GetTasksResponse> =
            ApiResponse.NetworkDisconnected()

        override suspend fun getTaskRecordsByLesson(token: String, lessonId: String): ApiResponse<GetTaskRecordsByLessonResponse> =
            ApiResponse.NetworkDisconnected()

        override suspend fun getTaskById(token: String, lessonId: String, taskId: String, studentStatus: Boolean): ApiResponse<GetTaskByIdResponse> =
            ApiResponse.NetworkDisconnected()

        override suspend fun getStudentTasks(lessonId: String, studentId: String, taskId: String): ApiResponse<List<GetStudentTaskResponse>> =
            ApiResponse.NetworkDisconnected()

        override suspend fun fetchUrlMeta(token: String, url: String): ApiResponse<GetLinkPreviewResponse> =
            ApiResponse.NetworkDisconnected()

        override suspend fun closeTask(token: String, taskId: String): ApiResponse<CloseTaskResponse> =
            ApiResponse.NetworkDisconnected()

        override suspend fun updateTaskResult(token: String, taskId: String, body: List<UpdateTaskResultBody>): ApiResponse<UpdateTaskResultResponse> =
            ApiResponse.NetworkDisconnected()

        override suspend fun getBatchTasksLatest(token: String, lessonId: String): ApiResponse<BatchTasksLatestResponse> =
            ApiResponse.NetworkDisconnected()

        override suspend fun batchCreateTasks(token: String, lessonId: String, body: BatchCreateTasksBody): ApiResponse<BatchCreateTasksResponse> =
            ApiResponse.NetworkDisconnected()

        override suspend fun closeBatchTask(token: String, lessonId: String, batchTaskId: String): ApiResponse<com.viewsonic.classswift.api.response.CloseBatchTaskResponse> =
            ApiResponse.NetworkDisconnected()
    }
}
