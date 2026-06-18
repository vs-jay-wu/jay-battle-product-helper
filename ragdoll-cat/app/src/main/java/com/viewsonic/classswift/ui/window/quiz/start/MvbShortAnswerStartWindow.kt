package com.viewsonic.classswift.ui.window.quiz.start

import android.content.Context
import androidx.core.view.isVisible
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowMvbShortAnswerStartBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.QuizManager.QuizzingUiState
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.widget.quiz.result.CSResultAnalyticChip
import com.viewsonic.classswift.ui.widget.quiz.result.CSResultOptionBarItem
import com.viewsonic.classswift.ui.widget.quiz.result.WcagPatternTiles
import com.viewsonic.classswift.ui.window.adapter.MvbQuizAnsweringAdapter
import com.viewsonic.classswift.ui.window.decoration.StudentAnswerResultItemDecoration
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.DateTimeUtils
import com.viewsonic.classswift.utils.TimeUtils
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

class MvbShortAnswerStartWindow(
    private val context: Context,
) : IWindow<WindowMvbShortAnswerStartBinding> {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val quizStartWindowModel: QuizStartWindowModel by inject(QuizStartWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val quizManager: QuizManager by inject(QuizManager::class.java)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var quizCancelJob: Job? = null
    private var stopwatchJob: Job? = null
    private lateinit var studentAdapter: MvbQuizAnsweringAdapter
    private lateinit var resultStudentAdapter: MvbQuizAnsweringAdapter
    private var currentResultTab: ResultTab = ResultTab.OVERVIEW
    private var selectedHighlightOptionId: Int? = null
    private var showStudentsName: Boolean = true
    private val resultBarItems: MutableList<CSResultOptionBarItem> = mutableListOf()
    private var currentResultInfoList: List<QuizAnsweringInfo> = emptyList()
    private var currentPopupStudentId: String = ""
    private var isSyncingShowNameToggles = false

    private enum class ResultTab { OVERVIEW, STUDENT_RESPONSES }

    override var tag: WindowTag = WindowTag.MVB_SHORT_ANSWER_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
    )

    override val binding: WindowMvbShortAnswerStartBinding = WindowMvbShortAnswerStartBinding.inflate(
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
        Timber.d("[onCreate] MvbShortAnswerStartWindow")
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
        initResultPanel()
        initCollection()
        setRefreshErrorHandler()
        applyPanelVisibility(quizStartWindowModel.quizzingUiState.value.quizState)
        if (quizStartWindowModel.quizzingUiState.value.quizState == QuizState.QUIZ_RESULTS) {
            bindResultData(quizStartWindowModel.quizzingUiState.value)
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
        binding.csSnackbar.setText(context.getString(R.string.mvb_network_disconnect_toast))
        binding.ivMinimizeWindow.visibility = View.VISIBLE
        binding.ivToolbarBringToFront.visibility = View.GONE
    }

    private fun initQuizzingPanel() {
        with(binding.panelQuizzing) {
            tvQuizTypeLabel.text = context.getString(R.string.short_answer_capitalized_first_word)
            tvQuizTypeSubtitle.visibility = View.GONE
            llQuizzingOptionsArea.visibility = View.GONE

            csScreenshotImage.apply {
                setImage(quizCommonWindowModel.getScreenImageUri())
                setMaskVisibility(false)
                setProgressbarVisibility(false)
                setCaptureAgainButtonVisibility(false)
            }

            val itemDecoration = StudentAnswerResultItemDecoration(
                STUDENT_GRID_SPAN,
                context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400),
                context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400),
            )
            studentAdapter = MvbQuizAnsweringAdapter { info ->
                quizStartWindowModel.updateStudentQuizAnsweringVisibility(info)
            }
            resultStudentAdapter = MvbQuizAnsweringAdapter { info ->
                showStudentAnswerPopup(info)
            }.also { it.setIsResult(true) }
            rvStudentList.apply {
                layoutManager = GridLayoutManager(context, STUDENT_GRID_SPAN)
                addItemDecoration(itemDecoration)
                adapter = studentAdapter
            }
            rvResultStudentList.apply {
                layoutManager = GridLayoutManager(context, STUDENT_GRID_SPAN)
                addItemDecoration(
                    StudentAnswerResultItemDecoration(
                        STUDENT_GRID_SPAN,
                        context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400),
                        context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400),
                    ),
                )
                adapter = resultStudentAdapter
            }

            buttonEndAndReview.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() { endQuiz() }
            })
            llRefreshButton.setOnClickListener { quizStartWindowModel.refreshOngoingQuizState() }
        }
        startStopwatch()
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
                binding.panelQuizzing.tvStopwatch.text = String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    private fun initResultPanel() {
        with(binding.panelQuizzing) {
            populateResultBars(llResultBarsContainer, RESULT_BAR_COUNT) { handleBarClick(it) }
            llResultTabOverview.setOnClickListener { setActiveResultTab(ResultTab.OVERVIEW) }
            llResultTabStudent.setOnClickListener { setActiveResultTab(ResultTab.STUDENT_RESPONSES) }
            swShowStudentsName.setOnCheckedChangeListener { _, isChecked ->
                if (isSyncingShowNameToggles) return@setOnCheckedChangeListener
                isSyncingShowNameToggles = true
                showStudentsName = isChecked
                studentAdapter.setShowStudentsName(isChecked)
                resultStudentAdapter.setShowStudentsName(isChecked)
                binding.swPopupShowStudentsName.isChecked = isChecked
                if (binding.flStudentAnswerPopup.visibility == View.VISIBLE) updatePopupStudentDisplay()
                isSyncingShowNameToggles = false
            }
        }
        binding.swPopupShowStudentsName.setOnCheckedChangeListener { _, isChecked ->
            if (isSyncingShowNameToggles) return@setOnCheckedChangeListener
            isSyncingShowNameToggles = true
            showStudentsName = isChecked
            studentAdapter.setShowStudentsName(isChecked)
            resultStudentAdapter.setShowStudentsName(isChecked)
            binding.panelQuizzing.swShowStudentsName.isChecked = isChecked
            updatePopupStudentDisplay()
            isSyncingShowNameToggles = false
        }
        binding.ivPopupPrev.setOnClickListener { navigateToPrevStudent() }
        binding.ivPopupNext.setOnClickListener { navigateToNextStudent() }
        binding.ivPopupClose.setOnClickListener { dismissStudentAnswerPopup() }
        with(binding.panelQuizzing) {
            applyLegendSwatches()
        }
        setActiveResultTab(currentResultTab)
    }

    private fun populateResultBars(container: LinearLayout, count: Int, onClick: (Int) -> Unit) {
        if (resultBarItems.size == count) return
        container.removeAllViews()
        resultBarItems.clear()
        val gap = context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_200)
        repeat(count) { index ->
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

    private fun applyLegendSwatches() {
        with(binding.panelQuizzing) {
            tvResultLegendCorrect.setCompoundDrawablesRelative(
                WcagPatternTiles.buildLegendSwatch(context, WcagPatternTiles.LegendStyle.CORRECT),
                null, null, null,
            )
            tvResultLegendNoAnswer.setCompoundDrawablesRelative(
                WcagPatternTiles.buildLegendSwatch(context, WcagPatternTiles.LegendStyle.NO_ANSWER),
                null, null, null,
            )
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.quizzingUiState.collect { uiState ->
                withContext(Dispatchers.Main) {
                    binding.panelQuizzing.tvResponseCount.text =
                        "${uiState.answerCount} / ${uiState.attendanceCount}"
                    applyPanelVisibility(uiState.quizState)
                    if (uiState.quizState == QuizState.QUIZ_RESULTS) {
                        bindResultData(uiState)
                    } else {
                        val infos = uiState.studentQuizzingInfoList.map { student ->
                            val hasAnswered = student.answerDataList.isNotEmpty() || student.answerStringData.isNotBlank()
                            val answeringState = when {
                                hasAnswered -> AnsweringState.ANSWERED
                                student.status == StudentInfo.Status.ACTIVE -> AnsweringState.NOT_ANSWER
                                else -> AnsweringState.ABSENT
                            }
                            QuizAnsweringInfo.fromStudentShortAnswerQuizzingInfo(student, answeringState)
                        }
                        studentAdapter.submitList(infos)
                    }
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
        val isResult = state == QuizState.QUIZ_RESULTS
        with(binding.panelQuizzing) {
            llResultOptionsArea.visibility = if (isResult) View.VISIBLE else View.GONE
            buttonEndAndReview.visibility = if (isResult) View.GONE else View.VISIBLE
            llStopwatchGroup.visibility = if (isResult) View.GONE else View.VISIBLE
            llRefreshButton.visibility = if (isResult) View.GONE else View.VISIBLE
            rvStudentList.visibility = if (isResult) View.GONE else View.VISIBLE
            llResultTabs.visibility = if (isResult) View.VISIBLE else View.GONE
            val showStudentResponses = isResult && currentResultTab == ResultTab.STUDENT_RESPONSES
            svResultOverviewContent.visibility =
                if (isResult && currentResultTab == ResultTab.OVERVIEW) View.VISIBLE else View.GONE
            rvResultStudentList.visibility = if (showStudentResponses) View.VISIBLE else View.GONE
            llShowStudentsName.visibility = if (showStudentResponses) View.VISIBLE else View.GONE
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
            val isResult = quizStartWindowModel.quizzingUiState.value.quizState == QuizState.QUIZ_RESULTS
            val showStudentResponses = isResult && !isOverview
            if (isResult) {
                svResultOverviewContent.visibility = if (isOverview) View.VISIBLE else View.GONE
                rvResultStudentList.visibility = if (showStudentResponses) View.VISIBLE else View.GONE
                llShowStudentsName.visibility = if (showStudentResponses) View.VISIBLE else View.GONE
            }
        }
    }

    private fun buildResultInfos(uiState: QuizzingUiState): List<QuizAnsweringInfo> {
        val infos = uiState.studentQuizzingInfoList.map { student ->
            val hasAnswered = student.answerDataList.isNotEmpty() || student.answerStringData.isNotBlank()
            val answeringState = when {
                hasAnswered -> AnsweringState.ANSWERED
                student.status == StudentInfo.Status.ACTIVE -> AnsweringState.NOT_ANSWER
                else -> AnsweringState.ABSENT
            }
            QuizAnsweringInfo.fromStudentShortAnswerQuizzingInfo(student, answeringState)
                .copy(canShowAnswer = true)
        }
        currentResultInfoList = infos
        return infos
    }

    private fun bindResultData(uiState: QuizzingUiState) {
        val infos = buildResultInfos(uiState)
        val submittedCount = infos.count { it.answeringState == AnsweringState.ANSWERED }
        val notSubmittedCount = infos.count { it.answeringState == AnsweringState.NOT_ANSWER }
        val attendance = submittedCount + notSubmittedCount

        bindLeftBars(submittedCount, notSubmittedCount, attendance)
        bindOverview(submittedCount, notSubmittedCount, attendance)
        resultStudentAdapter.submitList(infos)
    }

    private fun handleBarClick(optionId: Int) {
        selectedHighlightOptionId = if (selectedHighlightOptionId == optionId) null else optionId
        if (currentResultTab == ResultTab.OVERVIEW && selectedHighlightOptionId != null) {
            setActiveResultTab(ResultTab.STUDENT_RESPONSES)
        }
        if (quizStartWindowModel.quizzingUiState.value.quizState == QuizState.QUIZ_RESULTS) {
            resultStudentAdapter.setHighlightedOptionId(selectedHighlightOptionId)
            bindLeftBars(currentResultInfoList)
        }
    }

    private fun bindLeftBars(submitted: Int, notSubmitted: Int, total: Int) {
        if (resultBarItems.size < RESULT_BAR_COUNT) return
        val maxCount = total.coerceAtLeast(1)
        val selected = selectedHighlightOptionId
        resultBarItems[0].setData(
            CSResultOptionBarItem.Data(
                optionId = MvbQuizAnsweringAdapter.SUBMITTED_OPTION_PSEUDO_ID,
                label = context.getString(R.string.quiz_mvb_cell_submitted),
                isCorrect = false,
                responseCount = submitted,
                maxCount = maxCount,
                style = CSResultOptionBarItem.BarStyle.CORRECT,
                isHighlighted = selected == null || selected == MvbQuizAnsweringAdapter.SUBMITTED_OPTION_PSEUDO_ID,
            ),
        )
        resultBarItems[1].setData(
            CSResultOptionBarItem.Data(
                optionId = MvbQuizAnsweringAdapter.NOT_SUBMITTED_OPTION_PSEUDO_ID,
                label = context.getString(R.string.quiz_mvb_cell_not_submitted),
                isCorrect = false,
                responseCount = notSubmitted,
                maxCount = maxCount,
                style = CSResultOptionBarItem.BarStyle.NEUTRAL,
                isHighlighted = selected == null || selected == MvbQuizAnsweringAdapter.NOT_SUBMITTED_OPTION_PSEUDO_ID,
            ),
        )
    }

    private fun bindLeftBars(infos: List<QuizAnsweringInfo>) {
        val submitted = infos.count { it.answeringState == AnsweringState.ANSWERED }
        val notSubmitted = infos.count { it.answeringState == AnsweringState.NOT_ANSWER }
        val attendance = submitted + notSubmitted
        bindLeftBars(submitted, notSubmitted, attendance)
    }

    private fun answeredInfos() = currentResultInfoList.filter { it.answeringState == AnsweringState.ANSWERED }

    private fun showStudentAnswerPopup(info: QuizAnsweringInfo) {
        currentPopupStudentId = info.studentId
        binding.swPopupShowStudentsName.isChecked = showStudentsName
        binding.flStudentAnswerPopup.visibility = View.VISIBLE
        updatePopupContent(info)
    }

    private fun updatePopupContent(info: QuizAnsweringInfo) {
        val answered = answeredInfos()
        val idx = answered.indexOfFirst { it.studentId == info.studentId }
        with(binding) {
            tvPopupStudentAnswer.text = info.answerStringData
            tvPopupStudentName.visibility = if (showStudentsName) View.VISIBLE else View.GONE
            tvPopupStudentName.text = info.displayName
            val hasPrev = idx > 0
            val hasNext = idx < answered.size - 1
            ivPopupPrev.isEnabled = hasPrev
            ivPopupPrev.alpha = if (hasPrev) 1f else 0.3f
            ivPopupNext.isEnabled = hasNext
            ivPopupNext.alpha = if (hasNext) 1f else 0.3f
        }
    }

    private fun updatePopupStudentDisplay() {
        val info = answeredInfos().firstOrNull { it.studentId == currentPopupStudentId } ?: return
        updatePopupContent(info)
    }

    private fun navigateToPrevStudent() {
        val answered = answeredInfos()
        val idx = answered.indexOfFirst { it.studentId == currentPopupStudentId }
        if (idx > 0) {
            val prev = answered[idx - 1]
            currentPopupStudentId = prev.studentId
            updatePopupContent(prev)
        }
    }

    private fun navigateToNextStudent() {
        val answered = answeredInfos()
        val idx = answered.indexOfFirst { it.studentId == currentPopupStudentId }
        if (idx < answered.size - 1) {
            val next = answered[idx + 1]
            currentPopupStudentId = next.studentId
            updatePopupContent(next)
        }
    }

    private fun dismissStudentAnswerPopup() {
        binding.flStudentAnswerPopup.visibility = View.GONE
    }

    private fun bindOverview(submitted: Int, notSubmitted: Int, total: Int) {
        val submittedRate = if (total == 0) 0 else submitted * 100 / total
        val notSubmittedRate = (100 - submittedRate).coerceAtLeast(0)
        with(binding.panelQuizzing) {
            tvResultCorrectAnswerLabel.visibility = View.GONE
            llResultCorrectBadges.visibility = View.GONE
            viewResultCorrectAnswerDivider.visibility = View.GONE
            vResultCorrectIncorrectDivider.visibility = View.GONE
            csracIncorrect.visibility = View.GONE
            tvResultLegendIncorrect.visibility = View.GONE
            csracCorrect.setData(
                CSResultAnalyticChip.Data(
                    iconRes = null,
                    label = context.getString(R.string.quiz_mvb_cell_submitted),
                    count = submitted,
                ),
            )
            csracNoAnswer.setData(
                CSResultAnalyticChip.Data(
                    iconRes = null,
                    label = context.getString(R.string.quiz_mvb_cell_not_submitted),
                    count = notSubmitted,
                ),
            )
            csapcResultPieChart.setData(submitted, 0, notSubmitted)
            tvResultLegendCorrect.text =
                context.getString(R.string.quiz_mvb_result_correct_rate, submittedRate)
            tvResultLegendNoAnswer.text =
                context.getString(R.string.quiz_mvb_result_no_answer_rate, notSubmittedRate)
        }
    }

    private fun endQuiz() {
        binding.panelQuizzing.buttonEndAndReview.setLoading()
        quizCancelJob?.cancel()
        quizCancelJob = coroutineScope.launch(Dispatchers.IO) {
            if (quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.FINISH)) {
                quizStartWindowModel.changeQuizState(QuizState.QUIZ_RESULTS)
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

    private fun onCloseClicked() {
        showCloseConfirmDialog()
    }

    private fun closeQuiz() {
        quizCancelJob?.cancel()
        quizCancelJob = coroutineScope.launch(Dispatchers.Main) {
            showCloseLoadingUi()
            withContext(Dispatchers.IO) {
                val success = quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.CLOSE)
                withContext(Dispatchers.Main) {
                    if (success) {
                        csWindowManager.removeWindow(tag)
                        unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                    } else {
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

    override fun onDestroy() {
        Timber.d("[onDestroy] MvbShortAnswerStartWindow")
        super.onDestroy()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        quizCommonWindowModel.onCleared()
        quizStartWindowModel.onCleared()
        coroutineScope.cancel()
    }

    companion object {
        private const val STUDENT_GRID_SPAN = 4
        private const val RESULT_BAR_COUNT = 2
    }
}
