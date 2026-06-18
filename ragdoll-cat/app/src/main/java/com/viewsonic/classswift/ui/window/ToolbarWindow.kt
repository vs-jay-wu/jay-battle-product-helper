package com.viewsonic.classswift.ui.window

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AppConstants
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowToolbarBinding
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.ui.widget.toolbar.CSToolbarActionButton.ItemState
import com.viewsonic.classswift.ui.widget.toolbar.CSToolbarIconButton
import com.viewsonic.classswift.ui.widget.toolbar.factory.CSToolbarIconButtonDataFactory
import com.viewsonic.classswift.ui.window.leaderboard.LeaderboardWindow
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import com.viewsonic.classswift.uimanager.PushRespondUiManager
import com.viewsonic.classswift.uimanager.QuizUiManager
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.getLocationOnScreenWithoutStatusBar
import com.viewsonic.classswift.utils.extension.mapAndCollect
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnCSWindowChangedListener
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnMotionEventChangedListener
import com.viewsonic.classswift.windowframework.core.utils.LocationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class ToolbarWindow(context: Context) : IWindow<WindowToolbarBinding>, OnCSWindowChangedListener {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val socketManager: SocketManager by inject(SocketManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val toolbarManager: ToolbarManager by inject(ToolbarManager::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider by inject(MyViewBoardConnectionStateProvider::class.java)
    private val onMotionEventChangedListener = object : OnMotionEventChangedListener {
        override fun onMotionEventChanged(previousAction: Int, nextAction: Int) {
            Timber.d("[onMotionEventChanged]: pre -> $previousAction, next ->$nextAction")
            if (nextAction == MotionEvent.ACTION_UP) {
                binding.ivToolbarCollapsedDragArea.isSelected = false
                binding.ivToolbarExpandedDragArea.isSelected = false
            }
        }
    }
    private val iconButtonMap: MutableMap<CSToolbarIconButtonType, CSToolbarIconButton> = mutableMapOf()

    private val observedWindowTagList: List<WindowTag> = listOf(WindowTag.CS_SYSTEM_DIALOG, WindowTag.FORCE_LOGOUT_DIALOG)
    private var expansionStateJob: Job? = null
    private var participationStateJob: Job? = null

    private var isNeedToAdjustPositionToCenterAgain: Boolean = true

    private var socketEventJob: Job? = null

    private var dialogWindow: CSSystemDialogWindow? = null

    override var tag: WindowTag = WindowTag.TOOLBAR

    @SuppressLint("ClickableViewAccessibility")
    override val binding: WindowToolbarBinding = WindowToolbarBinding.inflate(
        LayoutInflater.from(context)
    )

    // 42.67f = 128 / 3
    override var size: SizeInPixels = SizeInPixels(WindowManager.LayoutParams.WRAP_CONTENT, 42.67f.dpToPx().toInt())

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(42.67f.dpToPx().toInt(), View.MeasureSpec.EXACTLY)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onViewCreated() {
        initView()
        initData()
        initCollection()
        toolbarManager.initNetworkStateCollection()
        csWindowManager.addOnWindowChangedListener(this)
    }

    override fun onViewSizeChanged(oldSizeInPixels: SizeInPixels, newSizeInPixels: SizeInPixels) {
        if (toolbarManager.toolbarUiState.value.isExpanded && isNeedToAdjustPositionToCenterAgain) {
            csWindowManager.getWindow(tag)?.let { window ->
                val centerBottomLocation = LocationUtil.gravityToLocation(Gravity.CENTER_BOTTOM, newSizeInPixels).apply {
                    coordinateY -= 23.dpToPx().toInt()
                }
                window.getWindowConfig().location = centerBottomLocation
                window.floatWindowLayoutParam.x = centerBottomLocation.coordinateX
                window.floatWindowLayoutParam.y = centerBottomLocation.coordinateY
                window.updateLayoutParam(window.floatWindowLayoutParam)
                isNeedToAdjustPositionToCenterAgain = false
            }
        }
    }

    override fun onDestroy() {
        toolbarManager.cancelNetworkStateCollection()
        coroutineScope.cancel()
    }

    //when CS_SYSTEM_DIALOG and Force logout dialog show, need to updateLayoutParam.
    override fun onCSWindowCountChanged() {
        observedWindowTagList.forEach { observedTag ->
            if (csWindowManager.getWindow(observedTag) != null) {
                csWindowManager.getWindow(tag)?.let { window ->
                    window.updateLayoutParam(window.floatWindowLayoutParam)
                }
                return@forEach
            }
        }
    }

    override fun onCSWindowHiddenCountChange() = Unit

    fun checkIconButtonSelectedState() {
        iconButtonMap.values.filter { iconButton ->
            iconButton.isVisible
        }.forEach { iconButton ->
            iconButton.checkIfInSelectedState()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        with(binding) {
            ivToolbarCollapsedDragArea.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        ivToolbarCollapsedDragArea.isSelected = true
                        csWindowManager.getWindow(tag)?.setOnMotionEventChangedListener(onMotionEventChangedListener)
                    }
                }
                false
            }
            ivToolbarExpandedDragArea.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        ivToolbarExpandedDragArea.isSelected = true
                        csWindowManager.getWindow(tag)?.setOnMotionEventChangedListener(onMotionEventChangedListener)
                    }
                }
                false
            }

            cstibQuitApp.setOnClickListener {
                toolbarManager.quitApp()
            }

            ivToolbarExpandButton.setOnClickListener {
                toolbarManager.setIsExpanded(true)
            }
            ivToolbarCollapseButton.setOnClickListener {
                toolbarManager.setIsExpanded(false)
            }

            cstabLeaveClass.setOnClickListener {
                leaveClass()
            }

            cstabStartLesson.setOnClickListener {
                cstabStartLesson.setItemState(ItemState.LOADING)
                coroutineScope.launch(Dispatchers.Main) {
                    val isSucceeded = toolbarManager.startLesson()
                    if (isSucceeded) {
                        toolbarManager.setParticipationState(ToolbarManager.ParticipationState.LESSON_STARTED)
                    } else {
                        showStartLessonToast()
                    }
                    cstabStartLesson.setItemState(ItemState.ACTIVE)
                }
                QuizSharedUiInfo.resetMultipleOptions()
            }

            cstabEndLesson.setOnClickListener {
                cstabEndLesson.setItemState(ItemState.LOADING)
                CSSystemDialogWindow.Builder(root.context)
                    .setTitle(root.context.getString(R.string.dialog_buttons_end_in_session))
                    .setMessage(root.context.getString(R.string.dialog_end_lesson_with_return))
                    .setNegativeButton(
                        text = root.context.getString(R.string.common_cancel),
                        color = ContextCompat.getColor(root.context, R.color.color_2E3133),
                        listener = {
                            cstabEndLesson.setItemState(ItemState.ACTIVE)
                            csWindowManager.removeWindow(WindowTag.CS_NORMAL_DIALOG)
                        }
                    )
                    .setPositiveButton(
                        text = root.context.getString(R.string.dialog_buttons_end_lesson),
                        color = ContextCompat.getColor(root.context, R.color.color_F02B2B),
                        listener = {
                            coroutineScope.launch(Dispatchers.Main) {
                                val isSucceeded = toolbarManager.endLesson()
                                if (isSucceeded) {
                                    leaveClass()
                                }
                                cstabEndLesson.setItemState(ItemState.ACTIVE)
                                csWindowManager.removeWindow(WindowTag.CS_NORMAL_DIALOG)
                            }
                        }
                    )
                    .build()
                    .show()
            }
        }
    }

    private fun leaveClass() {
        // leave class should clear socketManager joinLessonMessage
        coroutineScope.launch(Dispatchers.IO) {
            unclosedMissionUiManager.closeUnclosedMissions()
        }
        // Get showLeaderBroad value, before student info list be cleared.
        val showLeaderBoard = toolbarManager.showLeaderBoard()
        socketManager.setJoinLessonMessage(null)
        toolbarManager.resetMultipleQuizSelectionInfos()
        toolbarManager.setParticipationState(ToolbarManager.ParticipationState.NOT_JOINED)
        csWindowManager.removeAllWindowsExcept(listOf(WindowTag.TOOLBAR, WindowTag.CS_NORMAL_DIALOG, WindowTag.WINDOW_QUIZ_COLLECTION))
        csWindowManager.hideWindow(WindowTag.WINDOW_QUIZ_COLLECTION, isRecordHiddenState = true)
        if (myViewBoardConnectionStateProvider.isBound()) {
            val window: SelectOrgAndSelectClassWindow = get(SelectOrgAndSelectClassWindow::class.java)
            CSWindowManager.createWindow(window, Gravity.CENTER)
        } else {
            val window: MyClassWindow = get(MyClassWindow::class.java)
            CSWindowManager.createWindow(window, Gravity.CENTER)
        }
        //Show leaderboard window after show my classs window.
        if (showLeaderBoard) {
            val window: LeaderboardWindow = get(LeaderboardWindow::class.java)
            csWindowManager.createWindow(window, Gravity.CENTER)
        }
    }

    private fun updateActionButton() {
        when (toolbarManager.toolbarUiState.value.participationState) {
            ToolbarManager.ParticipationState.NOT_JOINED -> {
                binding.viewVerticalDivider.isVisible = false
                binding.cstabLeaveClass.isVisible = false
                binding.cstabStartLesson.isVisible = false
                binding.cstabEndLesson.isVisible = false
                binding.llToolbarNoNetworkContainer.isVisible = false
                binding.spaceEnd.isVisible = false
            }

            ToolbarManager.ParticipationState.JOINED -> {
                binding.viewVerticalDivider.isVisible = true
                binding.cstabLeaveClass.isVisible = true
                binding.cstabStartLesson.isVisible = true
                binding.cstabEndLesson.isVisible = false
                binding.llToolbarNoNetworkContainer.isVisible = false
                binding.spaceEnd.isVisible = true
            }

            ToolbarManager.ParticipationState.LESSON_STARTED -> {
                binding.viewVerticalDivider.isVisible = true
                binding.cstabLeaveClass.isVisible = false
                binding.cstabStartLesson.isVisible = false
                binding.cstabEndLesson.isVisible = true
                binding.llToolbarNoNetworkContainer.isVisible = false
                binding.spaceEnd.isVisible = true
            }

            ToolbarManager.ParticipationState.NETWORK_DISCONNECT -> {
                Timber.tag("ToolBarNetworkState").d("ToolbarWindow set NETWORK_DISCONNECT UI")
                binding.viewVerticalDivider.isVisible = true
                binding.cstabLeaveClass.isVisible = false
                binding.cstabStartLesson.isVisible = false
                binding.cstabEndLesson.isVisible = false
                binding.llToolbarNoNetworkContainer.isVisible = true
                binding.spaceEnd.isVisible = false
            }
        }
        updateFloatingWindowWidthIfChanged()
    }

    private fun initData() {
        with(binding) {
            iconButtonMap.clear()
            iconButtonMap.putAll(
                mapOf(
                    CSToolbarIconButtonType.CLASS_LIST to cstibClassList,
                    CSToolbarIconButtonType.CLASS to cstibClass,
                    CSToolbarIconButtonType.QUIZ to cstibQuiz,
                    CSToolbarIconButtonType.PRESET to cstibPreset,
                    CSToolbarIconButtonType.PUSH to cstibPush,
                    CSToolbarIconButtonType.TOOLS to cstibTools,
                    CSToolbarIconButtonType.INSTANT_PUSH to cstibInstantPush,
                    CSToolbarIconButtonType.SETTINGS to cstibSettings,
                    CSToolbarIconButtonType.QUIT_APP to cstibQuitApp,
                )
            )
        }
        toolbarManager.setPlan()
    }

    private fun initCollection() {
        expansionStateJob?.cancel()
        expansionStateJob = coroutineScope.launch(Dispatchers.IO) {
            toolbarManager.toolbarUiState.mapAndCollect({ isExpanded }) { isExpanded ->
                withContext(Dispatchers.Main) {
                    when (isExpanded) {
                        true -> {
                            binding.llToolbarCollapsedContainer.isVisible = false
                            binding.clToolbarExpandedContainer.isVisible = true
                        }

                        false -> {
                            WindowTag.getSubWindowTagList().forEach {
                                CSWindowManager.removeWindow(it)
                            }
                            binding.clToolbarExpandedContainer.isVisible = false
                            binding.llToolbarCollapsedContainer.isVisible = true
                        }
                    }
                    updateFloatingWindowWidthIfChanged()
                }
            }
        }
        participationStateJob?.cancel()
        participationStateJob = coroutineScope.launch(Dispatchers.IO) {
            toolbarManager.toolbarUiState.mapAndCollect({ isPremiumUser to participationState }) { (isPremiumUser, participationState) ->
                Timber.tag("ToolBarNetworkState").d("collect toolbarUiState: $participationState")
                withContext(Dispatchers.Main) {
                    WindowTag.getSubWindowTagList().forEach {
                        CSWindowManager.removeSubWindow(it)
                    }
                    CSToolbarIconButtonDataFactory.createMap(
                        isPremiumUser,
                        participationState
                    ).forEach { (buttonType, buttonData) ->
                        iconButtonMap[buttonType]?.setButtonData(buttonData)
                    }
                    updateActionButton()
                    dialogWindow?.setIsNetworkDisconnect(participationState == ToolbarManager.ParticipationState.NETWORK_DISCONNECT)
                    //if state is network disconnect, should dismiss non system dialog.
                    if (participationState == ToolbarManager.ParticipationState.NETWORK_DISCONNECT) {
                        if (dialogWindow?.isSystemDialog() == true) {
                            return@withContext
                        }
                        dialogWindow?.dismiss()
                    }
                }
            }
        }
        socketEventJob?.cancel()
        socketEventJob = coroutineScope.launch(Dispatchers.IO) {
            socketManager.receivedEventDataFlow.distinctUntilChanged().collect { receivedEventData ->
                when (receivedEventData.event) {
                    // received force logout from hub system, show dialog inform user
                    SocketManager.ReceivedEvent.EVENT_TEACHER_FORCE_LOGOUT -> {
                        val context = binding.root.context
                        withContext(Dispatchers.Main) {
                            csWindowManager.removeWindow(WindowTag.FORCE_LOGOUT_DIALOG)
                            dialogWindow = CSSystemDialogWindow.Builder(context, WindowTag.FORCE_LOGOUT_DIALOG)
                                .setTitle(context.getString(R.string.common_notice))
                                .setMessage(context.getString(R.string.dialog_permission_error_logged_out_admin))
                                .setPositiveButton(
                                    context.getString(R.string.common_confirm),
                                    context.getColor(R.color.neutral_900)
                                ) {
                                    if (dialogWindow?.isNetworkDisconnect() == true) {
                                        toolbarManager.quitApp()
                                    } else {
                                        toolbarManager.logOut()
                                        dialogWindow?.dismiss()
                                    }
                                }
                                .build()
                            dialogWindow?.show()
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun updateFloatingWindowWidthIfChanged() {
        csWindowManager.getWindow(tag)?.let { window ->
            window.updateLayoutParam(window.floatWindowLayoutParam)
        }
    }

    private fun showStartLessonToast() {
        val context = binding.root.context
        val location = binding.cstabStartLesson.getLocationOnScreenWithoutStatusBar()

        val toolBarLocation =  binding.root.getLocationOnScreenWithoutStatusBar()

        val toast = ToastWindow.MakeText(
            context,
            context.getString(R.string.my_class_error_msg_start_lesson),
            AppConstants.THREE_SEC_DELAY
        ).build()
        val size = toast.getCurrentSize()
        val toastWidth = size.width
        val toastHeight = size.height

        val buttonCenterX = location.first + (binding.cstabStartLesson.width / 2)
        val toolBarTopY = toolBarLocation.second

        val toastX = buttonCenterX - (toastWidth / 2)
        val toastY = toolBarTopY - toastHeight - 8.6f.toInt()

        val toastLocation = Location(coordinateX = toastX, coordinateY = toastY)

        toast.show(toastLocation)
    }

    enum class CSToolbarIconButtonType {
        CLASS_LIST,
        CLASS,
        QUIZ,
        PRESET,
        PUSH,
        TOOLS,
        INSTANT_PUSH,
        SETTINGS,
        QUIT_APP
    }
}