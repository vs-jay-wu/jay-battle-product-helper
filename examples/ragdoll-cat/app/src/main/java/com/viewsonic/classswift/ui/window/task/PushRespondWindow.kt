package com.viewsonic.classswift.ui.window.task

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.response.UpdateTaskResult
import com.viewsonic.classswift.coordinator.RecordsCoordinator
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.databinding.WindowPushRespondBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.task.content.ContentTaskWidget
import com.viewsonic.classswift.ui.widget.task.enums.TaskTabView
import com.viewsonic.classswift.ui.widget.task.records.RecordMarkWidget.RecordMarkWidgetEventListener
import com.viewsonic.classswift.ui.widget.task.records.RecordsTaskWidget
import com.viewsonic.classswift.ui.windowmodel.task.PushRespondWindowModel
import com.viewsonic.classswift.ui.windowmodel.task.state.PushRespondEvent
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class PushRespondWindow(
    private val context: Context,
) : IWindow<WindowPushRespondBinding> {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val windowModel: PushRespondWindowModel by inject(PushRespondWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var autoTabJob: Job? = null

    override var tag: WindowTag = WindowTag.PUSH_RESPOND
    override var size: SizeInPixels = SizeInPixels(1080f.dpToPx().toInt(), 588f.dpToPx().toInt())

    override val binding: WindowPushRespondBinding = WindowPushRespondBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )

    fun bringToTop() {
        coroutineScope.launch(Dispatchers.Main) {
            csWindowManager.bringWindowToTop(tag)
        }
    }

    override fun onViewCreated() {
        createTabs()
        initUrlMetaPreviewDialog()
        initClickedAction()
        initWindowModel()
        initContentWidgetAndRecordWidget()
        initRecordMaskWidget()
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        windowModel.onCleared()
        binding.csContentWidget.release()
        binding.csrmwRecordMaskWidget.release()
        super.onDestroy()
    }

    private fun initClickedAction() {

        with(binding) {
            WindowControlButtonsUiHelper.setup(
                ivClose = ibClose,
                ivMinimizeWindow = ivMinimizeWindow,
                ivToolbarBringToFront = ivToolbarBringToFront,
                windowTag = tag,
                isMvbBound = windowModel.isMyViewBoardBound(),
                csWindowManager = csWindowManager,
                coroutineScope = coroutineScope,
                onCloseClick = {
                    windowModel.closeWindow()
                    unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.PUSH_AND_RESPOND_TASK)
                },
                onAfterMinimize = { unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.PUSH_AND_RESPOND_TASK) }
            )

            ndvNetworkDisconnectMask.setOnClickListener {
                coroutineScope.launch(Dispatchers.Main) {
                    csWindowManager.let {
                        it.bringWindowToTop(tag)
                        it.bringWindowToTop(WindowTag.TOOLBAR)
                    }
                }
            }
        }
    }

    private fun initWindowModel() {
        coroutineScope.launch {
            windowModel.uiEventFlow.collect { event ->
                withContext(Dispatchers.Main) {
                    handleUiEventUpdate(event = event)
                }
            }
        }
    }


    private fun handleUiEventUpdate(event: PushRespondEvent) {
        when (event) {
            is PushRespondEvent.QuizRespondConflict -> {
                binding.csRecordsWidget.preloadData()
            }

            is PushRespondEvent.NetworkStatusChange -> {
                handleNetworkStatusChange(
                    isNetworkConnected = event.isNetworkConnected
                )
            }
            is PushRespondEvent.UpdateStudentRecordList -> {
                binding.csrmwRecordMaskWidget.refreshRecordData(event.recordList)
            }
            PushRespondEvent.GetRecordListFailed -> {
                with(binding) {
                    csrmwRecordMaskWidget.let {
                        if (it.isShownOnScreen()) {
                            it.dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun handleNetworkStatusChange(isNetworkConnected: Boolean) {
        binding.ndvNetworkDisconnectMask.isVisible = !isNetworkConnected
        if(isNetworkConnected) {
            binding.csRecordsWidget.preloadData()
        }
    }

    private fun initContentWidgetAndRecordWidget() {

        with(binding) {
            csRecordsWidget.preloadData()

            csContentWidget.let {

                // Setting the window tag is necessary for proper focus handling in floating windows.
                // For implementation reference, see UrlMetaPreviewDialog::initEditText().
                it.setContainerWindowTag(tag = tag)
                it.init()
                it.setUrlMetaPreviewDialogWidget(dialog = umdUrlMetaPreviewDialog)
                it.setContentTaskWidgetEventListener(
                    object : ContentTaskWidget.ContentTaskWidgetEventListener {

                        override fun onEndQuizAndBatchQuizzesTipDialogShow(onConfirm: () -> Unit, onCancel: () -> Unit) {
                            setupEndQuizAndBatchQuizzesTipDialog(onConfirm = onConfirm, onCancel = onCancel)
                            csMessageDialog.show()
                        }

                        override fun onEndQuizAndBatchQuizzesTipDialogDismiss() {
                            csMessageDialog.apply {
                                setPositiveLoading(isLoading = false)
                                setNegativeButtonEnabled(enabled = true)
                                dismiss()
                            }
                        }

                        override fun onToastShow(isError: Boolean, message: String) {
                            showToast(isError = isError, message = message)
                        }

                        override fun onStartPushTask() {
                            autoTabJob?.let { job ->
                                if (job.isActive) {
                                    job.cancel()
                                }
                            }
                        }

                        override fun onPushTaskCompleted() {
                            binding.csRecordsWidget.preloadData()
                            startAutoSwitchTabs(binding.tlTabs)
                        }

                        override fun onPushTaskPartialCompleted() {
                            binding.csRecordsWidget.preloadData()
                            startAutoSwitchTabs(binding.tlTabs)
                        }
                    })
            }
            csRecordsWidget.setRecordTaskWidgetEventListener(
                object : RecordsTaskWidget.RecordTaskWidgetEventListener {
                    override fun onSwitchContentMessageDialogShow(
                        onConfirm: () -> Unit,
                        onCancel: () -> Unit
                    ) {
                        setupSwitchContentDialog(onConfirm = onConfirm, onCancel = onCancel)
                        csMessageDialog.show()
                    }

                    override fun onSwitchContentMessageDialogClose() {
                        csMessageDialog.dismiss()
                    }

                    override fun onEndTaskDialogShow(onConfirm: () -> Unit, onCancel: () -> Unit) {
                        setupEndTaskDialog(
                            isEndAll = false,
                            onConfirm = onConfirm,
                            onCancel = onCancel
                        )
                        csMessageDialog.show()
                    }

                    override fun onEndTaskDialogClose() {
                        with(csMessageDialog) {
                            setNegativeButtonEnabled(enabled = true)
                            setPositiveButtonEnabled(enabled = true)
                            setPositiveLoading(isLoading = false)
                            dismiss()
                        }
                    }

                    override fun onEndAllTaskDialogShow(onConfirm: () -> Unit, onCancel: () -> Unit) {
                        setupEndTaskDialog(
                            isEndAll = true,
                            onConfirm = onConfirm,
                            onCancel = onCancel
                        )
                        csMessageDialog.show()
                    }

                    override fun onEndAllTaskDialogClose() {
                        with(csMessageDialog) {
                            setNegativeButtonEnabled(enabled = true)
                            setPositiveButtonEnabled(enabled = true)
                            setPositiveLoading(isLoading = false)
                            dismiss()
                        }
                    }

                    override fun onRecordMaskWidgetShow(infoIndex: Int) {
                        csrmwRecordMaskWidget.apply {
                            setSelectedIndex(infoIndex)
                            show()
                        }
                    }

                    override fun onNotSubmittedRecordClick() {
                        val message = context.getString(
                            R.string.push_and_respond_error_msg_not_submitted
                        )
                        showToast(isError = true, message = message)
                    }

                    override fun onRecordMaskWidgetClose() {
                        csrmwRecordMaskWidget.dismiss()
                    }

                    override fun onRecordMaskUpdateFailed(failedData: List<UpdateTaskResult>) {
                        csrmwRecordMaskWidget.onMarkUpdateResultError(failedData = failedData)
                    }
                    override fun onLabelAsMarkResultUpdate(successCount: Int, failedCount: Int) {
                        if (successCount == 0 && failedCount == 0) {
                            val message = context.getString(
                                R.string.error_msg_label_as_mark,
                            )
                            showToast(isError = true, message = message)
                        }

                        if (successCount > 0 && failedCount == 0) {
                            val message = context.getString(
                                R.string.success_msg_task_mark_success,
                                successCount.toString()
                            )
                            showToast(isError = false, message = message)
                        }

                        if (successCount > 0 && failedCount > 0) {
                            val message = context.getString(
                                R.string.error_msg_task_partial_failed,
                                successCount.toString(),
                                failedCount.toString()
                            )
                            showToast(isError = true, message = message)
                        }

                        if (successCount == 0 && failedCount > 0) {
                            val message = context.getString(
                                R.string.error_msg_task_mark_all_failed,
                                failedCount.toString()
                            )
                            showToast(isError = true, message = message)
                        }
                    }

                    override fun onLabelAsMarkUnknownError() {
                        val message = context.getString(
                            R.string.error_msg_label_as_mark,
                        )
                        showToast(isError = true, message = message)
                    }

                    override fun onMarkUnknownError() {
                        binding.csrmwRecordMaskWidget.onMarkUnknownError()
                    }

                    override fun onPushRecordsAllCompleted(successCount: Int) {
                        val message = context.getString(
                            R.string.push_and_respond_success_msg_push_all_successful,
                            successCount
                        )
                        showToast(isError = false, message = message)
                    }

                    override fun onPushRecordsFailed(successCount: Int, failedCount: Int) {
                        val message = if (successCount == 0) {
                            context.getString(
                                R.string.push_and_respond_error_msg_push_all_failed,
                                failedCount
                            )
                        } else {
                            context.getString(
                                R.string.push_and_respond_error_msg_push_partial_failed,
                                successCount,
                                failedCount
                            )
                        }

                        showToast(isError = true, message = message)
                    }

                    override fun onEndTaskAllCompleted(successCount: Int) {
                        val message = context.getString(
                            R.string.success_msg_task_end_success,
                            successCount
                        )
                        showToast(isError = false, message = message)
                    }

                    override fun onEndTaskFailed(successCount: Int, failedCount: Int) {
                        val message = if (successCount == 0) {
                            context.getString(
                                R.string.error_msg_task_end_all_failed,
                                failedCount
                            )
                        } else {
                            context.getString(
                                R.string.error_msg_task_end_partial_failed,
                                successCount,
                                failedCount
                            )
                        }

                        showToast(isError = true, message = message)
                    }

                    override fun setRecordsCoordinator(coordinator: RecordsCoordinator) {
                        windowModel.setRecordsCoordinator(coordinator)
                    }
                })
        }
    }

    private fun initRecordMaskWidget() {
        with(binding) {
            csrmwRecordMaskWidget.apply {
                setRecordMarkWidgetEventListener(
                    object : RecordMarkWidgetEventListener {
                        override fun onSendMarkedResult(data: TaskResultInfo.Content) {
                            csRecordsWidget.onSendMarkedResult(data = data)
                        }

                        override fun onClose() {
                            dismiss()
                        }

                        override fun onToastShow(message: String) {
                            showToast(isError = true, message = message)
                        }

                        override fun onRefetchStudentRecord(data: TaskResultInfo) {
                            windowModel.getStudentTaskResult(data.studentId)
                        }
                    })
            }
        }
    }

    private fun createTabs() {
        val tabTitles = context.resources.getStringArray(
            R.array.push_respond_tabs
        )

        with(binding.tlTabs) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = index == 0
                val tab = newTab().apply {
                    customView = TaskTabView(context).apply {
                        setTitle(title)
                        setSelectedStatus(isSelected)
                    }
                }
                addTab(tab, isSelected)
            }

            removeTabItemSpacing()

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {

                    tab?.let {
                        autoTabJob?.cancel()
                        handleTabSelected(position = it.position)

                        it.customView.let { customView ->
                            if (customView is TaskTabView) {
                                customView.setSelectedStatus(isSelected = true)
                            }
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    tab?.customView.let {
                        if (it is TaskTabView) {
                            it.setSelectedStatus(isSelected = false)
                        }
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            })
        }
    }

    /**
     * Removes the default spacing between tabs in TabLayout.
     *
     * Each tab in TabLayout comes with built-in horizontal padding or margin,
     * which can create unwanted spacing when using custom tab views.
     * This method applies negative start and end margins to each tab view
     * to visually cancel out that default spacing, resulting in tighter alignment.
     */
    private fun removeTabItemSpacing() {
        val tabStrip = binding.tlTabs.getChildAt(0) as ViewGroup
        for (i in 0 until tabStrip.childCount) {
            val tabView = tabStrip.getChildAt(i)
            tabView.setPadding(0, 0, 0, 0)
            val params = tabView.layoutParams as ViewGroup.MarginLayoutParams

            // NOTE: This is a workaround for TabLayout's internal spacing behavior.
            params.marginStart = -27f.dpToPx().toInt()
            params.marginEnd = -27f.dpToPx().toInt()
            tabView.layoutParams = params
        }
        binding.tlTabs.requestLayout()
    }

    private fun handleTabSelected(position: Int) {
        with(binding) {
            when (position) {
                0 -> {
                    csContentWidget.visibility = View.VISIBLE
                    csRecordsWidget.visibility = View.INVISIBLE
                }

                1 -> {
                    csContentWidget.visibility = View.INVISIBLE
                    csRecordsWidget.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showToast(isError: Boolean, message: String) {
        Timber.d("ShowToast[isError: $isError, Message: $message]")
        coroutineScope.launch(Dispatchers.Main) {
            with(binding.cstToast) {
                setText(message)
                setIsSuccess(isSuccess = !isError)

                visibility = View.VISIBLE
                withContext(Dispatchers.IO) {
                    delay(3000L)
                }
                visibility = View.GONE
            }
        }
    }

    private fun setupEndQuizAndBatchQuizzesTipDialog(
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        with(context) {
            binding.csMessageDialog.apply {
                val nextMissionName = context.getString(R.string.push_and_respond_task)

                setTitle(context.getString(R.string.ongoing_mission_conflict_title))
                setMessage(context.getString(R.string.ongoing_mission_conflict_message, nextMissionName))
                setPositiveButtonText(context.getString(R.string.ongoing_mission_conflict_positive_title, nextMissionName))

                setButtonClickListeners(
                    onPositive = {
                        setPositiveLoading(isLoading = true)
                        setNegativeButtonEnabled(enabled = false)
                        onConfirm.invoke()
                    },
                    onNegative = {
                        onCancel.invoke()
                    }
                )

                setMaskClickedListener {
                    coroutineScope.launch(Dispatchers.Main) {
                        csWindowManager.bringWindowToTop(windowTag = WindowTag.PUSH_RESPOND)
                    }
                }
            }
        }
    }

    private fun setupSwitchContentDialog(
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        with(context) {
            binding.csMessageDialog.apply {
                setTitle(getString(R.string.dialog_switch_content_title))
                setMessage(getString(R.string.dialog_switch_content_message))
                setPositiveButtonText(getString(R.string.dialog_switch_content_action_leave))
                setNegativeButtonText(getString(R.string.dialog_switch_content_action_stay))
                setPositiveButtonTextColor(
                    ContextCompat.getColor(context, R.color.records_label_as_mark_dialog_positive_text_color)
                )

                setButtonClickListeners(
                    onPositive = {
                        onConfirm.invoke()
                    },
                    onNegative = {
                        onCancel.invoke()
                    }
                )

                setMaskClickedListener {
                    coroutineScope.launch(Dispatchers.Main) {
                        csWindowManager.bringWindowToTop(windowTag = WindowTag.PUSH_RESPOND)
                    }
                }
            }
        }
    }

    private fun initUrlMetaPreviewDialog() {
        binding.umdUrlMetaPreviewDialog.apply {
            setContainerWindowTag(WindowTag.PUSH_RESPOND)
        }
    }

    private fun setupEndTaskDialog(
        isEndAll: Boolean,
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        with(context) {
            binding.csMessageDialog.apply {

                val titleResId = if (isEndAll) {
                    R.string.dialog_end_all_tasks_title
                } else {
                    R.string.dialog_end_task_title
                }

                val messageResId = if (isEndAll) {
                    R.string.dialog_end_all_tasks_message
                } else {
                    R.string.dialog_end_task_message
                }

                val positiveTextResId = if (isEndAll) {
                    R.string.dialog_action_end_all_tasks
                } else {
                    R.string.dialog_action_end_task
                }

                setTitle(getString(titleResId))
                setMessage(getString(messageResId))
                setPositiveButtonText(getString(positiveTextResId))
                setNegativeButtonText(getString(R.string.common_cancel))
                setPositiveButtonTextColor(
                    ContextCompat.getColor(context, R.color.records_end_task_dialog_positive_text_color)
                )

                setButtonClickListeners(
                    onPositive = {
                        setPositiveLoading(isLoading = true)
                        setNegativeButtonEnabled(enabled = false)
                        onConfirm.invoke()
                    },
                    onNegative = {
                        onCancel.invoke()
                    }
                )

                setMaskClickedListener {
                    coroutineScope.launch {
                        csWindowManager.bringWindowToTop(windowTag = WindowTag.PUSH_RESPOND)
                    }
                }
            }
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun startAutoSwitchTabs(tabLayout: TabLayout) {
        autoTabJob?.cancel()

        autoTabJob = coroutineScope.launch(Dispatchers.Main) {
            val tabCount = tabLayout.tabCount
            if (tabCount == 0) return@launch

            var currentIndex = tabLayout.selectedTabPosition

            val tickerChannel = ticker(
                delayMillis = 800,
                initialDelayMillis = 800L
            )

            val event = tickerChannel.receiveCatching().getOrNull()
            event?.let {
                currentIndex = (currentIndex + 1) % tabCount
                tabLayout.selectTab(tabLayout.getTabAt(currentIndex))
            }
            tickerChannel.cancel()
        }
    }
}