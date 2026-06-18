package com.viewsonic.classswift.ui.window.quiz.start

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowMvbAudioStartBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.widget.quiz.result.CSResultAnalyticChip
import com.viewsonic.classswift.ui.widget.quiz.result.CSResultOptionBarItem
import com.viewsonic.classswift.ui.window.adapter.AudioAnswerAdapter
import com.viewsonic.classswift.ui.window.adapter.MvbAudioAnswerAdapter
import com.viewsonic.classswift.ui.window.decoration.StudentAnswerResultItemDecoration
import com.viewsonic.classswift.ui.windowmodel.quiz.AudioStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.DateTimeUtils
import com.viewsonic.classswift.utils.SpannableStringUtils
import com.viewsonic.classswift.utils.TimeUtils
import androidx.core.view.isVisible
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.utils.extension.startTimerInMilliSec
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class MvbAudioQuizStartWindow(
    private val context: Context,
) : IWindow<WindowMvbAudioStartBinding>,
    AudioAnswerAdapter.OnAudioAnswerItemEventListener {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val quizStartWindowModel: QuizStartWindowModel by inject(QuizStartWindowModel::class.java)
    private val audioStartWindowModel: AudioStartWindowModel by inject(AudioStartWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val quizManager: QuizManager by inject(QuizManager::class.java)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var quizCancelJob: Job? = null
    private var stopwatchJob: Job? = null

    private lateinit var studentAdapter: MvbAudioAnswerAdapter

    private var currentResultTab: ResultTab = ResultTab.OVERVIEW
    private var selectedHighlightOptionId: Int? = null
    private var showStudentsName: Boolean = true
    private var hasTriggeredResultState: Boolean = false
    private val resultBarItems: MutableList<CSResultOptionBarItem> = mutableListOf()

    private enum class ResultTab { OVERVIEW, STUDENT_RESPONSES }

    override var tag: WindowTag = WindowTag.MVB_AUDIO_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
    )

    override val binding: WindowMvbAudioStartBinding = WindowMvbAudioStartBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents,
            ),
        ),
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onCreate() {
        super.onCreate()
        if (!QuizSharedUiInfo.isOngoing) {
            quizStartWindowModel.changeQuizState(QuizState.QUIZZING)
        }
        quizStartWindowModel.setStudentQuizzingList()
    }

    override fun onViewCreated() {
        super.onViewCreated()
        unclosedMissionUiManager.notifyMissionOngoingIfNeeded(MissionType.QUIZ)
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
        initHeader()
        initScreenshotImage()
        initStudentList()
        initResultPanel()
        initOnClick()
        initCollection()
        val currentState = quizStartWindowModel.quizzingUiState.value.quizState
        applyStateUi(currentState)
        // Match legacy changeUiStatusForOngoing: skip stopwatch when reopening in result state.
        if (currentState == QuizState.QUIZZING) {
            startStopwatch()
        } else if (currentState == QuizState.QUIZ_RESULTS) {
            // Reopening directly into Result: pre-bind UI and mark result state handled
            // so the upcoming collect's first emission doesn't re-award points.
            audioStartWindowModel.getCurrentStudentQuizAudioAnsweringInfoList()
            val resultList = audioStartWindowModel.setQuizResult()
            bindResultData(resultList)
            hasTriggeredResultState = true
        }
    }

    private fun initHeader() {
        WindowControlButtonsUiHelper.setup(
            ivClose = binding.ivClose,
            ivMinimizeWindow = binding.ivMinimizeWindow,
            ivToolbarBringToFront = binding.ivToolbarBringToFront,
            windowTag = tag,
            isMvbBound = quizCommonWindowModel.isMyViewBoardBound(),
            csWindowManager = csWindowManager,
            coroutineScope = coroutineScope,
            onCloseClick = ::onCloseClicked,
            onAfterMinimize = {
                unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.QUIZ)
            },
        )
        // MvbAudioQuizStartWindow is MVB-only by design; always show minimize button per Figma.
        binding.ivMinimizeWindow.visibility = View.VISIBLE
        binding.ivToolbarBringToFront.visibility = View.GONE
        binding.csSnackbar.setText(context.getString(R.string.mvb_network_disconnect_toast))
    }

    private fun initScreenshotImage() {
        binding.csScreenshotImage.apply {
            setImage(uri = quizCommonWindowModel.getScreenImageUri())
            setCircleProgressbarVisibility(isShown = false)
            setMaskVisibility(isShown = false)
        }
    }

    private fun initStudentList() {
        studentAdapter = MvbAudioAnswerAdapter(this)
        binding.rvStudentList.apply {
            layoutManager = GridLayoutManager(context, STUDENT_GRID_SPAN)
            addItemDecoration(
                StudentAnswerResultItemDecoration(
                    STUDENT_GRID_SPAN,
                    context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400),
                    context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400),
                ),
            )
            adapter = studentAdapter
        }
    }

    private fun initResultPanel() {
        // Build the 2 option bars (Submitted / Not submitted) once; later setData updates them.
        populateResultBars(binding.llResultBarsContainer) { handleBarClick(it) }

        binding.llResultTabOverview.setOnClickListener { setActiveResultTab(ResultTab.OVERVIEW) }
        binding.llResultTabStudent.setOnClickListener { setActiveResultTab(ResultTab.STUDENT_RESPONSES) }
        binding.swShowStudentsName.setOnCheckedChangeListener { _, isChecked ->
            showStudentsName = isChecked
            studentAdapter.setShowStudentsName(isChecked)
        }
    }

    private fun populateResultBars(
        container: LinearLayout,
        onClick: (Int) -> Unit,
    ) {
        if (resultBarItems.size == BAR_COUNT) return
        container.removeAllViews()
        resultBarItems.clear()
        val gap = context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_200)
        repeat(BAR_COUNT) { index ->
            val bar = CSResultOptionBarItem(context).apply {
                onBarClick = onClick
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            if (index > 0) lp.topMargin = gap
            container.addView(bar, lp)
            resultBarItems += bar
        }
    }

    private fun initOnClick() {
        binding.buttonEndAndReview.setOnClickListener {
            // Legacy AudioQuizStartWindow used buttonEndQuiz.setLoading() to gate double-clicks
            // while the FINISH API call was in flight. The MVB layout uses a plain AppCompatButton
            // (no loading indicator), so disable the button to preserve the same single-shot
            // semantics; endQuiz re-enables on failure.
            binding.buttonEndAndReview.isEnabled = false
            endQuiz()
        }
        binding.llRefreshButton.setOnClickListener {
            quizStartWindowModel.refreshOngoingQuizState()
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.quizzingUiState.collect { uiState ->
                withContext(Dispatchers.Main) {
                    binding.tvResponseCount.text =
                        "${uiState.answerCount} / ${uiState.attendanceCount}"
                    applyStateUi(uiState.quizState)
                    // setQuizResult() mutates audioAnswerInfos in place — populate it
                    // first so reopening straight into Result doesn't act on an empty list.
                    val infos = audioStartWindowModel.getCurrentStudentQuizAudioAnsweringInfoList()
                    if (uiState.quizState == QuizState.QUIZ_RESULTS) {
                        if (!hasTriggeredResultState) {
                            hasTriggeredResultState = true
                            val resultList = audioStartWindowModel.setQuizResult()
                            bindResultData(resultList)
                            val submittedStudentIds = resultList
                                .filter { it.answerResultState == AnswerResultState.ANSWERED }
                                .map { it.studentId }
                            coroutineScope.launch(Dispatchers.IO) {
                                quizStartWindowModel.updateStudentsPoint(submittedStudentIds, 1)
                            }
                        } else {
                            // Subsequent result-state emissions: re-render UI but don't re-award points.
                            bindResultData(infos)
                        }
                    } else {
                        submitListWithHighlight(infos)
                    }
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            audioStartWindowModel.audioInfosSharedFlow.collect { infos ->
                withContext(Dispatchers.Main) {
                    submitListWithHighlight(infos)
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.refreshFailedFlow.collectLatest {
                withContext(Dispatchers.Main) {
                    if (!binding.csSnackbar.isVisible) {
                        binding.mtCloseErrorToast.setText(context.getString(R.string.common_error_general))
                        binding.mtCloseErrorToast.show(coroutineScope)
                    }
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.updateStudentsPointErrorFlow.collectLatest { message ->
                withContext(Dispatchers.Main) {
                    Timber.d("[updateStudentsPointErrorFlow]: $message")
                    val spannable = SpannableStringUtils.replaceStringFirstArgAsBoldStyle(
                        message,
                        context.getString(R.string.quiz_all_correct_students),
                    )
                    binding.mtCloseErrorToast.setText(spannable)
                    binding.mtCloseErrorToast.show(coroutineScope)
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            quizCommonWindowModel.networkAvailabilityState.collect { hasNetwork ->
                withContext(Dispatchers.Main) {
                    if (!hasNetwork) binding.msdvCloseConfirm.dismiss()
                    binding.csSnackbar.isVisible = !hasNetwork
                }
            }
        }
    }

    private fun applyStateUi(state: QuizState) {
        val isQuizzing = state == QuizState.QUIZZING
        val isResult = state == QuizState.QUIZ_RESULTS

        // Quizzing-only chrome
        binding.llStopwatchGroup.visibility = if (isQuizzing) View.VISIBLE else View.GONE
        binding.buttonEndAndReview.visibility = if (isQuizzing) View.VISIBLE else View.GONE
        binding.llRefreshButton.visibility = if (isQuizzing) View.VISIBLE else View.GONE

        // Result-only chrome
        binding.llResultOptionsArea.visibility = if (isResult) View.VISIBLE else View.GONE
        binding.llResultTabs.visibility = if (isResult) View.VISIBLE else View.GONE

        // Right-side body content visibility depends on current tab when in result.
        applyResultTabVisibility(isResult)

        // Adapter applies the green Submitted palette only in QUIZ_RESULTS;
        // Quizzing keeps the existing purple styling.
        studentAdapter.setIsResult(isResult)
    }

    private fun applyResultTabVisibility(isResult: Boolean) {
        val showOverview = isResult && currentResultTab == ResultTab.OVERVIEW
        val showStudentResponses = isResult && currentResultTab == ResultTab.STUDENT_RESPONSES
        // In Quizzing, the student grid stays visible (used for live answering view).
        val showStudentGrid = !isResult || showStudentResponses
        binding.svResultOverviewContent.visibility = if (showOverview) View.VISIBLE else View.GONE
        binding.rvStudentList.visibility = if (showStudentGrid) View.VISIBLE else View.GONE
        binding.llShowStudentsName.visibility = if (showStudentResponses) View.VISIBLE else View.GONE
    }

    private fun setActiveResultTab(tab: ResultTab) {
        currentResultTab = tab
        val isOverview = tab == ResultTab.OVERVIEW
        binding.tvResultTabOverview.setTextColor(
            context.getColor(if (isOverview) R.color.color_4848F0 else R.color.neutral_900),
        )
        binding.tvResultTabStudent.setTextColor(
            context.getColor(if (!isOverview) R.color.color_4848F0 else R.color.neutral_900),
        )
        binding.vResultTabIndicatorOverview.visibility =
            if (isOverview) View.VISIBLE else View.INVISIBLE
        binding.vResultTabIndicatorStudent.visibility =
            if (!isOverview) View.VISIBLE else View.INVISIBLE
        applyResultTabVisibility(
            isResult = quizStartWindowModel.quizzingUiState.value.quizState == QuizState.QUIZ_RESULTS,
        )
    }

    private fun handleBarClick(optionId: Int) {
        selectedHighlightOptionId = if (selectedHighlightOptionId == optionId) null else optionId
        // Auto-switch to Student responses tab when a bar is highlighted in Overview, mirroring T/F.
        if (currentResultTab == ResultTab.OVERVIEW && selectedHighlightOptionId != null) {
            setActiveResultTab(ResultTab.STUDENT_RESPONSES)
        }
        bindResultData(audioStartWindowModel.getCurrentStudentQuizAudioAnsweringInfoList())
    }

    private fun bindResultData(resultList: List<AudioAnswerInfo>) {
        val activeStudents = resultList.filter { it.answerResultState != AnswerResultState.ABSENT }
        val submitted = resultList.count { it.answerResultState == AnswerResultState.ANSWERED }
        val notSubmitted = resultList.count { it.answerResultState == AnswerResultState.NO_ANSWER }
        val attendance = activeStudents.size

        bindOptionBars(submitted, notSubmitted, attendance)
        bindOverview(submitted, notSubmitted, attendance)
        submitListWithHighlight(resultList)
    }

    private fun bindOptionBars(submitted: Int, notSubmitted: Int, attendance: Int) {
        if (resultBarItems.size < BAR_COUNT) return
        val maxCount = attendance.coerceAtLeast(1)
        resultBarItems[0].setData(
            CSResultOptionBarItem.Data(
                optionId = OPTION_ID_SUBMITTED,
                label = context.getString(R.string.quiz_mvb_cell_submitted),
                isCorrect = false,
                responseCount = submitted,
                maxCount = maxCount,
                style = CSResultOptionBarItem.BarStyle.CORRECT,
                isHighlighted = selectedHighlightOptionId == null ||
                    selectedHighlightOptionId == OPTION_ID_SUBMITTED,
            ),
        )
        resultBarItems[1].setData(
            CSResultOptionBarItem.Data(
                optionId = OPTION_ID_NOT_SUBMITTED,
                label = context.getString(R.string.quiz_mvb_cell_not_submitted),
                isCorrect = false,
                responseCount = notSubmitted,
                maxCount = maxCount,
                style = CSResultOptionBarItem.BarStyle.NEUTRAL,
                isHighlighted = selectedHighlightOptionId == null ||
                    selectedHighlightOptionId == OPTION_ID_NOT_SUBMITTED,
            ),
        )
    }

    private fun bindOverview(submitted: Int, notSubmitted: Int, attendance: Int) {
        binding.csracSubmitted.setData(
            CSResultAnalyticChip.Data(
                iconRes = R.drawable.ic_check_white,
                label = context.getString(R.string.quiz_mvb_cell_submitted),
                count = submitted,
            ),
        )
        binding.csracNotSubmitted.setData(
            CSResultAnalyticChip.Data(
                iconRes = null,
                label = context.getString(R.string.quiz_mvb_cell_not_submitted),
                count = notSubmitted,
            ),
        )

        // Pie chart reuses the T/F 3-segment widget; pass 0 for "incorrect" so only
        // submitted (green) and not-submitted (gray) segments render.
        val submittedRate = if (attendance == 0) 0 else submitted * 100 / attendance
        val notSubmittedRate = (100 - submittedRate).coerceAtLeast(0)
        binding.csapcResultPieChart.setData(submitted, 0, notSubmitted)
        binding.tvResultLegendSubmitted.text =
            context.getString(R.string.quiz_mvb_result_submitted_rate, submittedRate)
        binding.tvResultLegendNotSubmitted.text =
            context.getString(R.string.quiz_mvb_result_not_submitted_rate, notSubmittedRate)
    }

    /**
     * In the MVB adapter, isPartiallyVisible drives card dim alpha:
     *  - false = full color (active / highlighted)
     *  - true  = dimmed (highlight-excluded)
     *
     * Note: AudioAnswerInfo.isPartiallyVisible defaults to true (legacy semantics =
     * "name hidden"). We MUST set it explicitly on every submit, otherwise every
     * freshly-bound or recycled ViewHolder lands at alpha 0.3.
     *
     * QUIZZING: no highlight concept — force all cards to full color.
     * QUIZ_RESULTS: dim only the cards that don't match the selected highlight bar.
     */
    private fun submitListWithHighlight(infos: List<AudioAnswerInfo>) {
        val state = quizStartWindowModel.quizzingUiState.value.quizState
        val transformed = if (state == QuizState.QUIZ_RESULTS) {
            val selected = selectedHighlightOptionId
            infos.map { info ->
                val matches = when (selected) {
                    null -> true
                    OPTION_ID_SUBMITTED -> info.answerResultState == AnswerResultState.ANSWERED
                    OPTION_ID_NOT_SUBMITTED -> info.answerResultState == AnswerResultState.NO_ANSWER
                    else -> true
                }
                info.copy(isPartiallyVisible = !matches)
            }
        } else {
            infos.map { it.copy(isPartiallyVisible = false) }
        }
        studentAdapter.submitList(transformed)
    }

    private fun startStopwatch() {
        if (stopwatchJob?.isActive == true) return
        val startTimeInMillis = if (quizManager.quizStartTimeInMillis > 0) {
            quizManager.quizStartTimeInMillis
        } else {
            System.currentTimeMillis()
        }
        val timeDiffInMillis = TimeUtils.getTimeDiffFromCurrentTimeInMillis(startTimeInMillis)
        stopwatchJob = coroutineScope.startTimerInMilliSec(
            startTimeInMillis,
            timeDiffInMillis,
        ) { tickMs ->
            val (minutes, seconds) = DateTimeUtils.formatToMinuteSecondPair(tickMs)
            withContext(Dispatchers.Main) {
                binding.tvStopwatch.text = String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    private fun stopStopwatch() {
        stopwatchJob?.cancel()
        stopwatchJob = null
    }

    private fun endQuiz() {
        coroutineScope.launch(Dispatchers.IO) {
            if (quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.FINISH)) {
                withContext(Dispatchers.Main) {
                    audioStartWindowModel.setStop()
                    stopStopwatch()
                }
                quizStartWindowModel.changeQuizState(QuizState.QUIZ_RESULTS)
            } else {
                withContext(Dispatchers.Main) {
                    binding.buttonEndAndReview.isEnabled = true
                    binding.mtCloseErrorToast.setText(context.getString(R.string.quiz_error_msg_end_quiz))
                    binding.mtCloseErrorToast.show(coroutineScope)
                }
            }
        }
    }

    private fun onCloseClicked() {
        showCloseConfirmDialog()
    }

    private fun showCloseConfirmDialog() {
        binding.msdvCloseConfirm.setup(
            title = context.getString(R.string.quiz_disclose_close_confirm_title),
            message = context.getString(R.string.quiz_disclose_close_confirm_body),
            positiveText = context.getString(R.string.quiz_disclose_close_confirm_positive),
            onPositive = {
                binding.msdvCloseConfirm.dismiss()
                if (binding.csSnackbar.isVisible) {
                    quizManager.clearCurrentQuizInfo()
                    csWindowManager.removeWindow(tag)
                    unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                } else {
                    val isResult = quizStartWindowModel.quizzingUiState.value.quizState == QuizState.QUIZ_RESULTS
                    if (isResult) {
                        closeQuiz()
                    } else {
                        showCloseLoadingUi()
                        cancelQuiz()
                    }
                }
            },
            negativeText = context.getString(R.string.quiz_disclose_close_confirm_negative),
            onNegative = { quizCancelJob?.cancel() },
        ).show()
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
                    dismissCloseLoadingUi()
                    binding.mtCloseErrorToast.apply {
                        setText(context.getString(R.string.quiz_error_msg_close_quiz))
                        show(coroutineScope)
                    }
                }
            }
        }
    }

    private fun closeQuiz() {
        quizCancelJob?.cancel()
        quizCancelJob = coroutineScope.launch(Dispatchers.Main) {
            showCloseLoadingUi()
            withContext(Dispatchers.IO) {
                val success = quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.CLOSE)
                if (success) {
                    withContext(Dispatchers.Main) {
                        csWindowManager.removeWindow(tag)
                        unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        dismissCloseLoadingUi()
                        binding.mtCloseErrorToast.setText(context.getString(R.string.quiz_error_msg_close_quiz))
                        binding.mtCloseErrorToast.show(coroutineScope)
                    }
                }
            }
        }
    }

    private fun showCloseLoadingUi() {
        binding.flCloseLoadingOverlay.visibility = View.VISIBLE
        binding.lavLoading.playAnimation()
    }

    private fun dismissCloseLoadingUi() {
        binding.flCloseLoadingOverlay.visibility = View.GONE
        binding.lavLoading.cancelAnimation()
    }

    override fun onAudioAnswerItemEvent(event: AudioAnswerAdapter.AudioAnswerItemEventListener) {
        when (event) {
            is AudioAnswerAdapter.AudioAnswerItemEventListener.ItemClick -> {
                // Result already auto-reveals every recording's play state, so tapping
                // the card must not collapse it back. Only allow toggle in Quizzing.
                if (quizStartWindowModel.quizzingUiState.value.quizState != QuizState.QUIZ_RESULTS) {
                    audioStartWindowModel.setCanShowAnswer(event.info)
                }
            }
            is AudioAnswerAdapter.AudioAnswerItemEventListener.PauseAudio ->
                audioStartWindowModel.setPause()
            is AudioAnswerAdapter.AudioAnswerItemEventListener.PlayAudio ->
                audioStartWindowModel.setPlay(event.info)
            is AudioAnswerAdapter.AudioAnswerItemEventListener.UpdateDuration ->
                audioStartWindowModel.setDuration(event.info)
        }
    }

    override fun onDestroy() {
        Timber.d("[onDestroy] MvbAudioQuizStartWindow")
        super.onDestroy()
        stopStopwatch()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        quizCommonWindowModel.onCleared()
        quizStartWindowModel.onCleared()
        audioStartWindowModel.onCleared()
        coroutineScope.cancel()
    }

    companion object {
        private const val STUDENT_GRID_SPAN = 4
        private const val BAR_COUNT = 2
        private const val OPTION_ID_SUBMITTED = 0
        private const val OPTION_ID_NOT_SUBMITTED = 1
    }
}
