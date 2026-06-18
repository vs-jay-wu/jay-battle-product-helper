package com.viewsonic.classswift.ui.window.quiz.start

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.SketchStudentCardInfo
import com.viewsonic.classswift.data.quiz.SketchEventInfo
import com.viewsonic.classswift.data.quiz.SketchMarkUpdateState
import com.viewsonic.classswift.data.quiz.SketchResultState
import com.viewsonic.classswift.data.task.TaskResultInfo
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import com.viewsonic.classswift.databinding.WindowMvbSketchResponseStartBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.ui.widget.ImageDownloadView
import com.viewsonic.classswift.ui.widget.quiz.result.CSResultOptionBarItem
import com.viewsonic.classswift.ui.widget.quiz.result.MvbSketchResponseStudentResponsesWidget
import com.viewsonic.classswift.ui.widget.quiz.result.SketchReviewWidget
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus
import com.viewsonic.classswift.ui.window.adapter.MvbQuizAnsweringAdapter
import com.viewsonic.classswift.ui.window.decoration.StudentAnswerResultItemDecoration
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.SketchState
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.utils.extension.toQuizAnsweringInfo
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

/**
 * VSFT-8451 / VSFT-8453 / VSFT-8454 — Sketch Response 單一視窗。
 *
 * 遵循 mVB Quiz family 雙 panel 模式（同 MvbShortAnswerStartWindow）：
 * - [SketchState.ANSWERING]：polling + 學生監控（Jacky 實作，`panel_quizzing`）
 * - [SketchState.RESULT]：批改 + 結果 Overview（`panel_result`）
 *
 * 狀態切換由 [MvbSketchResponseStartWindowModel.collectAllAndMark] 觸發，
 * [applyPanelVisibility] 根據 [MvbSketchResponseStartWindowModel.sketchState] 切換面板可見度。
 */
class MvbSketchResponseStartWindow(
    private val context: Context,
) : IWindow<WindowMvbSketchResponseStartBinding> {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val windowModel: MvbSketchResponseStartWindowModel by inject(MvbSketchResponseStartWindowModel::class.java)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val studentAdapter = MvbQuizAnsweringAdapter(itemClickCallback = { /* no per-card interaction in sketch quizzing */ })

    // ─── Result phase state ───────────────────────────────────────────────────
    private var currentResultTab: ResultTab = ResultTab.OVERVIEW
    private val resultBarItems: MutableList<CSResultOptionBarItem> = mutableListOf()
    /** 題目 preview URL 快取，避免 bindResultData 每次 refresh 都重新下載。 */
    private var currentTaskImgUrl: String = ""

    private enum class ResultTab { OVERVIEW, STUDENT_RESPONSES }

    override var tag: WindowTag = WindowTag.MVB_SKETCH_RESPONSE_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
    )

    override val binding: WindowMvbSketchResponseStartBinding =
        WindowMvbSketchResponseStartBinding.inflate(
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
        Timber.d("[onCreate] MvbSketchResponseStartWindow")
        super.onCreate()
    }

    override fun onViewCreated() {
        super.onViewCreated()
        unclosedMissionUiManager.notifyMissionOngoingIfNeeded(MissionType.QUIZ)
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
        initHeader()
        initAnsweringPanel()
        initResultPanel()
        bindListener()
        observeUiState()
        observeEvents()
        observeSketchState()
        observeSketchResult()
        observeMarkUpdate()
        observeSketchEvent()
        windowModel.start()
    }

    // ─── Header ───────────────────────────────────────────────────────────────

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
        binding.ivMinimizeWindow.visibility = View.VISIBLE
        binding.ivToolbarBringToFront.visibility = View.GONE
    }

    // ─── Answering panel (ANSWERING state) ────────────────────────────────────

    private fun initAnsweringPanel() {
        with(binding.panelQuizzing) {
            ivQuizTypeIcon.setImageResource(R.drawable.ic_pen_curve_line)
            tvQuizTypeSubtitle.visibility = View.GONE
            llQuizzingOptionsArea.visibility = View.GONE
            llDiscloseSelectorArea.visibility = View.GONE
            llResultOptionsArea.visibility = View.GONE
            cslbDisclosePublish.visibility = View.GONE

            csScreenshotImage.apply {
                setImage(quizCommonWindowModel.getScreenImageUri())
                setMaskVisibility(false)
                setProgressbarVisibility(false)
                setCaptureAgainButtonVisibility(false)
            }

            buttonEndAndReview.setEnableText(context.getString(R.string.mvb_sketch_response_collect_and_mark))
            buttonEndAndReview.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() { windowModel.collectAllAndMark() }
            })
            llRefreshButton.setOnClickListener { windowModel.refreshNow() }

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
            rvResultStudentList.visibility = View.GONE
        }
    }

    // ─── Result panel (RESULT state) ──────────────────────────────────────────

    private fun initResultPanel() {
        initQuestionTitle()
        initQuestionPreview()
        initLeftBars()
        initTabs()
        initResultRefresh()
        initStudentResponsesWidget()
    }

    private fun initQuestionTitle() {
        binding.panelResult.tvQuestionTitle.text = context.getString(
            R.string.mvb_sketch_response_result_title_prefix,
            QUESTION_INDEX_SPRINT_20,
        )
    }

    /**
     * 題目 preview 影像下載 lifecycle 串接。
     * 預設把 Failed container 收起，避免 inflate 預設 visible 閃現。
     */
    private fun initQuestionPreview() {
        with(binding.panelResult.ivQuestionPreview) {
            setCircleProgressbarVisibility(isShown = false)
            setFailedContainerVisibility(isShown = false)
            setOnDownloadImageListener(
                object : ImageDownloadView.DownloadImageListener {
                    override fun onReDownload() {
                        val url = currentTaskImgUrl
                        if (url.isNotEmpty()) setImage(url)
                    }

                    override fun onStartDownload() {
                        setCircleProgressbarVisibility(isShown = true)
                        setFailedContainerVisibility(isShown = false)
                        startProgressAnimation(fromPercentage = 0, toPercentage = 90)
                    }

                    override fun onDownloadSuccess() {
                        startProgressAnimation(fromPercentage = 0, toPercentage = 100)
                        setCircleProgressbarVisibility(isShown = false)
                        setFailedContainerVisibility(isShown = false)
                    }

                    override fun onDownloadCancel() {
                        setCircleProgressbarVisibility(isShown = false)
                        setFailedContainerVisibility(isShown = true)
                    }

                    override fun onDownloadError() {
                        setCircleProgressbarVisibility(isShown = false)
                        setFailedContainerVisibility(isShown = true)
                    }
                },
            )
        }
    }

    private fun initLeftBars() {
        if (resultBarItems.isNotEmpty()) return
        val container = binding.panelResult.llResultBarsContainer
        container.removeAllViews()
        val gap = context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_200)
        repeat(BAR_COUNT) { index ->
            val bar = CSResultOptionBarItem(context)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            if (index > 0) lp.topMargin = gap
            container.addView(bar, lp)
            resultBarItems += bar
        }
    }

    private fun initTabs() {
        binding.panelResult.llResultTabOverview.setOnClickListener { setActiveTab(ResultTab.OVERVIEW) }
        binding.panelResult.llResultTabStudent.setOnClickListener { setActiveTab(ResultTab.STUDENT_RESPONSES) }
        setActiveTab(currentResultTab)
    }

    private fun setActiveTab(tab: ResultTab) {
        currentResultTab = tab
        val isOverview = tab == ResultTab.OVERVIEW
        binding.panelResult.tvResultTabOverview.setTextColor(
            context.getColor(if (isOverview) R.color.color_4848F0 else R.color.neutral_900),
        )
        binding.panelResult.tvResultTabStudent.setTextColor(
            context.getColor(if (!isOverview) R.color.color_4848F0 else R.color.neutral_900),
        )
        binding.panelResult.vResultTabIndicatorOverview.visibility =
            if (isOverview) View.VISIBLE else View.INVISIBLE
        binding.panelResult.vResultTabIndicatorStudent.visibility =
            if (!isOverview) View.VISIBLE else View.INVISIBLE
        binding.panelResult.flTabContentOverview.visibility = if (isOverview) View.VISIBLE else View.GONE
        binding.panelResult.wsrStudentResponses.visibility = if (!isOverview) View.VISIBLE else View.GONE
    }

    private fun initResultRefresh() {
        binding.panelResult.llRefreshButton.setOnClickListener {
            if (windowModel.getCurrentFocusTaskId() == null) {
                // First load failed (slow network before "Collect all and mark") — retry entry.
                transitionToResult()
            } else {
                coroutineScope.launch { windowModel.refreshResult() }
            }
        }
    }

    private fun initStudentResponsesWidget() {
        binding.panelResult.wsrStudentResponses.setEventListener(
            object : MvbSketchResponseStudentResponsesWidget.EventListener {
                override fun onCardClicked(record: TaskResultInfo) {
                    enterSketchReviewMode(record)
                }
            },
        )
        binding.srwSketchReview.setEventListener(
            object : SketchReviewWidget.SketchReviewWidgetEventListener {
                override fun onSendMarkedResult(data: TaskResultInfo.Content) {
                    coroutineScope.launch { windowModel.saveAndHandBack(data) }
                }

                override fun onClose() {
                    exitSketchReviewMode()
                }

                override fun onRefetchStudentRecord(data: TaskResultInfo) {
                    windowModel.getStudentTaskResult(data.studentId)
                }
            },
        )
    }

    private fun enterSketchReviewMode(record: TaskResultInfo) {
        binding.srwSketchReview.setRecord(record)
        binding.srwSketchReview.show()
    }

    private fun exitSketchReviewMode() {
        binding.srwSketchReview.dismiss()
    }

    // ─── State switching ──────────────────────────────────────────────────────

    /**
     * 依 [SketchState] 切換兩個 panel 的可見度（互斥）。
     * ANSWERING：panel_quizzing visible, panel_result gone。
     * RESULT：反之。
     */
    private fun applyPanelVisibility(state: SketchState) {
        val isResult = state == SketchState.RESULT
        binding.panelQuizzing.root.visibility = if (isResult) View.GONE else View.VISIBLE
        binding.panelResult.root.visibility = if (isResult) View.VISIBLE else View.GONE
        resizeWindowForState(isResult)
    }

    /**
     * ANSWERING → RESULT 時動態加高視窗（Figma 1280×784 ÷ 1.5 = 853×522.67dp）。
     * size = WRAP_CONTENT × WRAP_CONTENT，updateLayoutParams 設完高度後需等 fl_shell_root
     * 完成 measure，才透過 post {} 呼叫 updateLayoutParam 通知 WindowManager re-measure。
     *
     * Guard：若 flShellRoot 已是目標高度（例如 Save 後 exitSketchReviewMode 觸發 re-layout）
     * 則跳過，避免 post {} 在不必要時再次觸發 WindowManager update 造成短暫閃動。
     */
    private fun resizeWindowForState(isResult: Boolean) {
        val targetHeightRes = if (isResult) {
            R.dimen.quiz_mvb_sketch_response_result_window_height
        } else {
            R.dimen.quiz_mvb_poll_start_window_height
        }
        val targetHeightPx = context.resources.getDimensionPixelSize(targetHeightRes)
        val currentHeightPx = binding.flShellRoot.layoutParams?.height ?: 0
        if (currentHeightPx == targetHeightPx) return
        binding.flShellRoot.updateLayoutParams {
            height = targetHeightPx
        }
        binding.flShellRoot.post {
            csWindowManager.getWindow(tag)?.let { container ->
                container.updateLayoutParam(container.getLayoutParam())
            }
        }
    }

    /**
     * [windowModel.sketchState] ANSWERING → RESULT 轉換時：
     * 1. 切換 panel 可見度
     * 2. 從 UiState 取得 taskId / batchTaskId → 交給 windowModel 載入結果
     */
    private fun observeSketchState() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.sketchState
                .collect { state ->
                    applyPanelVisibility(state)
                    if (state == SketchState.RESULT) {
                        transitionToResult()
                    }
                }
        }
    }

    private fun transitionToResult() {
        val taskInfo = windowModel.uiState.value.taskInfo
        val batchTaskId = taskInfo.taskId.takeIf { it.isNotBlank() }
        val taskId = taskInfo.taskIds.firstOrNull()
        if (taskId == null) {
            // Polling hasn't returned yet (slow network / pressed too early) — show toast so
            // the teacher knows they can hit Refresh to retry.
            Timber.w("[SketchResponse] transitionToResult: taskId unavailable, showing load failed toast")
            showLoadFailedToast()
            return
        }
        windowModel.setBatchTaskId(batchTaskId)
        coroutineScope.launch { windowModel.loadResult(taskId) }
    }

    // ─── Answering phase observers ────────────────────────────────────────────

    private fun bindListener() {
        windowModel.listener = object : MvbSketchResponseStartWindowModel.Listener {
            override fun onRequestCloseWindow() {
                csWindowManager.removeWindow(tag)
                unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
            }
        }
    }

    private fun observeUiState() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.uiState.collect { state -> renderState(state) }
        }
    }

    private fun observeEvents() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.events.collect { event -> handleAnsweringEvent(event) }
        }
    }

    private fun handleAnsweringEvent(event: MvbSketchResponseStartWindowModel.UiEvent) {
        val message = when (event) {
            MvbSketchResponseStartWindowModel.UiEvent.NetworkDisconnected ->
                context.getString(R.string.mvb_network_disconnect_toast)
            MvbSketchResponseStartWindowModel.UiEvent.RefreshFailed ->
                context.getString(R.string.mvb_sketch_response_refresh_failed_toast)
        }
        binding.mtRefreshErrorToast.apply {
            setText(message)
            show(coroutineScope)
        }
    }

    private fun renderState(state: MvbSketchResponseStartWindowModel.UiState) {
        renderSketchHeader(state)
        renderStudents(state.students)
        renderCounter(state.submittedCount, state.totalCount)
        renderRefreshButton(state.isRefreshing)
        renderCloseConfirm(state.closeConfirmShown)
    }

    private fun renderRefreshButton(isRefreshing: Boolean) {
        with(binding.panelQuizzing) {
            ivRefreshIcon.visibility = if (isRefreshing) View.GONE else View.VISIBLE
            tvRefreshLabel.visibility = if (isRefreshing) View.GONE else View.VISIBLE
            lavRefreshLoading.visibility = if (isRefreshing) View.VISIBLE else View.GONE
            llRefreshButton.isClickable = !isRefreshing
            if (isRefreshing) lavRefreshLoading.playAnimation() else lavRefreshLoading.cancelAnimation()
        }
    }

    private fun renderSketchHeader(state: MvbSketchResponseStartWindowModel.UiState) {
        val safeCount = state.taskInfo.sketchCount.coerceAtLeast(0)
        binding.panelQuizzing.tvQuizTypeLabel.text = context.resources.getQuantityString(
            R.plurals.mvb_sketch_response_in_progress,
            safeCount,
            safeCount,
        )
        binding.panelQuizzing.tvStopwatch.text = formatTimer(state.elapsedSeconds)
    }

    private fun renderStudents(students: List<SketchStudentCardInfo>) {
        studentAdapter.submitList(students.map { it.toQuizAnsweringInfo() })
    }

    private fun renderCounter(submitted: Int, total: Int) {
        binding.panelQuizzing.tvResponseCount.text = context.getString(
            R.string.mvb_sketch_response_counter_format,
            submitted,
            total,
        )
    }

    private fun renderCloseConfirm(shown: Boolean) {
        if (shown) {
            showAnsweringCloseConfirmDialog()
        } else {
            binding.msdvCloseConfirm.dismiss()
        }
    }

    /**
     * 兩個 close confirm dialog（ANSWERING / RESULT）共用相同 title / message / button 文字，
     * 只有 [onConfirm] 邏輯不同。[onCancel] 預設為 no-op（RESULT 狀態無需更新 WindowModel 狀態）。
     */
    private fun showCloseConfirmDialog(
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {},
    ) {
        binding.msdvCloseConfirm.setup(
            title = context.getString(R.string.quiz_disclose_close_confirm_title),
            message = context.getString(R.string.quiz_disclose_close_confirm_body),
            positiveText = context.getString(R.string.quiz_disclose_close_confirm_positive),
            onPositive = onConfirm,
            negativeText = context.getString(R.string.quiz_disclose_close_confirm_negative),
            onNegative = onCancel,
        ).show()
    }

    /**
     * ANSWERING 狀態的 [X] 確認：確認後只 removeWindow，不打任何 close API
     *（batch 尚未結束，直接關窗讓 batch 繼續在後台保留）。
     */
    private fun showAnsweringCloseConfirmDialog() {
        showCloseConfirmDialog(
            onConfirm = { windowModel.confirmClose() },
            onCancel = { windowModel.cancelCloseConfirm() },
        )
    }

    // ─── Result phase observers ───────────────────────────────────────────────

    private fun observeSketchResult() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.sketchResultFlow.collect { state -> bindResultData(state) }
        }
    }

    private fun observeSketchEvent() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.resultEventFlow.collect { event ->
                when (event) {
                    SketchEventInfo.GetRecordByTaskIdFailed -> showLoadFailedToast()
                }
            }
        }
    }

    private fun showLoadFailedToast() {
        with(binding.mtLoadFailedToast) {
            setText(context.getString(R.string.mvb_sketch_response_error_load_failed))
            show(coroutineScope)
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
                // 更新 coordinator 狀態（無論 widget 是否可見）
                state.success?.let { windowModel.applyMarkSuccess(it) }
                // 只在 widget 可見時更新 UI，避免 widget 已隱藏時觸發幽靈 toast / dismiss
                if (binding.srwSketchReview.isShownOnScreen()) {
                    when {
                        state.success != null -> binding.srwSketchReview.onMarkUpdateSuccess(state.success)
                        state.failed != null -> binding.srwSketchReview.onMarkUpdateResultError(state.failed)
                        // Backend returned empty success/failed lists — reset UI to unblock Send button.
                        else -> binding.srwSketchReview.onMarkUnknownError()
                    }
                }
            }
            is SketchMarkUpdateState.Failed -> {
                Timber.w("Sketch mark failed: ${state.reason}")
                if (binding.srwSketchReview.isShownOnScreen()) {
                    binding.srwSketchReview.onMarkUnknownError()
                }
            }
            is SketchMarkUpdateState.Idle -> Unit
        }
    }

    private fun bindResultData(state: SketchResultState) {
        val submittedCount = state.recordList.count { item ->
            item is TaskResultInfo.Content &&
                (item.triggerType == SubmitStatus.RESPONSE || item.triggerType == SubmitStatus.GRADED)
        }
        val notSubmittedCount = state.recordList.count { item ->
            item is TaskResultInfo.Content && item.triggerType == SubmitStatus.UNSUBMITTED
        }
        val attendance = submittedCount + notSubmittedCount

        binding.panelResult.tvResponseCountSubmitted.text = submittedCount.toString()
        binding.panelResult.tvResponseCountTotal.text = attendance.toString()

        if (state.taskImgUrl.isNotEmpty()) {
            binding.panelResult.ivQuestionPreview.visibility = View.VISIBLE
            if (state.taskImgUrl != currentTaskImgUrl) {
                currentTaskImgUrl = state.taskImgUrl
                binding.panelResult.ivQuestionPreview.setImage(state.taskImgUrl)
            }
        } else {
            binding.panelResult.ivQuestionPreview.visibility = View.INVISIBLE
            currentTaskImgUrl = ""
        }
        bindLeftBars(submittedCount, notSubmittedCount, attendance)
        bindOverview(submittedCount, notSubmittedCount, attendance)
        binding.panelResult.wsrStudentResponses.setRecords(state.recordList)
    }

    private fun bindLeftBars(submitted: Int, notSubmitted: Int, total: Int) {
        if (resultBarItems.size < BAR_COUNT) return
        val maxCount = total.coerceAtLeast(1)
        resultBarItems[BAR_INDEX_SUBMITTED].setData(
            CSResultOptionBarItem.Data(
                optionId = BAR_INDEX_SUBMITTED,
                label = context.getString(R.string.mvb_sketch_response_status_submitted),
                isCorrect = false,
                responseCount = submitted,
                maxCount = maxCount,
                style = CSResultOptionBarItem.BarStyle.CORRECT,
                isHighlighted = true,
            ),
        )
        resultBarItems[BAR_INDEX_NOT_SUBMITTED].setData(
            CSResultOptionBarItem.Data(
                optionId = BAR_INDEX_NOT_SUBMITTED,
                label = context.getString(R.string.mvb_sketch_response_status_not_submitted),
                isCorrect = false,
                responseCount = notSubmitted,
                maxCount = maxCount,
                style = CSResultOptionBarItem.BarStyle.NEUTRAL,
                isHighlighted = true,
            ),
        )
    }

    private fun bindOverview(submitted: Int, notSubmitted: Int, total: Int) {
        binding.panelResult.tvOverviewSubmittedCount.text = submitted.toString()
        binding.panelResult.tvOverviewNotSubmittedCount.text = notSubmitted.toString()
        binding.panelResult.csapcOverviewPieChart.setData(
            correct = submitted,
            incorrect = 0,
            noAnswer = notSubmitted,
        )
        val safeTotal = total.coerceAtLeast(1)
        val submittedPct = submitted * 100 / safeTotal
        val notSubmittedPct = (100 - submittedPct).coerceAtLeast(0)
        binding.panelResult.tvOverviewLegendSubmitted.text = context.getString(
            R.string.quiz_mvb_result_submitted_rate,
            submittedPct,
        )
        binding.panelResult.tvOverviewLegendNotSubmitted.text = context.getString(
            R.string.quiz_mvb_result_not_submitted_rate,
            notSubmittedPct,
        )
    }

    // ─── Close flow ───────────────────────────────────────────────────────────

    private fun onCloseClicked() {
        when (windowModel.sketchState.value) {
            SketchState.ANSWERING -> windowModel.requestClose()
            SketchState.RESULT -> showResultCloseConfirmDialog()
        }
    }

    /**
     * RESULT 狀態的 [X] 確認：先 closeBatchTask（學生端回上課狀態），再 removeWindow。
     * [windowModel.closeBatch] 回 false 代表 API 失敗 → 顯示 toast 並阻止關窗（符合全專案慣例）。
     * batchTaskId 為 null 時 closeBatch 回 true（無需打 API，直接關窗）。
     */
    private fun showResultCloseConfirmDialog() {
        showCloseConfirmDialog(
            onConfirm = {
                binding.msdvCloseConfirm.dismiss()
                coroutineScope.launch {
                    val success = windowModel.closeBatch()
                    withContext(Dispatchers.Main) {
                        if (!success) {
                            binding.mtLoadFailedToast.apply {
                                setText(context.getString(R.string.mvb_sketch_response_error_close_batch_failed))
                                show(coroutineScope)
                            }
                            return@withContext
                        }
                        csWindowManager.removeWindow(tag)
                        unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                    }
                }
            },
        )
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onDestroy() {
        Timber.d("[onDestroy] MvbSketchResponseStartWindow")
        super.onDestroy()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        windowModel.listener = null
        windowModel.onCleared()
        binding.panelResult.wsrStudentResponses.setEventListener(null)
        binding.srwSketchReview.release()
        coroutineScope.cancel()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun formatTimer(elapsedSeconds: Int): String {
        val safe = elapsedSeconds.coerceAtLeast(0)
        val minutes = safe / SECONDS_PER_MINUTE
        val seconds = safe % SECONDS_PER_MINUTE
        return context.getString(R.string.mvb_sketch_response_timer_format, minutes, seconds)
    }

    companion object {
        private const val STUDENT_GRID_SPAN = 4
        private const val SECONDS_PER_MINUTE = 60

        private const val BAR_COUNT = 2
        private const val BAR_INDEX_SUBMITTED = 0
        private const val BAR_INDEX_NOT_SUBMITTED = 1

        /** Sprint 20 單題 → 題目序號固定為 1。Sprint 21+ 改動態。 */
        private const val QUESTION_INDEX_SPRINT_20 = 1
    }
}
