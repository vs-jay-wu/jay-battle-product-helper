package com.viewsonic.classswift.ui.window.quiz.start

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.enum.SketchAnswerStatus
import com.viewsonic.classswift.data.info.SketchStudentCardInfo
import com.viewsonic.classswift.data.quiz.SketchMarkUpdateState
import com.viewsonic.classswift.data.quiz.SketchResultState
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.databinding.WindowMvbSketchQuizBinding
import com.viewsonic.classswift.feature.servicescreens.ui.MvbSketchResponseScreen
import com.viewsonic.classswift.feature.servicescreens.ui.QuizResponder
import com.viewsonic.classswift.feature.servicescreens.ui.ResponderState
import com.viewsonic.classswift.feature.servicescreens.ui.SketchCardStatus
import com.viewsonic.classswift.feature.servicescreens.ui.SketchPanelState
import com.viewsonic.classswift.feature.servicescreens.ui.SketchResponder
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.quiz.result.SketchReviewWidget
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus
import com.viewsonic.classswift.ui.window.quiz.mvb.ComposeWindowHost
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.SketchState
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

/**
 * MvbSketchResponseStartWindow — CMP port (hybrid). Same [MvbSketchResponseStartWindowModel] logic /
 * coordinator flow as before; the two panels are the Compose [MvbSketchResponseScreen] hosted in
 * [WindowMvbSketchQuizBinding]'s ComposeView, while the marking review ([SketchReviewWidget], with its
 * PaintView stroke + bitmap pipeline) stays native and is overlaid on a student-card click. Question
 * images (answering screenshot / result preview) load via Coil into the screen's slots.
 */
class MvbSketchResponseStartWindow(val context: Context) : IWindow<WindowMvbSketchQuizBinding> {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val windowModel: MvbSketchResponseStartWindowModel by inject(MvbSketchResponseStartWindowModel::class.java)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val composeHost = ComposeWindowHost()

    /** Result records by studentId, so a clicked [SketchResponder] maps back to its [TaskResultInfo]. */
    private var recordsById: Map<String, TaskResultInfo> = emptyMap()

    private data class Ui(
        val panel: SketchPanelState = SketchPanelState.ANSWERING,
        val inProgressLabel: String = "",
        val stopwatch: String = "00:00",
        val responders: List<QuizResponder> = emptyList(),
        val submitted: Int = 0,
        val notSubmitted: Int = 0,
        val sketchResponders: List<SketchResponder> = emptyList(),
        val previewUrl: String = "",
        val closeConfirm: Boolean = false,
        val errorToast: String? = null,
    )
    private val ui = MutableStateFlow(Ui())

    override var tag: WindowTag = WindowTag.MVB_SKETCH_RESPONSE_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(WRAP, WRAP)
    private val answeringSize = SizeInPixels(869f.dpToPx().toInt(), 496f.dpToPx().toInt())
    private val resultSize = SizeInPixels(869f.dpToPx().toInt(), 538.67f.dpToPx().toInt())
    override fun getCurrentSize(): SizeInPixels =
        if (ui.value.panel == SketchPanelState.RESULT) resultSize else answeringSize

    private val screenUri: String get() = quizCommonWindowModel.getScreenImageUri()

    override fun onViewCreated() {
        composeHost.attach(binding.cvBody) { Content() }
        unclosedMissionUiManager.notifyMissionOngoingIfNeeded(MissionType.QUIZ)
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
        windowModel.listener = object : MvbSketchResponseStartWindowModel.Listener {
            override fun onRequestCloseWindow() {
                csWindowManager.removeWindow(tag)
                unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
            }
        }
        initReviewWidget()
        observeAnswering()
        observeSketchState()
        observeResult()
        observeMarkUpdate()
        windowModel.start()
    }

    @Composable
    private fun OT(text: String, color: Color, size: TextUnit, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier) {
        BasicText(text, modifier = modifier, style = TextStyle(color = color, fontSize = size, fontWeight = weight))
    }

    @Composable
    private fun Content() {
        val s by ui.collectAsState()
        Box(Modifier.fillMaxSize()) {
            MvbSketchResponseScreen(
                state = s.panel,
                inProgressLabel = s.inProgressLabel,
                questionTitle = context.getString(R.string.mvb_sketch_response_result_title_prefix, 1),
                stopwatch = s.stopwatch,
                submitted = s.submitted,
                notSubmitted = s.notSubmitted,
                responders = s.responders,
                sketchResponders = s.sketchResponders,
                questionScreenshot = { m -> RemoteImage(screenUri, m) },
                questionPreview = { m -> RemoteImage(s.previewUrl, m) },
                onCardClick = { r -> onCardClicked(r) },
                onClose = { onCloseClicked() },
                onMinimize = {
                    csWindowManager.minimizeWindow(tag)
                    unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.QUIZ)
                },
                onRefresh = { onRefresh() },
                onCollectAndMark = { windowModel.collectAllAndMark() },
            )
            s.errorToast?.let { msg ->
                Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF02B2B)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OT(msg, Color.White, 12.sp)
                }
            }
            if (s.closeConfirm) CloseConfirmDialog()
        }
    }

    /** Coil-loaded image (local screenshot file:// or remote preview URL); empty frame when blank. */
    @Composable
    private fun RemoteImage(url: String, modifier: Modifier) {
        if (url.isNotBlank()) AsyncImage(model = url, contentDescription = null, modifier = modifier)
    }

    @Composable
    private fun CloseConfirmDialog() {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
            Column(Modifier.width(360.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(24.dp)) {
                OT(context.getString(R.string.quiz_disclose_close_confirm_title), Color(0xFF2E3133), 18.sp, FontWeight.Bold)
                OT(context.getString(R.string.quiz_disclose_close_confirm_body), Color(0xFF2E3133), 14.sp, modifier = Modifier.padding(top = 12.dp))
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    OT(
                        context.getString(R.string.quiz_disclose_close_confirm_negative), Color(0xFF5C6266), 14.sp, FontWeight.Medium,
                        modifier = Modifier.clickable { onCloseConfirmNegative() }.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    OT(
                        context.getString(R.string.quiz_disclose_close_confirm_positive), Color(0xFFF02B2B), 14.sp, FontWeight.Bold,
                        modifier = Modifier.clickable { onCloseConfirmPositive() }.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    // ─── Observers ──────────────────────────────────────────────────────────────

    private fun observeAnswering() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.uiState.collect { st ->
                ui.update {
                    it.copy(
                        inProgressLabel = context.resources.getQuantityString(
                            R.plurals.mvb_sketch_response_in_progress,
                            st.taskInfo.sketchCount.coerceAtLeast(0),
                            st.taskInfo.sketchCount.coerceAtLeast(0),
                        ),
                        stopwatch = formatTimer(st.elapsedSeconds),
                        responders = st.students.map(::toResponder),
                        closeConfirm = st.closeConfirmShown,
                    )
                }
            }
        }
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.events.collect { event ->
                val msg = when (event) {
                    MvbSketchResponseStartWindowModel.UiEvent.NetworkDisconnected -> context.getString(R.string.mvb_network_disconnect_toast)
                    MvbSketchResponseStartWindowModel.UiEvent.RefreshFailed -> context.getString(R.string.mvb_sketch_response_refresh_failed_toast)
                }
                showErrorToast(msg)
            }
        }
    }

    private fun observeSketchState() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.sketchState.collect { state ->
                val panel = if (state == SketchState.RESULT) SketchPanelState.RESULT else SketchPanelState.ANSWERING
                ui.update { it.copy(panel = panel) }
                if (state == SketchState.RESULT) {
                    resizeWindow()
                    transitionToResult()
                }
            }
        }
    }

    private fun observeResult() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.sketchResultFlow.collect { state -> bindResult(state) }
        }
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.resultEventFlow.collect { showErrorToast(context.getString(R.string.mvb_sketch_response_error_load_failed)) }
        }
    }

    private fun observeMarkUpdate() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.markUpdateResultFlow.collect { state ->
                try {
                    handleMarkUpdate(state)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Sketch handleMarkUpdate error")
                }
            }
        }
    }

    private suspend fun handleMarkUpdate(state: SketchMarkUpdateState) {
        when (state) {
            is SketchMarkUpdateState.Success -> {
                state.success?.let { windowModel.applyMarkSuccess(it) }
                if (binding.srwSketchReview.isShownOnScreen()) {
                    when {
                        state.success != null -> binding.srwSketchReview.onMarkUpdateSuccess(state.success)
                        state.failed != null -> binding.srwSketchReview.onMarkUpdateResultError(state.failed)
                        else -> binding.srwSketchReview.onMarkUnknownError()
                    }
                }
            }
            is SketchMarkUpdateState.Failed -> {
                Timber.w("Sketch mark failed: ${state.reason}")
                if (binding.srwSketchReview.isShownOnScreen()) binding.srwSketchReview.onMarkUnknownError()
            }
            is SketchMarkUpdateState.Idle -> Unit
        }
    }

    private fun transitionToResult() {
        val taskInfo = windowModel.uiState.value.taskInfo
        val batchTaskId = taskInfo.taskId.takeIf { it.isNotBlank() }
        val taskId = taskInfo.taskIds.firstOrNull()
        if (taskId == null) {
            Timber.w("[SketchResponse] transitionToResult: taskId unavailable")
            showErrorToast(context.getString(R.string.mvb_sketch_response_error_load_failed))
            return
        }
        windowModel.setBatchTaskId(batchTaskId)
        coroutineScope.launch { windowModel.loadResult(taskId) }
    }

    private fun bindResult(state: SketchResultState) {
        recordsById = state.recordList.associateBy { it.studentId }
        val submitted = state.recordList.count {
            it is TaskResultInfo.Content && (it.triggerType == SubmitStatus.RESPONSE || it.triggerType == SubmitStatus.GRADED)
        }
        val notSubmitted = state.recordList.count {
            it is TaskResultInfo.Content && it.triggerType == SubmitStatus.UNSUBMITTED
        }
        ui.update {
            it.copy(
                submitted = submitted,
                notSubmitted = notSubmitted,
                previewUrl = state.taskImgUrl,
                sketchResponders = state.recordList.map(::toSketchResponder),
            )
        }
    }

    // ─── Review widget (native overlay) ───────────────────────────────────────────

    private fun initReviewWidget() {
        binding.srwSketchReview.setEventListener(
            object : SketchReviewWidget.SketchReviewWidgetEventListener {
                override fun onSendMarkedResult(data: TaskResultInfo.Content) {
                    coroutineScope.launch { windowModel.saveAndHandBack(data) }
                }

                override fun onClose() {
                    binding.srwSketchReview.dismiss()
                }

                override fun onRefetchStudentRecord(data: TaskResultInfo) {
                    windowModel.getStudentTaskResult(data.studentId)
                }
            },
        )
    }

    private fun onCardClicked(responder: SketchResponder) {
        val record = recordsById[responder.id] ?: return
        binding.srwSketchReview.setRecord(record)
        binding.srwSketchReview.show()
    }

    // ─── Actions ──────────────────────────────────────────────────────────────────

    private fun onRefresh() {
        if (ui.value.panel == SketchPanelState.RESULT) {
            coroutineScope.launch { windowModel.refreshResult() }
        } else {
            windowModel.refreshNow()
        }
    }

    private fun onCloseClicked() {
        if (ui.value.panel == SketchPanelState.RESULT) {
            ui.update { it.copy(closeConfirm = true) }
        } else {
            windowModel.requestClose()
        }
    }

    private fun onCloseConfirmNegative() {
        ui.update { it.copy(closeConfirm = false) }
        windowModel.cancelCloseConfirm()
    }

    private fun onCloseConfirmPositive() {
        ui.update { it.copy(closeConfirm = false) }
        if (ui.value.panel == SketchPanelState.RESULT) {
            coroutineScope.launch {
                val success = windowModel.closeBatch()
                withContext(Dispatchers.Main) {
                    if (success) {
                        csWindowManager.removeWindow(tag)
                        unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                    } else {
                        showErrorToast(context.getString(R.string.mvb_sketch_response_error_close_batch_failed))
                    }
                }
            }
        } else {
            windowModel.confirmClose()
        }
    }

    private fun showErrorToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            ui.update { it.copy(errorToast = message) }
            withContext(Dispatchers.IO) { kotlinx.coroutines.delay(3000) }
            ui.update { it.copy(errorToast = null) }
        }
    }

    /** ANSWERING(480) → RESULT(522.67) grows the window; re-measure once the ComposeView re-lays-out. */
    private fun resizeWindow() {
        binding.root.post {
            csWindowManager.getWindow(tag)?.let { it.updateLayoutParam(it.getLayoutParam()) }
        }
    }

    // ─── Mappers ────────────────────────────────────────────────────────────────

    private fun toResponder(info: SketchStudentCardInfo): QuizResponder = QuizResponder(
        seat = info.displaySeatNumber,
        name = info.displayName,
        state = when (info.status) {
            SketchAnswerStatus.SUBMITTED -> ResponderState.ANSWERED
            SketchAnswerStatus.NOT_SUBMITTED -> ResponderState.NOT_SUBMITTED
            SketchAnswerStatus.ABSENT -> ResponderState.ABSENT
        },
    )

    private fun toSketchResponder(record: TaskResultInfo): SketchResponder = SketchResponder(
        seat = record.seatNumber,
        name = record.displayName,
        status = when (record) {
            is TaskResultInfo.Guest -> SketchCardStatus.ABSENT
            is TaskResultInfo.Content -> when (record.triggerType) {
                SubmitStatus.RESPONSE -> SketchCardStatus.CLICK_TO_VIEW
                SubmitStatus.GRADED -> SketchCardStatus.HANDED_BACK
                else -> SketchCardStatus.NOT_SUBMITTED
            }
            else -> SketchCardStatus.NOT_SUBMITTED
        },
        id = record.studentId,
    )

    private fun formatTimer(elapsedSeconds: Int): String {
        val safe = elapsedSeconds.coerceAtLeast(0)
        return context.getString(R.string.mvb_sketch_response_timer_format, safe / 60, safe % 60)
    }

    override fun onDestroy() {
        Timber.d("[onDestroy] MvbSketchResponseStartWindow")
        composeHost.destroy()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        windowModel.listener = null
        windowModel.onCleared()
        binding.srwSketchReview.release()
        coroutineScope.cancel()
    }

    // SketchReviewWidget (MaterialCardView etc.) needs a MaterialComponents theme to inflate.
    override val binding: WindowMvbSketchQuizBinding = WindowMvbSketchQuizBinding.inflate(
        LayoutInflater.from(androidx.appcompat.view.ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents)),
    )

    private companion object {
        val WRAP = android.view.WindowManager.LayoutParams.WRAP_CONTENT
    }
}
