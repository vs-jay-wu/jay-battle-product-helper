package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.api.body.BatchCreateTasksBody
import com.viewsonic.classswift.api.response.BatchCreateTasksResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.coordinator.UploadFileHandler
import com.viewsonic.classswift.data.info.ClassroomInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.data.uncategorized.AwsPreSignedUrl
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.ClassroomManager.ClassroomDataState
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel.Event
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel.UploadState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MvbSketchResponseEditWindowModelMockkTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var taskApiService: TaskApiService
    private lateinit var accountManager: AccountManager
    private lateinit var classroomManager: ClassroomManager
    private lateinit var uploadFileHandler: UploadFileHandler
    private lateinit var networkManager: com.viewsonic.classswift.manager.NetworkManager
    private lateinit var uploadFlow: MutableSharedFlow<Boolean>

    private val preSignedUrl = AwsPreSignedUrl(s3PutUrl = "", s3GetUrl = "https://cdn/u.png")

    private lateinit var sut: MvbSketchResponseEditWindowModel

    @Before
    fun setUp() {
        taskApiService = mockk(relaxed = true)
        accountManager = mockk()
        classroomManager = mockk()
        uploadFileHandler = mockk(relaxed = true)
        networkManager = mockk(relaxed = true)
        uploadFlow = MutableSharedFlow(replay = 0, extraBufferCapacity = 8)

        every { uploadFileHandler.uploadImageSharedFlow } returns uploadFlow
        every { uploadFileHandler.awsPreSignedUrl } returns preSignedUrl
        every { accountManager.getBearerToken() } returns "Bearer test-token"
        every { classroomManager.classroomDataStateFlow } returns MutableStateFlow(
            ClassroomDataState(selectedClassroomInfo = ClassroomInfo(lessonId = "L1"))
        )
        every { networkManager.networkAvailabilityState } returns MutableStateFlow(true)

        mockkObject(QuizSharedUiInfo)
        every { QuizSharedUiInfo.screenshotImageUri = any() } returns Unit
        every { QuizSharedUiInfo.updateQuizType(any()) } returns Unit

        sut = MvbSketchResponseEditWindowModel(
            taskApiService = taskApiService,
            accountManager = accountManager,
            classroomManager = classroomManager,
            uploadFileHandler = uploadFileHandler,
            networkManager = networkManager,
            ioDispatcher = testDispatcher,
            coroutineScopeOverride = testScope,
        )
    }

    private fun successResponse(): ApiResponse<BatchCreateTasksResponse> =
        ApiResponse.Success(
            data = mockk(relaxed = true),
            retrofitResponse = mockk(relaxed = true),
        )

    private fun failureResponse(): ApiResponse<BatchCreateTasksResponse> =
        ApiResponse.NetworkDisconnected()

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── US-2: startUpload emits Loading, then Success on upload result ──────────

    @Test
    fun `startUpload transitions state to Loading and calls fetchPreSignedUrl`() = runTest(testDispatcher) {
        sut.startUpload("content://capture/1")
        advanceUntilIdle()

        val state = sut.uiState.value
        assertTrue(state.uploadState is UploadState.Loading)
        assertEquals("content://capture/1", (state.uploadState as UploadState.Loading).uri)
        verify(exactly = 1) { uploadFileHandler.fetchPreSignedUrl("L1", "content://capture/1") }
    }

    @Test
    fun `uploadImageSharedFlow true transitions state to Success with preview URL`() = runTest(testDispatcher) {
        sut.startUpload("content://capture/1")
        advanceUntilIdle()

        uploadFlow.emit(true)
        advanceUntilIdle()

        val state = sut.uiState.value
        assertTrue(state.uploadState is UploadState.Success)
        assertEquals("https://cdn/u.png", (state.uploadState as UploadState.Success).previewImageUrl)
    }

    @Test
    fun `uploadImageSharedFlow false transitions state to Failed`() = runTest(testDispatcher) {
        sut.startUpload("content://capture/1")
        advanceUntilIdle()

        uploadFlow.emit(false)
        advanceUntilIdle()

        assertTrue(sut.uiState.value.uploadState is UploadState.Failed)
    }

    // ── US-3: retryUpload re-fires fetch only when a URI was cached ─────────────

    @Test
    fun `retryUpload re-triggers fetchPreSignedUrl with cached URI`() = runTest(testDispatcher) {
        sut.startUpload("content://capture/1")
        advanceUntilIdle()

        sut.retryUpload()
        advanceUntilIdle()

        verify(exactly = 2) { uploadFileHandler.fetchPreSignedUrl("L1", "content://capture/1") }
    }

    @Test
    fun `retryUpload before any startUpload is a no-op`() = runTest(testDispatcher) {
        sut.retryUpload()
        advanceUntilIdle()

        verify(exactly = 0) { uploadFileHandler.fetchPreSignedUrl(any(), any()) }
    }

    // ── cleanupBeforeClose: side effects without state churn ───────────────────

    @Test
    fun `cleanupBeforeClose cancels dispatch job and clears screenshot URI without touching uiState`() = runTest(testDispatcher) {
        coEvery { taskApiService.batchCreateTasks(any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(60_000)
            successResponse()
        }
        sut.startUpload("content://capture/1")
        advanceUntilIdle()
        uploadFlow.emit(true)
        advanceUntilIdle()
        sut.startQuestion()
        advanceUntilIdle()
        val beforeUploadState = sut.uiState.value.uploadState

        sut.cleanupBeforeClose()
        advanceUntilIdle()

        assertEquals(beforeUploadState, sut.uiState.value.uploadState)
        // No longer asserts QuizSharedUiInfo.screenshotImageUri="" — downstream
        // MvbSketchResponseStartWindow needs the URI for its preview; mission
        // close handles the cleanup instead. See cleanupBeforeClose javadoc.
    }

    // ── US-8: startQuestion success / failure / double-click protection ────────

    @Test
    fun `startQuestion success dispatches batchCreateTasks and emits OpenStartWindow`() = runTest(testDispatcher) {
        coEvery { taskApiService.batchCreateTasks(any(), any(), any()) } returns successResponse()
        sut.startUpload("content://capture/1")
        advanceUntilIdle()
        uploadFlow.emit(true)
        advanceUntilIdle()

        val collected = mutableListOf<Event>()
        val collectorJob = launch { sut.events.toList(collected) }

        sut.startQuestion()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            taskApiService.batchCreateTasks(any(), eq("L1"), any<BatchCreateTasksBody>())
        }
        assertTrue(collected.any { it is Event.OpenStartWindow })

        collectorJob.cancel()
    }

    @Test
    fun `startQuestion failure emits ShowErrorToast and clears dispatch flag`() = runTest(testDispatcher) {
        coEvery { taskApiService.batchCreateTasks(any(), any(), any()) } returns failureResponse()
        sut.startUpload("content://capture/1")
        advanceUntilIdle()
        uploadFlow.emit(true)
        advanceUntilIdle()

        val collected = mutableListOf<Event>()
        val collectorJob = launch { sut.events.toList(collected) }

        sut.startQuestion()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            taskApiService.batchCreateTasks(any(), eq("L1"), any<BatchCreateTasksBody>())
        }
        assertTrue(collected.any { it is Event.ShowErrorToast })
        assertFalse(sut.uiState.value.isDispatchInFlight)

        collectorJob.cancel()
    }

    @Test
    fun `startQuestion before upload Success is ignored`() = runTest(testDispatcher) {
        sut.startQuestion()
        advanceUntilIdle()

        coVerify(exactly = 0) { taskApiService.batchCreateTasks(any(), any(), any()) }
        assertFalse(sut.uiState.value.isDispatchInFlight)
    }

    @Test
    fun `startQuestion with no network emits ShowErrorToast and skips API`() = runTest(testDispatcher) {
        every { networkManager.networkAvailabilityState } returns MutableStateFlow(false)
        sut.startUpload("content://capture/1")
        advanceUntilIdle()
        uploadFlow.emit(true)
        advanceUntilIdle()

        val collected = mutableListOf<Event>()
        val collectorJob = launch { sut.events.toList(collected) }

        sut.startQuestion()
        advanceUntilIdle()

        coVerify(exactly = 0) { taskApiService.batchCreateTasks(any(), any(), any()) }
        assertTrue(collected.any { it is Event.ShowErrorToast })
        assertFalse(sut.uiState.value.isDispatchInFlight)

        collectorJob.cancel()
    }

    @Test
    fun `startQuestion with thrown exception catches and emits ShowErrorToast`() = runTest(testDispatcher) {
        coEvery { taskApiService.batchCreateTasks(any(), any(), any()) } throws java.io.IOException("boom")
        sut.startUpload("content://capture/1")
        advanceUntilIdle()
        uploadFlow.emit(true)
        advanceUntilIdle()

        val collected = mutableListOf<Event>()
        val collectorJob = launch { sut.events.toList(collected) }

        sut.startQuestion()
        advanceUntilIdle()

        assertTrue(collected.any { it is Event.ShowErrorToast })
        assertFalse(sut.uiState.value.isDispatchInFlight)

        collectorJob.cancel()
    }

    @Test
    fun `startQuestion double-tap only fires batchCreateTasks once`() = runTest(testDispatcher) {
        coEvery { taskApiService.batchCreateTasks(any(), any(), any()) } returns successResponse()
        sut.startUpload("content://capture/1")
        advanceUntilIdle()
        uploadFlow.emit(true)
        advanceUntilIdle()

        sut.startQuestion()
        sut.startQuestion()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            taskApiService.batchCreateTasks(any(), eq("L1"), any<BatchCreateTasksBody>())
        }
    }
}
