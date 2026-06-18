package com.viewsonic.classswift.ui.window.quiz.start

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowMvbMultipleChoiceStartBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.QuizManager.QuizzingUiState
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.ui.widget.quiz.disclose.DiscloseOptionItemData
import com.viewsonic.classswift.ui.widget.quiz.disclose.SelectionMode
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.widget.quiz.result.CSResultAnalyticChip
import com.viewsonic.classswift.ui.widget.quiz.result.CSResultCorrectAnswerBadge
import com.viewsonic.classswift.ui.widget.quiz.result.CSResultOptionBarItem
import com.viewsonic.classswift.ui.widget.quiz.result.WcagPatternTiles
import com.viewsonic.classswift.ui.window.adapter.MvbQuizAnsweringAdapter
import com.viewsonic.classswift.ui.window.adapter.MvbQuizResultAdapter
import com.viewsonic.classswift.ui.window.decoration.StudentAnswerResultItemDecoration
import com.viewsonic.classswift.ui.windowmodel.quiz.MultipleChoiceWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.DateTimeUtils
import com.viewsonic.classswift.utils.TimeUtils
import com.viewsonic.classswift.utils.extension.dpToPx
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
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import timber.log.Timber

class MvbMultipleChoiceStartWindow(
    private val context: Context,
) : IWindow<WindowMvbMultipleChoiceStartBinding> {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val quizStartWindowModel: QuizStartWindowModel by inject(QuizStartWindowModel::class.java)
    private val multipleChoiceWindowModel: MultipleChoiceWindowModel by inject(MultipleChoiceWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val quizManager: QuizManager by inject(QuizManager::class.java)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var quizCancelJob: Job? = null
    private var stopwatchJob: Job? = null
    private var selectedDiscloseIds: Set<Int> = emptySet()

    private lateinit var studentAdapter: MvbQuizAnsweringAdapter
    private lateinit var studentResultAdapter: MvbQuizResultAdapter
    private var selectedHighlightOptionId: Int? = null
    private var currentResultTab: ResultTab = ResultTab.OVERVIEW
    private var hasTriggeredResultState: Boolean = false
    private val resultBarItems: MutableList<CSResultOptionBarItem> = mutableListOf()

    private enum class ResultTab { OVERVIEW, STUDENT_RESPONSES }

    override var tag: WindowTag = WindowTag.MVB_MULTIPLE_CHOICE_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
    )

    override val binding: WindowMvbMultipleChoiceStartBinding =
        WindowMvbMultipleChoiceStartBinding.inflate(
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
        Timber.d("[onCreate] MvbMultipleChoiceStartWindow")
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
        initQuizzingPanel()
        initDisclosePanel()
        initResultPanel()
        initCollection()
        setDiscloseErrorHandler()
        setRefreshErrorHandler()
        applyPanelVisibility(quizStartWindowModel.quizzingUiState.value.quizState)
        if (quizStartWindowModel.quizzingUiState.value.quizState == QuizState.QUIZ_RESULTS) {
            bindResultData(quizStartWindowModel.quizzingUiState.value)
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
        // MvbMultipleChoiceStartWindow is MVB-only; always show minimize.
        binding.ivMinimizeWindow.visibility = View.VISIBLE
        binding.ivToolbarBringToFront.visibility = View.GONE
        binding.csSnackbar.setText(context.getString(R.string.mvb_network_disconnect_toast))
    }

    private fun initQuizzingPanel() {
        with(binding.panelQuizzing) {
            tvQuizTypeLabel.text = context.getString(R.string.quiz_mvb_quizzing_type_multiple_choice)
            tvQuizTypeSubtitle.text = context.getString(
                if (QuizSharedUiInfo.quizType == QuizType.MULTIPLE_SELECT) {
                    R.string.quiz_mvb_quizzing_subtitle_multi_select
                } else {
                    R.string.quiz_mvb_quizzing_subtitle_single_select
                },
            )
            tvQuizTypeSubtitle.visibility = View.VISIBLE
            csScreenshotImage.apply {
                setImage(quizCommonWindowModel.getScreenImageUri())
                setMaskVisibility(false)
                setProgressbarVisibility(false)
                setUploadFailedContainerVisibility(false)
                setCaptureAgainButtonVisibility(false)
            }

            populateOptionChips(llOptionsRow, buildOptionLabels())

            studentAdapter = MvbQuizAnsweringAdapter { info ->
                quizStartWindowModel.updateStudentQuizAnsweringVisibility(info)
            }
            rvStudentList.apply {
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

            buttonEndAndReview.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() { endQuiz() }
            })
            llRefreshButton.setOnClickListener { quizStartWindowModel.refreshOngoingQuizState() }
        }
        startStopwatch()
    }

    /** Drive the in-panel `tv_stopwatch` TextView. Mirrors legacy [com.viewsonic.classswift.ui.widget.quiz.Stopwatch]. */
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
                binding.panelQuizzing.tvStopwatch.text = String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    private fun buildOptionLabels(): List<String> {
        val count = QuizSharedUiInfo.quizOptionCount
        val useAlphabet = QuizSharedUiInfo.quizOptionType == QuizOptionType.ALPHABET
        return (1..count).map { id ->
            if (useAlphabet) ('A' + (id - 1)).toString() else id.toString()
        }
    }

    private fun populateOptionChips(container: LinearLayout, labels: List<String>) {
        container.removeAllViews()
        val gap = context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400)
        labels.forEachIndexed { index, label ->
            val chip = TextView(context).apply {
                text = label
                gravity = Gravity.CENTER
                setTextColor(context.getColor(R.color.neutral_900))
                textSize = 26.67f
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundResource(R.drawable.bg_neutral100_radius800_line_neutral300_border400)
            }
            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            )
            if (index > 0) params.marginStart = gap
            container.addView(chip, params)
        }
    }

    companion object {
        private const val STUDENT_GRID_SPAN = 4
    }

    private fun initDisclosePanel() {
        val options = buildDiscloseOptions()
        val mode = if (QuizSharedUiInfo.quizType == QuizType.MULTIPLE_SELECT) {
            SelectionMode.MULTIPLE
        } else {
            SelectionMode.SINGLE
        }
        with(binding.panelQuizzing) {
            cswDiscloseOptionGroup.setMode(mode)
            cswDiscloseOptionGroup.setItems(options)
            cswDiscloseOptionGroup.onSelectionChanged = { selected ->
                selectedDiscloseIds = selected
                updatePublishButtonState(selected.isNotEmpty())
            }
            cslbDisclosePublish.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() = sendDiscloseAnswer()
            })
        }
        updatePublishButtonState(false)
    }

    private fun buildDiscloseOptions(): List<DiscloseOptionItemData> {
        val count = QuizSharedUiInfo.quizOptionCount
        val useAlphabet = QuizSharedUiInfo.quizOptionType == QuizOptionType.ALPHABET
        return (1..count).map { id ->
            val label = if (useAlphabet) ('A' + (id - 1)).toString() else id.toString()
            DiscloseOptionItemData(id, label)
        }
    }

    private fun updatePublishButtonState(enabled: Boolean) {
        val button = binding.panelQuizzing.cslbDisclosePublish
        if (enabled) button.setEnable() else button.setDisable()
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.quizzingUiState.collect { uiState ->
                maybeTriggerResultState(uiState)
                val infos = uiState.studentQuizzingInfoList.map { student ->
                    val answeringState = when {
                        student.answerDataList.isNotEmpty() -> AnsweringState.ANSWERED
                        student.status == StudentInfo.Status.ACTIVE -> AnsweringState.NOT_ANSWER
                        else -> AnsweringState.ABSENT
                    }
                    QuizAnsweringInfo.fromStudentMultipleChoiceQuizzingInfo(
                        student,
                        QuizSharedUiInfo.quizOptionType,
                        QuizSharedUiInfo.quizType,
                        answeringState,
                        student.answerDataList,
                    )
                }
                withContext(Dispatchers.Main) {
                    studentAdapter.submitList(infos)
                    binding.panelQuizzing.tvResponseCount.text =
                        "${uiState.answerCount} / ${uiState.attendanceCount}"
                    applyPanelVisibility(uiState.quizState)
                    if (uiState.quizState == QuizState.QUIZ_RESULTS) {
                        bindResultData(uiState)
                    }
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            quizCommonWindowModel.networkAvailabilityState.collect { hasNetwork ->
                withContext(Dispatchers.Main) {
                    if (!hasNetwork) {
                        binding.msdvCloseConfirm.dismiss()
                    }
                    binding.csSnackbar.isVisible = !hasNetwork
                }
            }
        }
    }

    private fun setDiscloseErrorHandler() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.discloseAnswerErrorFlow.collectLatest { _ ->
                withContext(Dispatchers.Main) {
                    updatePublishButtonState(selectedDiscloseIds.isNotEmpty())
                }
            }
        }
    }

    private fun setRefreshErrorHandler() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.refreshFailedFlow.collectLatest {
                withContext(Dispatchers.Main) {
                    if (!binding.csSnackbar.isVisible) {
                        binding.panelQuizzing.cstToast.setText(context.getString(R.string.common_error_general))
                        binding.panelQuizzing.cstToast.show(coroutineScope)
                    }
                }
            }
        }
    }

    private fun applyPanelVisibility(state: QuizState) {
        val showDisclose = state == QuizState.DISCLOSE_ANSWER
        val showResult = state == QuizState.QUIZ_RESULTS
        val hideQuizzing = showDisclose || showResult
        with(binding.panelQuizzing) {
            llQuizzingOptionsArea.visibility = if (hideQuizzing) View.GONE else View.VISIBLE
            llDiscloseSelectorArea.visibility = if (showDisclose) View.VISIBLE else View.GONE
            llResultOptionsArea.visibility = if (showResult) View.VISIBLE else View.GONE
            buttonEndAndReview.visibility = if (hideQuizzing) View.GONE else View.VISIBLE
            cslbDisclosePublish.visibility = if (showDisclose) View.VISIBLE else View.GONE
            llStopwatchGroup.visibility = if (hideQuizzing) View.GONE else View.VISIBLE
            llRefreshButton.visibility = if (hideQuizzing) View.GONE else View.VISIBLE
            rvStudentList.visibility = if (showResult) View.GONE else View.VISIBLE
            llResultTabs.visibility = if (showResult) View.VISIBLE else View.GONE
            svResultOverviewContent.visibility =
                if (showResult && currentResultTab == ResultTab.OVERVIEW) View.VISIBLE else View.GONE
            rvResultStudentList.visibility =
                if (showResult && currentResultTab == ResultTab.STUDENT_RESPONSES) View.VISIBLE else View.GONE
        }
    }

    private fun onCloseClicked() {
        showCloseConfirmDialog()
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
                        binding.mtCloseErrorToast.apply {
                            setText(context.getString(R.string.quiz_error_msg_close_quiz))
                            show(coroutineScope)
                        }
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

    private fun endQuiz() {
        binding.panelQuizzing.buttonEndAndReview.setLoading()
        coroutineScope.launch(Dispatchers.IO) {
            if (quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.FINISH)) {
                quizStartWindowModel.changeQuizState(QuizState.DISCLOSE_ANSWER)
            } else {
                withContext(Dispatchers.Main) {
                    binding.panelQuizzing.buttonEndAndReview.setEnable()
                    binding.mtCloseErrorToast.apply {
                        setText(context.getString(R.string.quiz_error_msg_close_quiz))
                        show(coroutineScope)
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
                    dismissCloseLoadingUi()
                    binding.mtCloseErrorToast.apply {
                        setText(context.getString(R.string.quiz_error_msg_close_quiz))
                        show(coroutineScope)
                    }
                }
            }
        }
    }

    private fun sendDiscloseAnswer() {
        binding.panelQuizzing.cslbDisclosePublish.setLoading()
        quizStartWindowModel.discloseAnswer(selectedDiscloseIds.toList())
    }

    private fun initResultPanel() {
        studentResultAdapter = MvbQuizResultAdapter()
        val barCount = QuizSharedUiInfo.quizOptionCount + 1 // + Not submitted
        with(binding.panelQuizzing) {
            populateResultBars(llResultBarsContainer, barCount)
            rvResultStudentList.apply {
                layoutManager = GridLayoutManager(context, STUDENT_GRID_SPAN)
                addItemDecoration(
                    StudentAnswerResultItemDecoration(
                        STUDENT_GRID_SPAN,
                        context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400),
                        context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400),
                    ),
                )
                adapter = studentResultAdapter
            }
            llResultTabOverview.setOnClickListener { setActiveResultTab(ResultTab.OVERVIEW) }
            llResultTabStudent.setOnClickListener { setActiveResultTab(ResultTab.STUDENT_RESPONSES) }
            applyLegendSwatches()
        }
        setActiveResultTab(currentResultTab)
    }

    /** Overlay WCAG patterns onto the 3 legend swatches under the pie chart (VSFT-7272). */
    private fun applyLegendSwatches() {
        with(binding.panelQuizzing) {
            tvResultLegendCorrect.setCompoundDrawablesRelative(
                WcagPatternTiles.buildLegendSwatch(context, WcagPatternTiles.LegendStyle.CORRECT),
                null, null, null,
            )
            tvResultLegendIncorrect.setCompoundDrawablesRelative(
                WcagPatternTiles.buildLegendSwatch(context, WcagPatternTiles.LegendStyle.INCORRECT),
                null, null, null,
            )
            tvResultLegendNoAnswer.setCompoundDrawablesRelative(
                WcagPatternTiles.buildLegendSwatch(context, WcagPatternTiles.LegendStyle.NO_ANSWER),
                null, null, null,
            )
        }
    }

    private fun populateResultBars(container: LinearLayout, count: Int) {
        if (resultBarItems.size == count) return
        container.removeAllViews()
        resultBarItems.clear()
        val gap = context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_200)
        repeat(count) { index ->
            val bar = CSResultOptionBarItem(context).apply {
                onBarClick = { handleBarClick(it) }
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

    private fun setActiveResultTab(tab: ResultTab) {
        currentResultTab = tab
        val isOverview = tab == ResultTab.OVERVIEW
        with(binding.panelQuizzing) {
            tvResultTabOverview.setTextColor(
                context.getColor(if (isOverview) R.color.color_4848F0 else R.color.neutral_900),
            )
            tvResultTabStudent.setTextColor(
                context.getColor(if (!isOverview) R.color.color_4848F0 else R.color.neutral_900),
            )
            vResultTabIndicatorOverview.visibility = if (isOverview) View.VISIBLE else View.INVISIBLE
            vResultTabIndicatorStudent.visibility = if (!isOverview) View.VISIBLE else View.INVISIBLE
            if (quizStartWindowModel.quizzingUiState.value.quizState == QuizState.QUIZ_RESULTS) {
                svResultOverviewContent.visibility = if (isOverview) View.VISIBLE else View.GONE
                rvResultStudentList.visibility = if (!isOverview) View.VISIBLE else View.GONE
            }
        }
    }

    private fun handleBarClick(optionId: Int) {
        selectedHighlightOptionId = if (selectedHighlightOptionId == optionId) null else optionId
        if (currentResultTab == ResultTab.OVERVIEW && selectedHighlightOptionId != null) {
            setActiveResultTab(ResultTab.STUDENT_RESPONSES)
        }
        studentResultAdapter.setHighlightedOptionId(selectedHighlightOptionId)
        bindResultData(quizStartWindowModel.quizzingUiState.value)
    }

    private fun maybeTriggerResultState(uiState: QuizzingUiState) {
        if (hasTriggeredResultState) return
        if (uiState.quizState != QuizState.DISCLOSE_ANSWER) return
        if (uiState.discloseAnswerData.isEmpty()) return
        hasTriggeredResultState = true
        multipleChoiceWindowModel.triggerQuizResultState()
    }

    private fun bindResultData(uiState: QuizzingUiState) {
        val students = uiState.studentQuizzingInfoList
        val correctDiscloseIds = uiState.discloseAnswerData.map { it.optionId }
        val optionCount = QuizSharedUiInfo.quizOptionCount
        val useAlphabet = QuizSharedUiInfo.quizOptionType == QuizOptionType.ALPHABET
        val infos = students.map { student ->
            val answeringState = when {
                student.answerDataList.isNotEmpty() -> AnsweringState.ANSWERED
                student.status == StudentInfo.Status.ACTIVE -> AnsweringState.NOT_ANSWER
                else -> AnsweringState.ABSENT
            }
            QuizAnsweringInfo.fromStudentMultipleChoiceQuizzingInfo(
                student,
                QuizSharedUiInfo.quizOptionType,
                QuizSharedUiInfo.quizType,
                answeringState,
                student.answerDataList,
            )
        }
        // Both student answerOption and disclose use 1-based ids (matches legacy
        // MultipleChoiceStartWindow.setAnswerOptionInfoView). For each option id
        // (1..N), count how many students picked it.
        val perOptionCount = IntArray(optionCount + 1) // [0] unused, [1..N] option counts
        infos.forEach { info ->
            info.answerOption.forEach { id ->
                if (id in 1..optionCount) perOptionCount[id]++
            }
        }
        // Match legacy convention (MultipleChoiceStartWindow.setAnswerOptionInfoView):
        // bar denominator = attendance (active students only, excludes absent).
        // The "Not submitted" bar counts only active students who did not answer.
        val noAnswerCount = infos.count { it.answeringState == AnsweringState.NOT_ANSWER }
        val attendance = infos.count { it.answeringState != AnsweringState.ABSENT }
        val correctCount = if (correctDiscloseIds.isEmpty()) 0 else infos.count {
            it.answeringState == AnsweringState.ANSWERED &&
                MvbQuizResultAdapter.ViewHolder.studentChoseCorrect(it, correctDiscloseIds)
        }
        val incorrectCount = (attendance - correctCount - noAnswerCount).coerceAtLeast(0)

        bindLeftOptionsForMc(
            correctDiscloseIds = correctDiscloseIds,
            optionCount = optionCount,
            useAlphabet = useAlphabet,
            perOptionCount = perOptionCount,
            noAnswerCount = noAnswerCount,
            attendance = attendance,
        )
        bindOverviewForMc(
            correctDiscloseIds = correctDiscloseIds,
            useAlphabet = useAlphabet,
            correct = correctCount,
            incorrect = incorrectCount,
            noAnswer = noAnswerCount,
            total = attendance,
        )
        studentResultAdapter.setCorrectDiscloseIds(correctDiscloseIds)
        studentResultAdapter.submitList(infos)
    }

    private fun bindLeftOptionsForMc(
        correctDiscloseIds: List<Int>,
        optionCount: Int,
        useAlphabet: Boolean,
        perOptionCount: IntArray,
        noAnswerCount: Int,
        attendance: Int,
    ) {
        if (resultBarItems.size < optionCount + 1) return
        val maxCount = attendance.coerceAtLeast(1)
        val correctIdSet = correctDiscloseIds.toSet()
        for (i in 1..optionCount) {
            val label = if (useAlphabet) ('A' + (i - 1)).toString() else i.toString()
            val isCorrect = correctIdSet.contains(i)
            resultBarItems[i - 1].setData(
                CSResultOptionBarItem.Data(
                    optionId = i,
                    label = label,
                    isCorrect = isCorrect,
                    responseCount = perOptionCount[i],
                    maxCount = maxCount,
                    style = if (isCorrect) CSResultOptionBarItem.BarStyle.CORRECT
                    else CSResultOptionBarItem.BarStyle.INCORRECT,
                    isHighlighted = selectedHighlightOptionId == null || selectedHighlightOptionId == i,
                ),
            )
        }
        // Last bar = Not submitted
        resultBarItems[optionCount].setData(
            CSResultOptionBarItem.Data(
                optionId = MvbQuizResultAdapter.ViewHolder.NOT_SUBMITTED_OPTION_PSEUDO_ID,
                label = context.getString(R.string.quiz_mvb_cell_not_submitted),
                isCorrect = false,
                responseCount = noAnswerCount,
                maxCount = maxCount,
                style = CSResultOptionBarItem.BarStyle.NEUTRAL,
                isHighlighted = selectedHighlightOptionId == null ||
                    selectedHighlightOptionId == MvbQuizResultAdapter.ViewHolder.NOT_SUBMITTED_OPTION_PSEUDO_ID,
            ),
        )
    }

    private fun bindOverviewForMc(
        correctDiscloseIds: List<Int>,
        useAlphabet: Boolean,
        correct: Int,
        incorrect: Int,
        noAnswer: Int,
        total: Int,
    ) {
        val correctLabels = correctDiscloseIds
            .sorted()
            .map { id ->
                if (useAlphabet) ('A' + (id - 1)).toString() else id.toString()
            }
            .ifEmpty { listOf("-") }
        val correctRate = if (total == 0) 0 else correct * 100 / total
        val incorrectRate = if (total == 0) 0 else incorrect * 100 / total
        val noAnswerRate = (100 - correctRate - incorrectRate).coerceAtLeast(0)
        with(binding.panelQuizzing) {
            populateCorrectBadges(llResultCorrectBadges, correctLabels)
            csracCorrect.setData(
                CSResultAnalyticChip.Data(
                    iconRes = R.drawable.ic_check_white,
                    label = context.getString(R.string.quiz_mvb_result_answered_correctly),
                    count = correct,
                ),
            )
            csracIncorrect.setData(
                CSResultAnalyticChip.Data(
                    iconRes = R.drawable.ic_cross,
                    label = context.getString(R.string.quiz_mvb_result_answered_incorrectly),
                    count = incorrect,
                ),
            )
            csracNoAnswer.setData(
                CSResultAnalyticChip.Data(
                    iconRes = null,
                    label = context.getString(R.string.quiz_mvb_result_no_answer),
                    count = noAnswer,
                ),
            )
            csapcResultPieChart.setData(correct, incorrect, noAnswer)
            tvResultLegendCorrect.text =
                context.getString(R.string.quiz_mvb_result_correct_rate, correctRate)
            tvResultLegendIncorrect.text =
                context.getString(R.string.quiz_mvb_result_incorrect_rate, incorrectRate)
            tvResultLegendNoAnswer.text =
                context.getString(R.string.quiz_mvb_result_no_answer_rate, noAnswerRate)
        }
    }

    private fun populateCorrectBadges(container: LinearLayout, labels: List<String>) {
        container.removeAllViews()
        val badgeSize = 48f.dpToPx().toInt()
        val gap = context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_200)
        labels.forEachIndexed { index, label ->
            val badge = CSResultCorrectAnswerBadge(context).apply { setLabel(label) }
            val lp = LinearLayout.LayoutParams(badgeSize, badgeSize)
            if (index > 0) lp.marginStart = gap
            container.addView(badge, lp)
        }
    }

    override fun onDestroy() {
        Timber.d("[onDestroy] MvbMultipleChoiceStartWindow")
        super.onDestroy()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        quizCommonWindowModel.onCleared()
        quizStartWindowModel.onCleared()
        coroutineScope.cancel()
    }
}
