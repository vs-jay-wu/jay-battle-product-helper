package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.enum.ClassType
import com.viewsonic.classswift.data.enum.MvbToolbarPosition
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.databinding.WindowJoinClassBinding
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.helper.JoinClassWindowPositioner
import com.viewsonic.classswift.ui.helper.MvbSpinnerWindowOpener
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.ui.window.adapter.JoinClassStudentListAdapter
import com.viewsonic.classswift.ui.windowmodel.JoinClassWindowModel
import com.viewsonic.classswift.ui.windowmodel.JoinClassWindowModel.JoinClassUIEvent
import com.viewsonic.classswift.utils.DisplayUtils
import com.viewsonic.classswift.utils.QRCodeUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class JoinClassWindow(
    val context: Context
) : IWindow<WindowJoinClassBinding> {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val wModel: JoinClassWindowModel by inject(JoinClassWindowModel::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var uiCollectJob: Job? = null

    /** 等待確認移除的學生，in-window dialog 開啟期間持有，關閉時清除 */
    private var pendingRemoveStudentInfo: StudentInfo? = null

    /** VSFT-8256：首次出現斷線 UI 後設為 true，防止 Amplitude event 重複送出 */
    private var hasNetworkDisconnectBodyShownOnce = false

    override var tag: WindowTag = WindowTag.JOIN_CLASS

    // VSFT-7257: 視窗尺寸與 window_join_class.xml 的 root layout 對齊。
    // 釘死成固定 dp（不再 WRAP_CONTENT）以保證高度在不同 toolbar 位置、
    // 不同 measure 時點下都一致；之前 WRAP_CONTENT 會讓 getCurrentSize() 在
    // view-not-attached 時走 measure(UNSPECIFIED) 走出不同結果，導致視窗
    // 在不同 toolbar 位置下高度不同。
    override var size: SizeInPixels = SizeInPixels(
        WINDOW_WIDTH_DP.dpToPx().toInt(),
        WINDOW_HEIGHT_DP.dpToPx().toInt()
    )

    override val binding: WindowJoinClassBinding = WindowJoinClassBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )

    // VSFT-7257: 回傳與 [size] 相同的固定值，保證 reposition 與 opener 計算
    // location 時都用一致 windowSize（不再依 view 是否 attached 而變動）。
    override fun getCurrentSize(): SizeInPixels = size

    override fun onCreate() {
        if (!wModel.isMyViewBoardBound()) {
            csWindowManager.showWindow(WindowTag.TOOLBAR)
        }
    }

    override fun onViewCreated() {
        if (!wModel.isSelectedClassroomInfoExisted()) {
            binding.root.post {
                CSSystemDialogWindow.Builder(context)
                    .setTitle(context.getString(R.string.error_msg_general))
                    .setMessage(context.getString(R.string.error_msg_general_try_again))
                    .setPositiveButton(
                        context.getString(R.string.common_ok),
                        context.getColor(R.color.color_2E3133)
                    ) {
                        csWindowManager.removeWindow(tag)
                    }
                    .build()
                    .show()
            }
            return
        }
        // VSFT-7257: initial position is set up-front by `JoinClassWindowOpener`
        // (callers must use it). Repositioning here is impossible because at
        // onViewCreated() time the window is not yet in CSWindowManager's
        // windowMap, so reposition() would silently no-op. Subsequent toolbar
        // position changes flow through the collect in initCollect() below.
        initView()
        initCollect()
        fetchAndInitStudentList()
    }

    override fun onDestroy() {
        uiCollectJob?.cancel()
        coroutineScope.cancel()
        wModel.onCleared()
    }

    private fun initView() {
        with(binding) {
            // Class info row: show class name
            tvJoinClassTitle.text = wModel.getClassName()

            // Step 2: class code tiles
            setupClassCodeTiles(wModel.getClassCode())

            val roomLink = wModel.getRoomLink()
            if (roomLink.isEmpty()) {
                Timber.w("[JoinClassWindow] roomLink is empty, closing window")
                csWindowManager.removeWindow(tag)
                return
            }
            // 顯示短網址（若有），複製 / QR code 仍使用完整 roomLink
            tvJoinClassLink.text = wModel.getDisplayRoomLink()

            // QR code
            coroutineScope.launch(Dispatchers.IO) {
                val bitmap = QRCodeUtils.generateQRCodeWithBackground(
                    text = roomLink,
                    qrSize = 1080,
                    bgRadius = 10f.dpToPx()
                )
                withContext(Dispatchers.Main) {
                    bitmap?.let {
                        ivQrCode.setImageBitmap(it)
                        ivQrCodeExpanded.setImageBitmap(it)
                    }
                }
            }

            // Copy link
            ivCopyLink.setOnClickListener {
                wModel.onCopyLink()
            }

            // QR expand / collapse (AC-5)
            ivQrExpand.setOnClickListener {
                llJoinInfo.visibility = View.GONE
                llQrExpandedSection.visibility = View.VISIBLE
            }
            ivQrCollapse.setOnClickListener {
                llQrExpandedSection.visibility = View.GONE
                llJoinInfo.visibility = View.VISIBLE
            }

            // Switch Class
            llSwitchClass.setOnClickListener {
                wModel.onSwitchClass()
            }

            // Guest mode: hide Switch Class (same logic as StudentManagementWindow.llBackToClassList)
            if (wModel.isGuestMode()) {
                llSwitchClass.visibility = View.INVISIBLE
            }

            // Window control buttons
            WindowControlButtonsUiHelper.setup(
                ivClose = ivClose,
                ivMinimizeWindow = ivMinimizeWindow,
                ivToolbarBringToFront = ivToolbarBringToFront,
                windowTag = tag,
                isMvbBound = wModel.isMyViewBoardBound(),
                csWindowManager = csWindowManager,
                coroutineScope = coroutineScope,
                onCloseClick = { csWindowManager.removeWindow(tag) }
            )

            // Spinner entry point (VSFT-8437) — opens MvbSpinnerWindow per VSFT-8430 behavior
            ivSpinner.setOnClickListener {
                coroutineScope.launch(Dispatchers.Main) {
                    MvbSpinnerWindowOpener.open()
                }
            }

            // Plan limit tooltip (AC-8)
            ivStudentInfo.setOnClickListener {
                showPlanLimitTooltip()
            }

            // Switch Class dialog button wiring
            val dismissDialog = { flLeaveClassDialog.visibility = View.GONE }
            btnDialogCancel.setOnClickListener { dismissDialog() }
            ivDialogClose.setOnClickListener { dismissDialog() }
            btnDialogLeave.setOnClickListener { wModel.confirmSwitchClass() }

            // Remove Student dialog button wiring（in-window overlay，與 fl_leave_class_dialog 相同模式）
            val dismissRemoveDialog = {
                flRemoveStudentDialog.isVisible = false
                pendingRemoveStudentInfo = null
            }
            btnRemoveDialogCancel.setOnClickListener { dismissRemoveDialog() }   // AC-4: 點 Cancel → 關閉，無異動
            ivRemoveDialogClose.setOnClickListener { dismissRemoveDialog() }
            btnRemoveDialogConfirm.setOnClickListener {
                val studentInfo = pendingRemoveStudentInfo ?: return@setOnClickListener
                dismissRemoveDialog()   // AC-2/AC-3: 先關閉 dialog，再執行 API
                coroutineScope.launch(Dispatchers.Main) {
                    showStudentLoading(true)
                    val success = wModel.removeStudent(studentInfo.studentId)
                    showStudentLoading(false)
                    val msg = if (success) {
                        context.getString(R.string.join_class_remove_success)
                    } else {
                        context.getString(R.string.join_class_remove_failed)
                    }
                    showToast(msg, success)
                }
            }
        }
    }

    private fun showPlanLimitTooltip() {
        with(binding) {
            // 設定文案：guest mode 與 logged-in 用兩條不同字串（REQ-I18N-004）
            tvPlanLimitTooltip.text = if (wModel.isGuestMode()) {
                context.getString(
                    R.string.join_class_guest_mode_limit,
                    wModel.getMaxStudentCount()
                )
            } else {
                String.format(
                    context.getString(R.string.my_class_info_plan_maximum),
                    accountManager.selectedOrg?.displayPlanName(context),
                    accountManager.selectedOrg?.studentConcurrent.toString()
                )
            }

            // 動態定位：tooltip 右側對齊 student list 右側
            val rootLoc = IntArray(2)
            root.getLocationOnScreen(rootLoc)
            val iconLoc = IntArray(2)
            ivStudentInfo.getLocationOnScreen(iconLoc)
            val panelLoc = IntArray(2)
            llStudentPanel.getLocationOnScreen(panelLoc)

            llPlanLimitTooltip.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val tooltipWidth = llPlanLimitTooltip.measuredWidth
            val tooltipHeight = llPlanLimitTooltip.measuredHeight
            val arrowHalfWidth = binding.ivPlanLimitArrow.layoutParams.width / 2

            val iconCenterX = iconLoc[0] - rootLoc[0] + ivStudentInfo.width / 2
            val iconTopY = iconLoc[1] - rootLoc[1]

            // tooltip 右側對齊 ll_student_panel 外右緣
            val studentListRight = panelLoc[0] - rootLoc[0] + llStudentPanel.width

            // tooltip 右側對齊 student list 右側
            val tooltipLeft = (studentListRight - tooltipWidth).coerceAtLeast(0)

            // 箭頭 leftMargin = ⓘ 中心相對於 tooltip 左側 - 箭頭半寬
            val arrowLeft = (iconCenterX - tooltipLeft - arrowHalfWidth).coerceIn(
                0, tooltipWidth - binding.ivPlanLimitArrow.layoutParams.width
            )

            (llPlanLimitTooltip.layoutParams as ViewGroup.MarginLayoutParams).apply {
                leftMargin = tooltipLeft
                topMargin = (iconTopY - tooltipHeight).coerceAtLeast(0)
            }
            (binding.ivPlanLimitArrow.layoutParams as ViewGroup.MarginLayoutParams).apply {
                leftMargin = arrowLeft
            }
            llPlanLimitTooltip.requestLayout()
            llPlanLimitTooltip.isVisible = true

            // 3 秒後自動消失
            coroutineScope.launch(Dispatchers.IO) {
                kotlinx.coroutines.delay(3000)
                withContext(Dispatchers.Main) {
                    llPlanLimitTooltip.isVisible = false
                }
            }
        }
    }

    private fun setupClassCodeTiles(classCode: String) {
        binding.llClassCodeTiles.removeAllViews()
        classCode.forEach { char ->
            val tile = LayoutInflater.from(context)
                .inflate(R.layout.item_class_code_tile, binding.llClassCodeTiles, false) as TextView
            tile.text = char.toString()
            binding.llClassCodeTiles.addView(tile)
        }
    }

    // ── VSFT-7256: Student attendance panel ──────────────────────────────────

    private fun fetchAndInitStudentList() {
        coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { showStudentLoading(true) }
            val success = wModel.fetchStudentInfoList()
            withContext(Dispatchers.Main) {
                showStudentLoading(false)
                if (!success) {
                    showToast(
                        context.getString(R.string.student_list_error_msg_failed_to_find_student),
                        isSuccess = false
                    )
                } else {
                    initStudentList()
                }
            }
        }
    }

    private fun initStudentList() {
        binding.rvStudentList.apply {
            layoutManager = GridLayoutManager(context, STUDENT_GRID_SPAN)
            adapter = JoinClassStudentListAdapter { studentInfo ->
                showRemoveStudentDialog(studentInfo)
            }
        }
        // 在 fetch 成功後才開始 collect，避免 StateFlow 初始值 emptyList() 觸發空狀態
        coroutineScope.launch(Dispatchers.IO) {
            wModel.studentInfoListFlow.collect { list ->
                val displayList = wModel.getDisplayList(list)
                val joined = displayList.count { it.isJoinedClass() }
                val hasPreRoster = displayList.any {
                    it.getParticipationState() == StudentInfo.ParticipationState.NOT_JOINED
                }
                withContext(Dispatchers.Main) {
                    updateStudentCount(joined, wModel.getClassType(), hasPreRoster)
                    updateStudentListVisibility(displayList, hasPreRoster)
                    (binding.rvStudentList.adapter as? JoinClassStudentListAdapter)
                        ?.submitList(displayList.sortedBy { it.serialNumber })
                }
            }
        }
    }

    private fun showStudentLoading(show: Boolean) {
        binding.llStudentLoading.isVisible = show
        if (show) binding.lottieStudentLoading.playAnimation()
        else binding.lottieStudentLoading.cancelAnimation()
    }

    private fun updateStudentCount(joined: Int, classType: ClassType, hasPreRoster: Boolean) {
        binding.tvStudentCount.text = if (classType.showFractionCount(hasPreRoster)) {
            context.getString(
                R.string.join_class_student_count_fraction,
                joined,
                wModel.getMaxStudentCount()
            )
        } else {
            context.getString(R.string.join_class_student_count, joined)
        }
    }

    private fun updateStudentListVisibility(list: List<StudentInfo>, hasPreRoster: Boolean) {
        val joined = list.count { it.isJoinedClass() }
        val hasJoining = list.any { it.getParticipationState() == StudentInfo.ParticipationState.JOINING }
        val showEmpty = !hasPreRoster && joined == 0 && !hasJoining
        // 三種狀態互斥：loading / list / empty 同一時間只能有一個可見
        binding.llStudentLoading.isVisible = false
        binding.lottieStudentLoading.cancelAnimation()
        binding.llEmptyState.isVisible = showEmpty
        binding.rvStudentList.isVisible = !showEmpty
    }

    private fun showRemoveStudentDialog(studentInfo: StudentInfo) {
        if (!wModel.isStudentExisted(studentInfo)) {
            // 學生已先行離開（race condition）→ 直接顯示錯誤 toast，不開 dialog
            showToast(
                context.getString(
                    R.string.student_list_error_msg_student_leave,
                    studentInfo.getActualDisplayName(context)
                ),
                isSuccess = false
            )
            return
        }
        // 記錄待確認的學生，confirm 按鈕點擊時使用（button wiring 在 initView()）
        pendingRemoveStudentInfo = studentInfo
        binding.flRemoveStudentDialog.isVisible = true
    }

    private fun showToast(message: CharSequence, isSuccess: Boolean) {
        coroutineScope.launch(Dispatchers.Main) {
            with(binding) {
                cstToast.setIsSuccess(isSuccess)
                cstToast.setText(message)
                cstToast.isVisible = true
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.delay(3000)
                }
                cstToast.isVisible = false
            }
        }
    }

    // ── VSFT-8256: Network disconnect UI ─────────────────────────────────────
    private fun updateNetworkDisconnectUi(hasNetwork: Boolean) {
        with(binding) {
            // Header 右側互斥切換
            llStudentCountGroup.isVisible = hasNetwork
            llNetworkStatus.isVisible = !hasNetwork

            // Student panel body 第 4 態 + 底部 banner（VSFT-8256）
            llNetworkDisconnectBody.isVisible = !hasNetwork
            llNetworkDisconnectBanner.isVisible = !hasNetwork
            if (!hasNetwork) {
                // 其他三個 body state 全部隱藏
                llStudentLoading.isVisible = false
                lottieStudentLoading.cancelAnimation()
                rvStudentList.isVisible = false
                llEmptyState.isVisible = false

                // Remove student dialog 自動 dismiss（AC-4）
                if (flRemoveStudentDialog.isVisible) {
                    flRemoveStudentDialog.isVisible = false
                    pendingRemoveStudentInfo = null
                }

                // Amplitude event：只送第一次（AC-5）
                if (!hasNetworkDisconnectBodyShownOnce) {
                    hasNetworkDisconnectBodyShownOnce = true
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.RECONNECT_PROMPT_SHOWN).send()
                }
            } else {
                // 網路恢復（AC-3）：studentInfoListFlow 不一定重新 emit，直接用當前值重算 body state
                val currentList = wModel.getDisplayList(wModel.studentInfoListFlow.value)
                val joined = currentList.count { it.isJoinedClass() }
                val hasPreRoster = currentList.any {
                    it.getParticipationState() == StudentInfo.ParticipationState.NOT_JOINED
                }
                updateStudentCount(joined, wModel.getClassType(), hasPreRoster)
                updateStudentListVisibility(currentList, hasPreRoster)
                (rvStudentList.adapter as? JoinClassStudentListAdapter)
                    ?.submitList(currentList.sortedBy { it.serialNumber })
            }
        }
    }
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        private const val STUDENT_GRID_SPAN = 4

        // VSFT-7257: window 尺寸，與 layout/window_join_class.xml 的 root
        // FrameLayout android:layout_width / android:layout_height 對齊。
        private const val WINDOW_WIDTH_DP = 333.33f
        private const val WINDOW_HEIGHT_DP = 565.33f
    }

    /**
     * VSFT-7257: reposition this window so it sits on the opposite side of the
     * myViewBoard main toolbar. Called both for the initial layout (sync from
     * [onViewCreated]) and on every subsequent toolbar position change.
     */
    private fun reposition(position: MvbToolbarPosition) {
        val container = csWindowManager.getWindow(tag) ?: return
        val (screenWidth, _) = DisplayUtils.getScreenSize()
        val newLocation = JoinClassWindowPositioner.calculate(
            toolbarPosition = position,
            screenWidth = screenWidth,
            windowSize = getCurrentSize(),
            horizontalMarginPx = JoinClassWindowPositioner.HORIZONTAL_MARGIN_DP.dpToPx().toInt(),
            whiteboardTopOffsetPx = resolveWhiteboardTopOffsetPx()
        )
        container.getWindowConfig().location = newLocation
        container.floatWindowLayoutParam.x = newLocation.coordinateX
        container.floatWindowLayoutParam.y = newLocation.coordinateY
        container.updateLayoutParam(container.floatWindowLayoutParam)
    }

    /**
     * VSFT-7257: prefer the dynamic mVB-reported whiteboard top edge (sent
     * via `MessageToolbarPositionChanged.payload.whiteboard_top_dp`); fall
     * back to the hardcoded default when older mVB builds don't carry it.
     */
    private fun resolveWhiteboardTopOffsetPx(): Int {
        val dynamicDp = wModel.mvbWhiteboardTopDpFlow.value
        return dynamicDp?.toFloat()?.dpToPx()?.toInt()
            ?: JoinClassWindowPositioner.WHITEBOARD_TOP_OFFSET_DP_FALLBACK.dpToPx().toInt()
    }

    private fun initCollect() {
        // VSFT-7257: subscribe to BOTH toolbar position and whiteboard top
        // edge — either change triggers a reposition. We intentionally do
        // NOT drop the first emission: opener applies the snapshot values at
        // createWindow time, but if mVB pushes MessageToolbarPositionChanged
        // concurrently during the CS bind handshake, the new value may land
        // in the manager between the opener snapshot and this subscribe. The
        // first emission then carries the corrected state; if we dropped it
        // the window would stay at CENTER until the next change.
        // reposition() is idempotent, so the redundant call in the steady-
        // state case (manager values == those used by opener) is harmless.
        coroutineScope.launch(Dispatchers.IO) {
            combine(
                wModel.mvbToolbarPositionFlow,
                wModel.mvbWhiteboardTopDpFlow,
            ) { position, _ -> position }
                .collect { position ->
                    position ?: return@collect
                    withContext(Dispatchers.Main) {
                        reposition(position)
                    }
                }
        }

        // VSFT-8256：network state
        coroutineScope.launch(Dispatchers.IO) {
            wModel.networkAvailabilityFlow.collect { hasNetwork ->
                withContext(Dispatchers.Main) {
                    updateNetworkDisconnectUi(hasNetwork)
                }
            }
        }

        // VSFT-8437：spinner button visibility
        coroutineScope.launch(Dispatchers.IO) {
            wModel.isSpinnerButtonVisible.collect { visible ->
                withContext(Dispatchers.Main) {
                    binding.ivSpinner.isVisible = visible
                }
            }
        }

        uiCollectJob = coroutineScope.launch(Dispatchers.IO) {
            wModel.uiEvent.collect { event ->
                withContext(Dispatchers.Main) {
                    when (event) {
                        JoinClassUIEvent.ShowCopySuccess -> {
                            // Align tooltip center to iv_copy_link center
                            val rootLoc = IntArray(2)
                            binding.root.getLocationOnScreen(rootLoc)
                            val iconLoc = IntArray(2)
                            binding.ivCopyLink.getLocationOnScreen(iconLoc)
                            val iconCenterX = iconLoc[0] - rootLoc[0] + binding.ivCopyLink.width / 2

                            binding.llCopyTooltip.measure(
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                            )
                            val tooltipWidth = binding.llCopyTooltip.measuredWidth
                            (binding.llCopyTooltip.layoutParams as ViewGroup.MarginLayoutParams).apply {
                                leftMargin = (iconCenterX - tooltipWidth / 2).coerceAtLeast(0)
                            }
                            binding.llCopyTooltip.requestLayout()
                            binding.llCopyTooltip.isVisible = true

                            // Auto-hide after 3 seconds
                            coroutineScope.launch(Dispatchers.IO) {
                                kotlinx.coroutines.delay(3000)
                                withContext(Dispatchers.Main) {
                                    binding.llCopyTooltip.isVisible = false
                                }
                            }
                        }

                        JoinClassUIEvent.ShowSwitchClassDialog -> {
                            binding.flLeaveClassDialog.visibility = View.VISIBLE
                        }

                        JoinClassUIEvent.NavigateBackToSelectClass -> {
                            csWindowManager.removeAllWindowsExcept(
                                listOf(WindowTag.TOOLBAR, WindowTag.CS_NORMAL_DIALOG)
                            )
                            if (wModel.isMyViewBoardBound()) {
                                val window: SelectOrgAndSelectClassWindow =
                                    get(SelectOrgAndSelectClassWindow::class.java)
                                CSWindowManager.createWindow(window, Gravity.CENTER)
                            } else {
                                val window: MyClassWindow =
                                    get(MyClassWindow::class.java)
                                CSWindowManager.createWindow(window, Gravity.CENTER)
                            }
                        }
                    }
                }
            }
        }
    }
}
