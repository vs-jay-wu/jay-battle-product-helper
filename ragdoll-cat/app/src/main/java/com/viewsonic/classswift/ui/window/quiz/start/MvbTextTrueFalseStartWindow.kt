package com.viewsonic.classswift.ui.window.quiz.start

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.clickable
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowMvbTextQuizBinding
import com.viewsonic.classswift.feature.servicescreens.ui.BarStyle
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizStartScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.feature.servicescreens.ui.QuizPanelState
import com.viewsonic.classswift.feature.servicescreens.ui.QuizResponder
import com.viewsonic.classswift.feature.servicescreens.ui.ResponderState
import com.viewsonic.classswift.feature.servicescreens.ui.ResultBar
import com.viewsonic.classswift.feature.servicescreens.ui.TextDiscloseOption
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.QuizManager.QuizzingUiState
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.window.adapter.MvbQuizResultAdapter
import com.viewsonic.classswift.ui.window.quiz.mvb.ComposeWindowHost
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.DateTimeUtils
import com.viewsonic.classswift.utils.TimeUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.isLatexContent
import com.viewsonic.classswift.utils.extension.startTimerInMilliSec
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import kotlin.math.roundToInt

/**
 * MvbTextTrueFalseStartWindow — CMP port (text variant). Shares [TrueFalseWindowModel] logic / socket
 * actions with [MvbTrueFalseStartWindow]; the panel is the Compose [MvbQuizStartScreen]
 * (type = TEXT_TRUE_FALSE) hosted in a hybrid shell ([WindowMvbTextQuizBinding]): a native FrameLayout
 * root holding the ComposeView plus a native [KatexView][com.viewsonic.classswift.ui.widget.KatexView]
 * sibling. Plain-text questions render in pure Compose; a LaTeX question is drawn by the native
 * KatexView overlaid on the Compose question box (the overlay window isn't hardware-accelerated, so a
 * WebView only paints as a direct native child — not nested in the ComposeView). The box is a fixed
 * 169dp, so the overlay is a fixed rect positioned from the Compose box's reported bounds.
 */
class MvbTextTrueFalseStartWindow(val context: Context) : IWindow<WindowMvbTextQuizBinding> {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val quizStartWindowModel: QuizStartWindowModel by inject(QuizStartWindowModel::class.java)
    private val trueFalseWindowModel: TrueFalseWindowModel by inject(TrueFalseWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val quizManager: QuizManager by inject(QuizManager::class.java)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val composeHost = ComposeWindowHost()
    private var quizCancelJob: Job? = null
    private var stopwatchJob: Job? = null
    private var hasTriggeredResultState: Boolean = false

    /** Disclose rows from the runtime quiz options (content + AI reason + suggested flag). */
    private val discloseOptions: List<TextDiscloseOption> by lazy {
        QuizSharedUiInfo.quizOptionList.map {
            TextDiscloseOption(content = it.content, reason = it.reason, isSuggested = it.isCorrectAnswer())
        }
    }
    private val discloseOptionIds: List<Int> by lazy { QuizSharedUiInfo.quizOptionList.map { it.optionId } }
    private val isQuestionLatex: Boolean by lazy { QuizSharedUiInfo.quizContent.isLatexContent() }

    // Last applied question-overlay rect (px, root space) — skip redundant layout churn.
    private var katexLeft = -1
    private var katexTop = -1
    private var katexWidth = -1
    private var katexHeight = -1

    override var tag: WindowTag = WindowTag.MVB_TEXT_TRUE_FALSE_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(869f.dpToPx().toInt(), 496f.dpToPx().toInt()) // 853×480 shell + 8dp shadow padding
    override val binding: WindowMvbTextQuizBinding = WindowMvbTextQuizBinding.inflate(LayoutInflater.from(context))
    override fun getCurrentSize(): SizeInPixels = size

    private data class Ui(
        val state: QuizPanelState = QuizPanelState.QUIZZING,
        val joined: Int = 0,
        val capacity: Int = 0,
        val stopwatch: String = "00:00",
        val responders: List<QuizResponder> = emptyList(),
        val resultBars: List<ResultBar> = emptyList(),
        val hasNetwork: Boolean = true,
        val closeConfirm: Boolean = false,
        val closeLoading: Boolean = false,
        val errorToast: String? = null,
    )
    private val ui = MutableStateFlow(Ui())

    override fun onCreate() {
        if (!QuizSharedUiInfo.isOngoing) quizStartWindowModel.changeQuizState(QuizState.QUIZZING)
        quizStartWindowModel.setStudentQuizzingList()
    }

    override fun onViewCreated() {
        composeHost.attach(binding.cvBody) { Content() }
        unclosedMissionUiManager.notifyMissionOngoingIfNeeded(MissionType.QUIZ)
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
        initCollection()
        setDiscloseErrorHandler()
        setRefreshErrorHandler()
        startStopwatch()
        val st = quizStartWindowModel.quizzingUiState.value
        if (st.quizState == QuizState.QUIZ_RESULTS) hasTriggeredResultState = true
        pushUi(st)
    }

    /** Overlay text via BasicText (app module has no material3). */
    @Composable
    private fun OT(text: String, color: Color, size: TextUnit, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier) {
        BasicText(text, modifier = modifier, style = TextStyle(color = color, fontSize = size, fontWeight = weight))
    }

    @Composable
    private fun Content() {
        val s by ui.collectAsState()
        Box(Modifier.fillMaxSize()) {
            MvbQuizStartScreen(
                type = MvbQuizType.TEXT_TRUE_FALSE,
                state = s.state,
                joined = s.joined,
                capacity = s.capacity,
                stopwatch = s.stopwatch,
                options = listOf(context.getString(R.string.text_true), context.getString(R.string.text_false)),
                responders = s.responders,
                resultBars = s.resultBars,
                textDiscloseOptions = discloseOptions,
                screenshot = { m -> QuestionContent(m) },
                onClose = { showCloseConfirmDialog() },
                onMinimize = {
                    csWindowManager.minimizeWindow(tag)
                    unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.QUIZ)
                },
                onEndAndReview = { endQuiz() },
                onPublishDisclose = { indices -> sendDiscloseAnswer(indices) },
            )
            if (!s.hasNetwork) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2E3133)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    OT(context.getString(R.string.mvb_network_disconnect_toast), Color.White, 12.sp)
                }
            }
            s.errorToast?.let { msg ->
                Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF02B2B)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OT(msg, Color.White, 12.sp)
                }
            }
            if (s.closeConfirm) CloseConfirmDialog()
            if (s.closeLoading) {
                Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                    AndroidView(factory = { ctx ->
                        LottieAnimationView(ctx).apply {
                            setAnimation("ani_loading.json")
                            repeatCount = LottieDrawable.INFINITE
                            playAnimation()
                        }
                    }, modifier = Modifier.width(80.dp))
                }
            }
        }
    }

    /**
     * The question region (slot). Plain text → pure Compose; LaTeX → a transparent hole whose bounds
     * drive the native [KatexView][binding.cskvQuestion] overlay (positioned in [positionQuestionKatex]).
     */
    @Composable
    private fun QuestionContent(modifier: Modifier) {
        val content = QuizSharedUiInfo.quizContent
        if (isQuestionLatex) {
            Box(modifier.onGloballyPositioned { positionQuestionKatex(it.positionInRoot(), it.size, content) })
        } else {
            OT(content, Color(0xFF333333), 10.67.sp, modifier = modifier)
        }
    }

    /** Place the native KatexView over the Compose question box (px, root space); idempotent. */
    private fun positionQuestionKatex(pos: Offset, sizePx: IntSize, text: String) {
        val left = pos.x.roundToInt()
        val top = pos.y.roundToInt()
        if (left == katexLeft && top == katexTop && sizePx.width == katexWidth && sizePx.height == katexHeight) return
        katexLeft = left; katexTop = top; katexWidth = sizePx.width; katexHeight = sizePx.height
        val kv = binding.cskvQuestion
        val lp = (kv.layoutParams as FrameLayout.LayoutParams)
        lp.width = sizePx.width
        lp.height = sizePx.height
        lp.leftMargin = left
        lp.topMargin = top
        kv.layoutParams = lp
        if (kv.visibility != View.VISIBLE) kv.visibility = View.VISIBLE
        kv.setText(text)
    }

    // ---- State pump: live quizzing UI → Compose Ui ----

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.quizzingUiState.collect { uiState ->
                maybeTriggerResultState(uiState)
                withContext(Dispatchers.Main) { pushUi(uiState) }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            quizCommonWindowModel.networkAvailabilityState.collect { hasNetwork ->
                withContext(Dispatchers.Main) {
                    ui.update { it.copy(hasNetwork = hasNetwork) }
                    if (!hasNetwork) ui.update { it.copy(closeConfirm = false) }
                }
            }
        }
    }

    private fun pushUi(uiState: QuizzingUiState) {
        val infos = uiState.studentQuizzingInfoList.map { student ->
            val answeringState = when {
                student.answerDataList.isNotEmpty() -> AnsweringState.ANSWERED
                student.status == StudentInfo.Status.ACTIVE -> AnsweringState.NOT_ANSWER
                else -> AnsweringState.ABSENT
            }
            QuizAnsweringInfo.fromStudentTrueFalseQuizzingInfo(student, answeringState, student.answerDataList)
        }
        val correctId = uiState.discloseAnswerData.firstOrNull()?.optionId
        val correctIds = if (correctId == null) emptyList() else listOf(correctId)
        val tCount = infos.count { it.answerOption.firstOrNull() == TrueFalseWindowModel.TRUE_OPTION_INDEX }
        val fCount = infos.count { it.answerOption.firstOrNull() == TrueFalseWindowModel.FALSE_OPTION_INDEX }
        val noAnswerCount = infos.count { it.answeringState == AnsweringState.NOT_ANSWER }
        val attendance = infos.count { it.answeringState != AnsweringState.ABSENT }
        val correctIsT = correctId == TrueFalseWindowModel.TRUE_OPTION_INDEX
        val correctIsF = correctId == TrueFalseWindowModel.FALSE_OPTION_INDEX

        val responders = infos.map { info ->
            QuizResponder(
                seat = info.displaySeatNumber,
                name = info.displayName,
                state = when (info.answeringState) {
                    AnsweringState.ANSWERED -> ResponderState.ANSWERED
                    AnsweringState.NOT_ANSWER -> ResponderState.NOT_SUBMITTED
                    else -> ResponderState.ABSENT
                },
                answer = answerLabel(info.answerOption.firstOrNull()),
                correct = if (correctIds.isEmpty()) null else MvbQuizResultAdapter.ViewHolder.studentChoseCorrect(info, correctIds),
            )
        }
        val bars = listOf(
            ResultBar(answerLabel(TrueFalseWindowModel.TRUE_OPTION_INDEX) ?: "T", tCount, attendance, correctIsT, if (correctIsT) BarStyle.CORRECT else BarStyle.INCORRECT),
            ResultBar(answerLabel(TrueFalseWindowModel.FALSE_OPTION_INDEX) ?: "F", fCount, attendance, correctIsF, if (correctIsF) BarStyle.CORRECT else BarStyle.INCORRECT),
            ResultBar(context.getString(R.string.quiz_mvb_cell_not_submitted), noAnswerCount, attendance, false, BarStyle.NEUTRAL),
        )
        ui.update {
            it.copy(
                state = when (uiState.quizState) {
                    QuizState.DISCLOSE_ANSWER -> QuizPanelState.DISCLOSE
                    QuizState.QUIZ_RESULTS -> QuizPanelState.RESULT
                    else -> QuizPanelState.QUIZZING
                },
                joined = uiState.answerCount,
                capacity = uiState.attendanceCount,
                responders = responders,
                resultBars = bars,
            )
        }
    }

    private fun answerLabel(optionId: Int?): String? = when (optionId) {
        TrueFalseWindowModel.TRUE_OPTION_INDEX -> context.getString(R.string.quiz_disclose_option_label_true)
        TrueFalseWindowModel.FALSE_OPTION_INDEX -> context.getString(R.string.quiz_disclose_option_label_false)
        else -> null
    }

    private fun maybeTriggerResultState(uiState: QuizzingUiState) {
        if (hasTriggeredResultState) return
        if (uiState.quizState != QuizState.DISCLOSE_ANSWER) return
        if (uiState.discloseAnswerData.isEmpty()) return
        hasTriggeredResultState = true
        trueFalseWindowModel.triggerQuizResultState()
    }

    // ---- Stopwatch ----

    private fun startStopwatch() {
        if (stopwatchJob?.isActive == true) return
        val startTimeInMillis = if (quizManager.quizStartTimeInMillis > 0) quizManager.quizStartTimeInMillis else System.currentTimeMillis()
        val timeDiffInMillis = TimeUtils.getTimeDiffFromCurrentTimeInMillis(startTimeInMillis)
        stopwatchJob = coroutineScope.startTimerInMilliSec(startTimeInMillis, timeDiffInMillis) { tickMs ->
            val (minutes, seconds) = DateTimeUtils.formatToMinuteSecondPair(tickMs)
            withContext(Dispatchers.Main) { ui.update { it.copy(stopwatch = String.format("%02d:%02d", minutes, seconds)) } }
        }
    }

    // ---- Error handlers ----

    private fun setDiscloseErrorHandler() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.discloseAnswerErrorFlow.collectLatest {
                withContext(Dispatchers.Main) { showErrorToast(context.getString(R.string.common_error_general)) }
            }
        }
    }

    private fun setRefreshErrorHandler() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.refreshFailedFlow.collectLatest {
                withContext(Dispatchers.Main) {
                    if (ui.value.hasNetwork) showErrorToast(context.getString(R.string.common_error_general))
                }
            }
        }
    }

    private fun showErrorToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            ui.update { it.copy(errorToast = message) }
            withContext(Dispatchers.IO) { kotlinx.coroutines.delay(3000) }
            ui.update { it.copy(errorToast = null) }
        }
    }

    // ---- Actions (same socket logic as MvbTrueFalseStartWindow) ----

    private fun onCloseConfirmPositive() {
        ui.update { it.copy(closeConfirm = false) }
        if (!ui.value.hasNetwork) {
            quizManager.clearCurrentQuizInfo()
            csWindowManager.removeWindow(tag)
            unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
        } else if (quizStartWindowModel.quizzingUiState.value.quizState == QuizState.QUIZ_RESULTS) {
            closeQuiz()
        } else {
            ui.update { it.copy(closeLoading = true) }
            cancelQuiz()
        }
    }

    private fun onCloseConfirmNegative() {
        ui.update { it.copy(closeConfirm = false) }
        quizCancelJob?.cancel()
    }

    private fun showCloseConfirmDialog() {
        ui.update { it.copy(closeConfirm = true) }
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

    private fun closeQuiz() {
        quizCancelJob?.cancel()
        quizCancelJob = coroutineScope.launch(Dispatchers.Main) {
            ui.update { it.copy(closeLoading = true) }
            withContext(Dispatchers.IO) {
                val success = quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.CLOSE)
                if (success) {
                    withContext(Dispatchers.Main) {
                        csWindowManager.removeWindow(tag)
                        unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        ui.update { it.copy(closeLoading = false) }
                        showErrorToast(context.getString(R.string.quiz_error_msg_close_quiz))
                    }
                }
            }
        }
    }

    private fun cancelQuiz() {
        quizCancelJob?.cancel()
        quizCancelJob = coroutineScope.launch(Dispatchers.IO) {
            if (quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.CANCEL)) {
                withContext(Dispatchers.Main) {
                    csWindowManager.removeWindow(tag)
                    unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                }
            } else {
                withContext(Dispatchers.Main) {
                    ui.update { it.copy(closeLoading = false) }
                    showErrorToast(context.getString(R.string.quiz_error_msg_close_quiz))
                }
            }
        }
    }

    private fun endQuiz() {
        coroutineScope.launch(Dispatchers.IO) {
            if (quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.FINISH)) {
                quizStartWindowModel.changeQuizState(QuizState.DISCLOSE_ANSWER)
            } else {
                withContext(Dispatchers.Main) { showErrorToast(context.getString(R.string.quiz_error_msg_close_quiz)) }
            }
        }
    }

    /** [indices] are disclose-row indices; map to the runtime option ids. Single-select for TF. */
    private fun sendDiscloseAnswer(indices: List<Int>) {
        val ids = indices.mapNotNull { discloseOptionIds.getOrNull(it) }
        quizStartWindowModel.discloseAnswer(ids)
    }

    override fun onDestroy() {
        composeHost.destroy()
        binding.cskvQuestion.release()
        stopwatchJob?.cancel()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        quizCommonWindowModel.onCleared()
        quizStartWindowModel.onCleared()
        coroutineScope.cancel()
    }
}
