package com.viewsonic.classswift.ui.window.quiz.start

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.feature.servicescreens.ui.BarStyle
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizStartScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.feature.servicescreens.ui.QuizPanelState
import com.viewsonic.classswift.feature.servicescreens.ui.QuizResponder
import com.viewsonic.classswift.feature.servicescreens.ui.ResponderState
import com.viewsonic.classswift.feature.servicescreens.ui.ResultBar
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.windowmodel.quiz.AudioStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.AudioState
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.DateTimeUtils
import com.viewsonic.classswift.utils.TimeUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.milliSecondToTimerUnit
import com.viewsonic.classswift.utils.extension.startTimerInMilliSec
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
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

/**
 * MvbAudioQuizStartWindow — CMP port (same WindowModel logic / socket actions; view is Compose via
 * [ComposeHostWindow]). Audio specifics: NO disclose (End-and-review → results directly), NO option
 * chips, submission overview with a check-icon Submitted chip ([MvbQuizStartScreen]'s audioMode); in
 * result an answered cell shows a play/pause control + time (driven by [AudioStartWindowModel]).
 */
class MvbAudioQuizStartWindow(val context: Context) : ComposeHostWindow(context) {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val quizStartWindowModel: QuizStartWindowModel by inject(QuizStartWindowModel::class.java)
    private val audioStartWindowModel: AudioStartWindowModel by inject(AudioStartWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val quizManager: QuizManager by inject(QuizManager::class.java)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var quizCancelJob: Job? = null
    private var stopwatchJob: Job? = null
    private var hasTriggeredResultState = false
    private var currentAudioInfos: List<AudioAnswerInfo> = emptyList()
    private val durationFetching = mutableSetOf<Int>()

    override var tag: WindowTag = WindowTag.MVB_AUDIO_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(869f.dpToPx().toInt(), 496f.dpToPx().toInt()) // 853×480 shell + 8dp shadow padding
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
        super.onViewCreated()
        unclosedMissionUiManager.notifyMissionOngoingIfNeeded(MissionType.QUIZ)
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
        initCollection()
        setRefreshErrorHandler()
        // Match legacy: stopwatch only while quizzing (skip when reopening into result).
        if (quizStartWindowModel.quizzingUiState.value.quizState == QuizState.QUIZZING) startStopwatch()
    }

    @Composable
    private fun OT(text: String, color: Color, size: TextUnit, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier) {
        BasicText(text, modifier = modifier, style = TextStyle(color = color, fontSize = size, fontWeight = weight))
    }

    @Composable
    override fun Content() {
        val s by ui.collectAsState()
        Box(Modifier.fillMaxSize()) {
            MvbQuizStartScreen(
                type = MvbQuizType.AUDIO,
                state = s.state,
                joined = s.joined,
                capacity = s.capacity,
                stopwatch = s.stopwatch,
                options = emptyList(),
                audioMode = true,
                responders = s.responders,
                resultBars = s.resultBars,
                screenshot = { m -> Screenshot(m) },
                onClose = { showCloseConfirmDialog() },
                onMinimize = {
                    csWindowManager.minimizeWindow(tag)
                    unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.QUIZ)
                },
                onEndAndReview = { endQuiz() },
                onAudioToggle = { r -> onAudioToggle(r) },
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

    @Composable
    private fun Screenshot(modifier: Modifier) {
        val path = quizCommonWindowModel.getScreenImageUri()
        val bitmap by produceState<ImageBitmap?>(null, path) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val file = path.removePrefix("file://")
                    if (file.isBlank()) null else BitmapFactory.decodeFile(file)?.asImageBitmap()
                }.getOrNull()
            }
        }
        bitmap?.let { Image(it, contentDescription = null, modifier = modifier) }
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

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.quizzingUiState.collect { uiState ->
                withContext(Dispatchers.Main) {
                    val infos = audioStartWindowModel.getCurrentStudentQuizAudioAnsweringInfoList()
                    if (uiState.quizState == QuizState.QUIZ_RESULTS && !hasTriggeredResultState) {
                        hasTriggeredResultState = true
                        val resultList = audioStartWindowModel.setQuizResult()
                        render(resultList)
                        val submittedIds = resultList.filter { it.answerResultState == AnswerResultState.ANSWERED }.map { it.studentId }
                        coroutineScope.launch(Dispatchers.IO) { quizStartWindowModel.updateStudentsPoint(submittedIds, 1) }
                    } else {
                        render(infos)
                    }
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            audioStartWindowModel.audioInfosSharedFlow.collect { infos ->
                withContext(Dispatchers.Main) { render(infos) }
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

    private fun render(infos: List<AudioAnswerInfo>) {
        currentAudioInfos = infos
        val state = quizStartWindowModel.quizzingUiState.value.quizState
        val uiState = quizStartWindowModel.quizzingUiState.value
        val submitted = infos.count { it.answerResultState == AnswerResultState.ANSWERED }
        val notSubmitted = infos.count { it.answerResultState == AnswerResultState.NO_ANSWER }
        val attendance = submitted + notSubmitted
        val responders = infos.map { toResponder(it) }
        val bars = listOf(
            ResultBar(context.getString(R.string.quiz_mvb_cell_submitted), submitted, attendance, isCorrect = false, BarStyle.CORRECT),
            ResultBar(context.getString(R.string.quiz_mvb_cell_not_submitted), notSubmitted, attendance, isCorrect = false, BarStyle.NEUTRAL),
        )
        ui.update {
            it.copy(
                state = if (state == QuizState.QUIZ_RESULTS) QuizPanelState.RESULT else QuizPanelState.QUIZZING,
                joined = uiState.answerCount,
                capacity = uiState.attendanceCount,
                responders = responders,
                resultBars = bars,
            )
        }
        // Pre-fetch durations for revealed recordings so the cell shows the time before play
        // (the legacy ViewHolder did this per-card via MediaMetadataRetriever).
        infos.filter { it.answerResultState == AnswerResultState.ANSWERED && it.canShowAnswer && it.audioDuration == null && it.audioUrl.isNotBlank() }
            .forEach { fetchDuration(it) }
    }

    private fun toResponder(info: AudioAnswerInfo): QuizResponder = QuizResponder(
        seat = info.displaySeatNumber,
        name = info.displayName,
        state = when (info.answerResultState) {
            AnswerResultState.ANSWERED -> ResponderState.ANSWERED
            AnswerResultState.NO_ANSWER -> ResponderState.NOT_SUBMITTED
            else -> ResponderState.ABSENT
        },
        correct = if (info.answerResultState == AnswerResultState.ANSWERED) true else null,
        audioPlaying = info.audioState == AudioState.PLAY,
        audioLoading = info.canShowAnswer && info.audioDuration == null,
        audioTime = info.audioDuration?.let { d ->
            (if (info.audioState == AudioState.INIT) d else info.audioRemainTime).milliSecondToTimerUnit()
        },
    )

    private fun onAudioToggle(r: QuizResponder) {
        val info = currentAudioInfos.firstOrNull { it.displaySeatNumber == r.seat } ?: return
        if (info.audioState == AudioState.PLAY) audioStartWindowModel.setPause() else audioStartWindowModel.setPlay(info)
    }

    private fun fetchDuration(info: AudioAnswerInfo) {
        if (info.serialNumber in durationFetching) return
        durationFetching += info.serialNumber
        coroutineScope.launch(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            val duration = try {
                retriever.setDataSource(info.audioUrl)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                0L
            } finally {
                runCatching { retriever.release() }
            }
            audioStartWindowModel.setDuration(info.copy(audioDuration = duration))
            withContext(Dispatchers.Main) {
                durationFetching -= info.serialNumber
                render(audioStartWindowModel.getCurrentStudentQuizAudioAnsweringInfoList())
            }
        }
    }

    private fun startStopwatch() {
        if (stopwatchJob?.isActive == true) return
        val startTimeInMillis = if (quizManager.quizStartTimeInMillis > 0) quizManager.quizStartTimeInMillis else System.currentTimeMillis()
        val timeDiffInMillis = TimeUtils.getTimeDiffFromCurrentTimeInMillis(startTimeInMillis)
        stopwatchJob = coroutineScope.startTimerInMilliSec(startTimeInMillis, timeDiffInMillis) { tickMs ->
            val (minutes, seconds) = DateTimeUtils.formatToMinuteSecondPair(tickMs)
            withContext(Dispatchers.Main) { ui.update { it.copy(stopwatch = String.format("%02d:%02d", minutes, seconds)) } }
        }
    }

    private fun setRefreshErrorHandler() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.refreshFailedFlow.collectLatest {
                withContext(Dispatchers.Main) { if (ui.value.hasNetwork) showErrorToast(context.getString(R.string.common_error_general)) }
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

    /** Audio skips DISCLOSE_ANSWER — End-and-review goes straight to results; stop any playback. */
    private fun endQuiz() {
        coroutineScope.launch(Dispatchers.IO) {
            if (quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.FINISH)) {
                withContext(Dispatchers.Main) {
                    audioStartWindowModel.setStop()
                    stopwatchJob?.cancel()
                    stopwatchJob = null
                }
                quizStartWindowModel.changeQuizState(QuizState.QUIZ_RESULTS)
            } else {
                withContext(Dispatchers.Main) { showErrorToast(context.getString(R.string.quiz_error_msg_end_quiz)) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopwatchJob?.cancel()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        quizCommonWindowModel.onCleared()
        quizStartWindowModel.onCleared()
        audioStartWindowModel.onCleared()
        coroutineScope.cancel()
    }
}
