package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.response.UpdateTaskResult
import com.viewsonic.classswift.coordinator.RecordsCoordinator
import com.viewsonic.classswift.data.info.RecordGroupInfo
import com.viewsonic.classswift.data.records.RecordListInfo
import com.viewsonic.classswift.data.records.TaskListUpdateInfo
import com.viewsonic.classswift.data.task.PopupOptionInfo
import com.viewsonic.classswift.data.task.RecordEventInfo
import com.viewsonic.classswift.data.task.TaskApiResult
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.data.task.TaskStatusInfo
import com.viewsonic.classswift.databinding.WidgetRecordsTaskBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.task.content.TaskItemDecoration
import com.viewsonic.classswift.ui.widget.task.enums.RecordType
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus
import com.viewsonic.classswift.ui.widget.task.enums.TaskStatus
import com.viewsonic.classswift.ui.widget.task.records.RecordGroupAdapter.RecordGroupItemEventListener
import com.viewsonic.classswift.ui.widget.task.records.RecordSeatListAdapter.RecordItemEventListener
import com.viewsonic.classswift.ui.widgetmodel.records.RecordsTaskWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.records.state.RecordUiEvent
import com.viewsonic.classswift.ui.widgetmodel.records.state.RecordsUiState
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.formatAsTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class RecordsTaskWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    RecordListAdapter.TaskItemEventListener, RecordItemEventListener, RecordGroupItemEventListener {

    private val sortOptionGroup = context.getString(R.string.common_group)
    private val sortOptionNumber = context.getString(R.string.push_and_respond_sort_number)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val widgetModel: RecordsTaskWidgetModel by inject(RecordsTaskWidgetModel::class.java)
    private var listAdapter: RecordListAdapter? = null
    private var groupAdapter: RecordGroupAdapter? = null
    private var seatAdapter: RecordSeatListAdapter? = null
    private var eventListener: RecordTaskWidgetEventListener? = null
    private var currentSortOption: String = sortOptionNumber
    private var currentResultTaskId = ""

    private val binding: WidgetRecordsTaskBinding = WidgetRecordsTaskBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    interface RecordTaskWidgetEventListener {
        fun onSwitchContentMessageDialogShow(onConfirm: () -> Unit, onCancel: () -> Unit)
        fun onSwitchContentMessageDialogClose()
        fun onEndTaskDialogShow(onConfirm: () -> Unit, onCancel: () -> Unit)
        fun onEndTaskDialogClose()
        fun onEndAllTaskDialogShow(onConfirm: () -> Unit, onCancel: () -> Unit)
        fun onEndAllTaskDialogClose()
        fun onRecordMaskWidgetShow(infoIndex: Int)
        fun onNotSubmittedRecordClick()
        fun onRecordMaskWidgetClose()
        fun onRecordMaskUpdateFailed(failedData: List<UpdateTaskResult>)
        fun onLabelAsMarkResultUpdate(successCount: Int, failedCount: Int)
        fun onLabelAsMarkUnknownError()
        fun onMarkUnknownError()
        fun onPushRecordsAllCompleted(successCount: Int)
        fun onPushRecordsFailed(successCount: Int, failedCount: Int)
        fun onEndTaskAllCompleted(successCount: Int)
        fun onEndTaskFailed(successCount: Int, failedCount: Int)

        fun setRecordsCoordinator(coordinator: RecordsCoordinator)
    }

    init {

        initSortPopupOptionSelector()
        initRecyclerView()
        preloadPointChangeSoundEffect()
        initClickAction()
        observeUiState()
    }

    fun preloadData() {
        getRecordList()
    }

    fun setRecordTaskWidgetEventListener(listener: RecordTaskWidgetEventListener) {
        eventListener = listener
        eventListener?.setRecordsCoordinator(widgetModel.getRecordsCoordinator())
    }

    fun onSendMarkedResult(data: TaskResultInfo.Content) {
        widgetModel.sendMarkedResult(data = data)
    }

    //RecordListAdapter #TaskItemEventListener
    override fun onTaskListItemSelected(task: RecordListInfo.TaskItem) {
        Timber.d("onTaskSelected= ${task.id}, type = ${task.taskType}")
        resetAllHintView()

        val taskId = task.id
        val taskType = task.taskType
        val taskStatus = task.taskStatus

        if (taskId.isEmpty()) return

        currentResultTaskId = taskId
        val isLabelAsMarkedMode = widgetModel.isLabelAsMarkMode()
        val isPushRecordMode = widgetModel.isPushRecordMode()
        val hasSelected = widgetModel.getSelectedCount()

        handleToolbarLabelAsMarkedButtonVisible(type = taskType)
        handleToolbarPushRecordButtonVisible(type = taskType, taskStatus = taskStatus)

        if (isLabelAsMarkedMode) {
            if (hasSelected > 0) {
                eventListener?.onSwitchContentMessageDialogShow(
                    onConfirm = {
                        clearRecord()
                        binding.clLabelAsMarked.isVisible = false
                        showLabelAsMarkedButton(isShow = false)
                        widgetModel.enableLabelAsMarkedMode(isEnable = false, isNeedUpdateData = true)

                        eventListener?.onSwitchContentMessageDialogClose()
                        loadTask(taskId = taskId)
                    },
                    onCancel = { eventListener?.onSwitchContentMessageDialogClose() }
                )
                return
            } else {
                clearRecord()
                binding.clLabelAsMarked.isVisible = false
                showLabelAsMarkedButton(isShow = false)
                widgetModel.enableLabelAsMarkedMode(isEnable = false, isNeedUpdateData = true)

                loadTask(taskId = taskId)
                return
            }
        }

        if (isPushRecordMode) {
            if (hasSelected > 0) {
                eventListener?.onSwitchContentMessageDialogShow(
                    onConfirm = {
                        clearRecord()
                        binding.clPushRecord.isVisible = false
                        showPushRecordButtonPanel(isShow = false)
                        widgetModel.enablePushRecordMode(isEnable = false, isNeedUpdateData = true)

                        eventListener?.onSwitchContentMessageDialogClose()
                        loadTask(taskId = taskId)

                    },
                    onCancel = { eventListener?.onSwitchContentMessageDialogClose() }
                )
                return
            } else {
                clearRecord()
                binding.clPushRecord.isVisible = false
                showPushRecordButtonPanel(isShow = false)
                widgetModel.enablePushRecordMode(isEnable = false, isNeedUpdateData = true)

                loadTask(taskId = taskId)
                return
            }
        }

        clearRecord()
        loadTask(taskId = taskId)
        return
    }

    private fun clearRecord() {
        seatAdapter?.clearAll()
        groupAdapter?.clearAll()
    }

    //RecordListAdapter #TaskItemEventListener
    override fun onTaskListInitSelected(task: RecordListInfo.TaskItem) {
        Timber.d("onTaskListInitSelected task id = ${task.id}, task type = ${task.taskType}")
        resetAllHintView()

        val taskId = task.id
        val taskType = task.taskType
        val taskStatus = task.taskStatus

        binding.rvRecordList.scrollToPosition(0)

        if (taskType == RecordType.LINK) {
            binding.tvLabelAsMarked.isVisible = false
            binding.tvPushRecord.isVisible = false
        }
        showLoading(isFullScreen = false, isShow = true)
        handleToolbarPushRecordButtonVisible(type = taskType, taskStatus = taskStatus)
        widgetModel.getTaskResultById(taskId = taskId)
    }

    //RecordListAdapter #TaskItemEventListener
    override fun onAdapterDataCommited() {
        scrollToCurrentTask()
    }

    private fun loadTask(taskId: String) {
        val currentFocusTaskId = listAdapter?.getCurrentSelectedTaskId() ?: ""
        widgetModel.stopTimerByTaskId(taskId = currentFocusTaskId)
        val isIgnore = listAdapter?.toggleSelectedState(taskId = taskId) ?: false
        showLoading(isFullScreen = false, isShow = isIgnore)
        widgetModel.getTaskResultById(taskId = taskId)
    }

    private fun preloadPointChangeSoundEffect() {
        widgetModel.preloadPointChangeSoundEffect(
            addPointResId = R.raw.sound_effect_add_point,
            subtractPointResId = R.raw.sound_effect_minus_point
        )
    }

    private fun initSortPopupOptionSelector() {

        context?.let {

            val sortOptions = it.resources.getStringArray(
                R.array.push_respond_sort_options
            )

            val optionList = mutableListOf<PopupOptionInfo>()
            sortOptions.forEachIndexed { index, option ->
                optionList.add(
                    PopupOptionInfo(
                        index = index,
                        title = option,
                        isSelected = index == 0
                    )
                )
            }

            binding.ppsvSortOptions.apply {
                setOptions(options = optionList)
                setSelectedListener { option ->
                    handleSortConditionChanged(option = option)
                }
            }
        }
    }

    private fun initClickAction() {
        with(binding) {
            llSort.setOnClickListener {
                ppsvSortOptions.show(offsetY = 10f.dpToPx().toInt())
            }

            tvLabelAsMarked.setOnClickListener {
                clLabelAsMarked.isVisible = true
                showLabelAsMarkedButton(isShow = true)
                widgetModel.enableLabelAsMarkedMode(isEnable = true, isNeedUpdateData = true)
            }

            tvCancelLabelAsMarked.setOnClickListener {
                clLabelAsMarked.isVisible = false
                showLabelAsMarkedButton(isShow = false)
                widgetModel.enableLabelAsMarkedMode(isEnable = false, isNeedUpdateData = true)
            }

            tvPushRecord.setOnClickListener {
                clPushRecord.isVisible = true
                showPushRecordButtonPanel(isShow = true)
                widgetModel.enablePushRecordMode(isEnable = true, isNeedUpdateData = true)
            }

            tvCancelPushRecord.setOnClickListener {
                clPushRecord.isVisible = false
                showPushRecordButtonPanel(isShow = false)
                widgetModel.enablePushRecordMode(isEnable = false, isNeedUpdateData = true)
            }

            tvClearMarked.setOnClickListener {
                widgetModel.clearAllSelectLabelMarkedItem()
            }

            tvSelectAll.setOnClickListener {
                widgetModel.selectAllToMarked()
            }

            tvSubmittedAddPoint.setOnClickListener {
                widgetModel.playAddPointSoundEffect()
                showAddPointAnimation()
                widgetModel.submittedAddPoint(taskId = currentResultTaskId)
            }

            cslbLabelAsMarked.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    cslbLabelAsMarked.setState(LoadingButtonState.LOADING)
                    widgetModel.markSelectedResults()
                }
            })

            cslbEndTask.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    handleEndTaskClicked()
                }
            })

            cslbEndAll.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    handleEndAllTaskClicked()
                }
            })

            cslbToAll.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    handlePushRecordToAllClicked()
                }
            })

            cslbLoadRecordFailedReload.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    clLoadRecordFailed.isVisible = false
                    loadTask(currentResultTaskId)
                }
            })

            cslbReloadTask.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    clLoadTaskFailed.isVisible = false
                    getRecordList()
                }
            })
        }
    }

    private fun initRecyclerView() {
        //Task list recycler view layout init
        binding.rvRecordList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
            applyLayoutManagerRecordResultList(sortOption = currentSortOption)

            if (listAdapter == null) {
                listAdapter = RecordListAdapter().apply {
                    setTaskItemEventListener(this@RecordsTaskWidget)
                }
                adapter = listAdapter
            }
        }
    }

    private fun setGridStyleLayoutManager() {
        //Result list recycler view layout init
        val spaceHorizontal = context.resources.getDimensionPixelSize(
            R.dimen.push_respond_record_item_horizontal_space
        )

        val spaceVertical = context.resources.getDimensionPixelSize(
            R.dimen.push_respond_record_item_vertical_space
        )

        val itemDecoration = TaskItemDecoration(
            spanCount = GRID_SPAN_COUNT,
            spacingStart = spaceHorizontal,
            spacingEnd = spaceHorizontal,
            spacingTop = spaceVertical,
            spacingBottom = spaceVertical,
            includeEdge = false
        )

        binding.rvResultList.apply {
            layoutManager = GridLayoutManager(context, GRID_SPAN_COUNT)
            addItemDecoration(itemDecoration)
            itemAnimator = null
        }
    }

    private fun setListStyleLayoutManager() {

        val spaceVertical = context.resources.getDimensionPixelSize(
            R.dimen.push_respond_record_item_vertical_space
        )

        val itemDecoration = TaskItemDecoration(
            spanCount = 1,
            spacingStart = 0,
            spacingEnd = 0,
            spacingTop = spaceVertical,
            spacingBottom = spaceVertical,
            includeEdge = false
        )

        binding.rvResultList.apply {
            layoutManager = LinearLayoutManager(context)
            if (itemDecorationCount == 0) {
                addItemDecoration(itemDecoration)
            }
        }
    }

    private fun getRecordList() {
        showLoading(isFullScreen = true, isShow = true)
        showEmptyHint(isShow = false)
        widgetModel.getRecordList()
        seatAdapter?.clearAll()
        groupAdapter?.clearAll()
    }

    private fun observeUiState() {
        with(widgetModel) {
            coroutineScope.launch {
                uiStateFlow.collect { state ->
                    withContext(Dispatchers.Main) {
                        onUiStateUpdate(state)
                    }
                }
            }

            coroutineScope.launch {
                eventFlow.collect { event ->
                    withContext(Dispatchers.Main) {
                        onUiEventUpdate(event)
                    }
                }
            }

            coroutineScope.launch {
                timerFlow.collect { time ->
                    withContext(Dispatchers.Main) {
                        onTimerUpdate(time)
                    }
                }
            }
        }
    }

    private fun applyLayoutManagerRecordResultList(sortOption: String) {

        binding.rvResultList.apply {
            layoutManager = GridLayoutManager(context, GRID_SPAN_COUNT)
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
        }

        when (sortOption) {
            sortOptionNumber -> setGridStyleLayoutManager()
            sortOptionGroup -> setListStyleLayoutManager()
        }
    }

    private fun onUiStateUpdate(state: RecordsUiState) {
        Timber.d("[RecordsTaskWidget] on UI State Update : $state")
        resetAllHintView()

        when (state) {
            is RecordsUiState.RecordListUpdate -> {
                handleRecordListUpdate(info = state.data)
            }

            is RecordsUiState.RecordResultUpdate -> {
                handleRecordResultUpdate(
                    taskStatusInfo = state.taskStatusInfo,
                    data = state.data
                )
            }

            is RecordsUiState.SingleMarkUpdate -> {
                handleSingleMarkUpdate(state.success, state.failed)
            }

            is RecordsUiState.MultiMarkUpdate -> {
                handleMultiMarkUpdate(state.success, state.failed)
            }

            is RecordsUiState.PushRecordCompleted -> {
                handlePushRecordCompleted(state.data.size)
            }

            else -> Unit
        }
    }

    private fun onUiEventUpdate(event: RecordUiEvent) {
        Timber.d("on UI Event Update : $event")
        resetAllHintView()

        when (event) {
            is RecordUiEvent.RecordEventUpdate ->
                handleRecordEventUpdate(event = event.data)

            is RecordUiEvent.MarkUpdateFailed -> handleMarkFailedEvent(event = event)

            is RecordUiEvent.PushRecordFail -> handlePushRecordFailed(
                successfullyCount = event.successfullyCount,
                failedCount = event.failedCount,
                data = event.data // data will be used later, keep it for now.
            )
        }
    }

    private fun onTimerUpdate(data: Long) {
        val timerDisplayText = data.formatAsTime()
        binding.tvTimer.text = timerDisplayText
    }

    private fun handleRecordListUpdate(info: TaskListUpdateInfo) {
        Timber.d("handleRecordListUpdate : $info")

        if (info.data.isNotEmpty()) {
            with(binding) {
                if (!clLeftPanel.isVisible) {
                    clLeftPanel.isVisible
                }

                if (!clRightPanel.isVisible) {
                    clRightPanel.isVisible
                }
            }
        }

        if (info.data.isEmpty()) {
            showEmptyHint(isShow = true)
            return
        } else {
            showEmptyHint(isShow = false)
        }
        if (!info.isEndTask) {
            listAdapter?.clearFocusTask()
        }

        updateEndAllTaskButtonStatus(info = info)

        val isSelectFirstIfEmpty = !info.isEndTask
        listAdapter?.updateAll(data = info.data, selectFirstIfEmpty = isSelectFirstIfEmpty)
    }

    private fun handleRecordResultUpdate(
        taskStatusInfo: TaskStatusInfo,
        data: List<TaskResultInfo>
    ) {
        showLoading(isFullScreen = false, isShow = false)

        if (data.isEmpty()) return
        handleEndTaskButtonDisplayAndTimerBehavior(taskStatusInfo = taskStatusInfo)

        if (widgetModel.isLabelAsMarkMode()) {
            val selectCount = widgetModel.getSelectedCount()
            updateSelectedCountDisplayText(selectedCount = selectCount)
            updateLabelAsMarkedButtonEnableStatus(selectedCount = selectCount)
        }

        if (widgetModel.isPushRecordMode()) {
            val selectCount = widgetModel.getSelectedCount()
            updatePushRecordDisplayText(selectedCount = selectCount)
            updatePushRecordButtonEnableStatus(selectedCount = selectCount)
        }

        currentResultTaskId = taskStatusInfo.taskId
        // Ensure all adapters are initialized first.
        if (seatAdapter == null) {
            seatAdapter = RecordSeatListAdapter()
            seatAdapter?.setRecordItemEventListener(listener = this@RecordsTaskWidget)
        }

        if (groupAdapter == null) {
            groupAdapter = RecordGroupAdapter()
            groupAdapter?.setRecordGroupItemEventListener(listener = this@RecordsTaskWidget)
        }

        with(binding.rvResultList) {
            when (currentSortOption) {

                sortOptionNumber -> {
                    if (adapter !== seatAdapter) {
                        adapter = seatAdapter
                    }
                    seatAdapter?.updateAll(
                        taskId = taskStatusInfo.taskId,
                        data = data
                    )
                }

                sortOptionGroup -> {
                    if (adapter !== groupAdapter) {
                        adapter = groupAdapter
                    }
                    groupAdapter?.updateAll(
                        taskId = taskStatusInfo.taskId,
                        data = data
                    )
                }
            }
        }
    }

    private fun handleSingleMarkUpdate(
        successResults: UpdateTaskResult?,
        failedResults: UpdateTaskResult?
    ) {
        successResults?.let {
            widgetModel.updateTaskResults(data = listOf(it))
        }


        failedResults?.let {
            eventListener?.onRecordMaskUpdateFailed(failedData = listOf(it))
        }
    }

    private fun handleMultiMarkUpdate(
        success: List<UpdateTaskResult>,
        failed: List<UpdateTaskResult>
    ) {
        val successCount = success.size
        val failedCount = failed.size

        if (widgetModel.isLabelAsMarkMode() && failedCount == 0) {
            widgetModel.enableLabelAsMarkedMode(isEnable = false, isNeedUpdateData = true)
            binding.clLabelAsMarked.isVisible = false
            binding.cslbLabelAsMarked.setState(LoadingButtonState.DISABLE)
            showLabelAsMarkedButton(isShow = false)
        }
        widgetModel.updateTaskResults(data = success)

        if (failed.isNotEmpty()) {
            binding.cslbLabelAsMarked.setState(LoadingButtonState.ENABLE)
            widgetModel.updateTaskResults(data = success)
        }

        eventListener?.onLabelAsMarkResultUpdate(
            successCount = successCount,
            failedCount = failedCount
        )
    }

    private fun handleRecordEventUpdate(event: RecordEventInfo) {

        resetAllHintView()

        when (event) {
            is RecordEventInfo.GetTasksFailed -> {
                showLoadRecordListFailedView(isShow = true)
            }

            is RecordEventInfo.GetRecordByTaskIdFailed -> {
                showLoadRecordContentFailedView(isShow = true)
            }

            is RecordEventInfo.EndTaskSuccess -> {
                eventListener?.onEndAllTaskDialogClose()
                eventListener?.onEndTaskAllCompleted(successCount = event.successCount)
            }

            is RecordEventInfo.EndTaskFailed -> {
                eventListener?.onEndAllTaskDialogClose()
                eventListener?.onEndTaskFailed(
                    successCount = event.successCount,
                    failedCount = event.failedCount
                )
            }
        }
    }

    private fun handlePushRecordCompleted(successCount: Int) {
        binding.cslbToAll.setState(LoadingButtonState.ENABLE)
        binding.clPushRecord.isVisible = false
        showPushRecordButtonPanel(isShow = false)
        widgetModel.enablePushRecordMode(isEnable = false, isNeedUpdateData = true)
        getRecordList()
        eventListener?.onPushRecordsAllCompleted(successCount = successCount)
    }

    private fun scrollToCurrentTask() {
        listAdapter?.let { it ->
            val currentFocusId = it.getCurrentSelectedTaskId()
            val adapterIndex = listAdapter?.getAdapterIndexByTaskId(currentFocusId) ?: -1
            if (adapterIndex != -1) {
                binding.rvRecordList.post {
                    if (adapterIndex == 1) {
                        binding.rvRecordList.smoothScrollToPosition(0)
                    } else {
                        binding.rvRecordList.smoothScrollToPosition(adapterIndex)
                    }
                }
            }
        }
    }

    private fun handleMarkFailedEvent(event: RecordUiEvent.MarkUpdateFailed) {
        if(!event.isMultiMark) {
            eventListener?.onMarkUnknownError()
        } else {
            val selectedCount = widgetModel.getSelectedCount()
            updateLabelAsMarkedButtonEnableStatus(selectedCount = selectedCount)
            eventListener?.onLabelAsMarkUnknownError()
        }
    }

    private fun showEmptyHint(isShow: Boolean) {
        with(binding) {
            clLeftPanel.isVisible = !isShow
            clRightPanel.isVisible = !isShow
            viewDivider.isVisible = !isShow
            clNoRecordsHit.isVisible = isShow
        }
    }

    private fun showLoading(isFullScreen: Boolean, isShow: Boolean) {
        with(binding) {

            val params = clLoading.layoutParams as ConstraintLayout.LayoutParams

            if (isFullScreen) {
                params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
                params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            } else {
                params.width = resources.getDimensionPixelSize(
                    R.dimen.push_respond_record_non_full_screen_loading_width
                )

                params.height = resources.getDimensionPixelSize(
                    R.dimen.push_respond_record_non_full_screen_loading_height
                )

                val rightPanelId = binding.clRightPanel.id
                params.bottomToBottom = rightPanelId
                params.topToTop = rightPanelId
                params.startToStart = rightPanelId
                params.endToEnd = rightPanelId
            }

            clLoading.layoutParams = params
            clLoading.isVisible = isShow
            if (isShow) {
                lavLoadingAnimation.playAnimation()
            } else {
                lavLoadingAnimation.cancelAnimation()
            }
        }
    }

    private fun handleSortConditionChanged(option: PopupOptionInfo) {

        if (currentSortOption == option.title) return
        currentSortOption = option.title
        val data = widgetModel.getCurrentResultById(currentResultTaskId)

        Timber.d("Sort condition change to [${data.toString()}]")

        data?.let {
            applyLayoutManagerRecordResultList(sortOption = option.title)

            when (option.title) {
                sortOptionGroup -> {
                    Timber.d("Sort condition change to [${option.title}]")
                    binding.rvResultList.adapter = groupAdapter
                    groupAdapter?.updateAll(taskId = it.taskInfo.taskId, data = it.recordList)
                }

                sortOptionNumber -> {
                    Timber.d("Sort condition change to [${option.title}]")
                    binding.rvResultList.adapter = seatAdapter
                    seatAdapter?.updateAll(taskId = it.taskInfo.taskId, data = it.recordList)
                }

                else -> Unit
            }
        }
    }

    // RecordSeatListAdapter RecordItemEventListener
    override fun onThumbnailClicked(data: TaskResultInfo) {
        val itemIndex = data.serialNumber - 1
        val adapter = seatAdapter ?: return
        if (itemIndex !in 0 until adapter.itemCount) {
            Timber.d("onThumbnailClicked Index out of range: index=$itemIndex, itemCount=${adapter.itemCount}")
            return
        }
        handleItemThumbnailClicked(data = data, itemIndex)
    }

    // RecordSeatListAdapter RecordItemEventListener
    override fun onRecordDataSelectUpdate(data: TaskResultInfo) {
        widgetModel.updateItemSelectStatus(data = data)
    }

    // RecordSeatListAdapter RecordItemEventListener
    override fun onAddPointClicked(data: TaskResultInfo) {
        widgetModel.playAddPointSoundEffect()
        widgetModel.addPointByStudentId(studentId = data.studentId)
    }

    override fun onRefetchRecordData(data: TaskResultInfo.ApiFail) {
        widgetModel.getStudentTaskResult(data.studentId)
    }

    //RecordGroupAdapter RecordGroupItemEventListener
    override fun onGroupItemThumbnailClicked(data: TaskResultInfo) {
        val itemIndex = data.serialNumber - 1
        // check itemIndex is over item list count range
        val adapter = groupAdapter ?: return
        if (itemIndex !in 0 until adapter.getRecordResultItemCount()) {
            Timber.d("onGroupItemThumbnailClicked Index out of range: index=$itemIndex, itemCount=${adapter.itemCount}")
            return
        }
        handleItemThumbnailClicked(data = data, itemIndex)
    }

    //RecordGroupAdapter RecordGroupItemEventListener
    override fun onGroupItemSelectUpdate(data: TaskResultInfo) {
        widgetModel.updateItemSelectStatus(data = data)
    }

    //RecordGroupAdapter RecordGroupItemEventListener
    override fun onAddGroupMemberPointClicked(data: TaskResultInfo) {
        widgetModel.playAddPointSoundEffect()
        widgetModel.addPointByStudentId(studentId = data.studentId)
    }

    //RecordGroupAdapter RecordGroupItemEventListener
    override fun onAddGroupPointClicked(data: RecordGroupInfo) {
        val studentIds = data.data.map { it.studentId }
            .filter { it.isNotBlank() }
        widgetModel.playAddPointSoundEffect()
        widgetModel.addGroupPoint(groupStudentIds = studentIds)
    }

    //RecordGroupAdapter RecordGroupItemEventListener
    override fun onSubtractGroupPointClicked(data: RecordGroupInfo) {
        val studentCount = data.data.map { it.studentId }
            .filter { it.isNotBlank() }.size

        widgetModel.playSubtractPointSoundEffect()
        widgetModel.subtractGroupPoint(
            groupStudentCount = studentCount,
            groupId = data.groupId
        )
    }

    override fun onRefetchRecordData(data: TaskResultInfo) {
        widgetModel.getStudentTaskResult(data.studentId)
    }

    private fun updateSelectedCountDisplayText(selectedCount: Int) {
        val displayText = context.getString(
            R.string.push_and_respond_info_item_selected,
            selectedCount
        )
        binding.tvSelectedCount.text = displayText
    }

    private fun updatePushRecordDisplayText(selectedCount: Int) {
        val displayText = context.getString(
            R.string.push_and_respond_info_item_selected,
            selectedCount
        )
        binding.tvPushRecordsSelectedCount.text = displayText
    }

    private fun updateLabelAsMarkedButtonEnableStatus(selectedCount: Int) {
        val isActive = selectedCount > 0
        if (isActive) {
            binding.cslbLabelAsMarked.setState(LoadingButtonState.ENABLE)
        } else {
            binding.cslbLabelAsMarked.setState(LoadingButtonState.DISABLE)
        }
    }

    private fun updatePushRecordButtonEnableStatus(selectedCount: Int) {
        with(binding) {
            val isActive = selectedCount > 0
            if (isActive) {
                cslbToAll.setState(LoadingButtonState.ENABLE)
                cslbToAll.setEnable()
                //TODO custom push not in Q3 scope
                cslbCustomPush.setState(LoadingButtonState.DISABLE)
            } else {
                cslbToAll.setState(LoadingButtonState.DISABLE)
                cslbToAll.setDisable()
                //TODO custom push not in Q3 scope
                cslbCustomPush.setState(LoadingButtonState.DISABLE)
            }
        }
    }

    private fun handleItemThumbnailClicked(data: TaskResultInfo, index: Int) {
        if (data is TaskResultInfo.Content) {
            Timber.d("On Screen Record Click: $data")
            val type = data.triggerType

            when (type) {
                SubmitStatus.RESPONSE,
                SubmitStatus.GRADED -> {
                    eventListener?.onRecordMaskWidgetShow(index)
                }

                SubmitStatus.UNSUBMITTED -> {
                    eventListener?.onNotSubmittedRecordClick()
                }

                SubmitStatus.UNKNOWN -> Unit
            }
        }
    }

    private fun showLabelAsMarkedButton(isShow: Boolean) {
        val heightRes = if (isShow) {
            R.dimen.push_respond_record_list_label_as_marked_mode_height
        } else {
            R.dimen.push_respond_record_list_normal_mode_height
        }

        with(binding) {
            cslbLabelAsMarked.isVisible = isShow

            val recyclerViewHeight = resources.getDimensionPixelSize(heightRes)
            rvResultList.layoutParams = rvResultList.layoutParams.apply {
                height = recyclerViewHeight
            }
        }
    }

    private fun showPushRecordButtonPanel(isShow: Boolean) {
        val heightRes = if (isShow) {
            R.dimen.push_respond_record_list_push_record_mode_height
        } else {
            R.dimen.push_respond_record_list_normal_mode_height
        }

        with(binding) {
            clPushRecordButtonPanel.isVisible = isShow

            val recyclerViewHeight = resources.getDimensionPixelSize(heightRes)
            rvResultList.layoutParams = rvResultList.layoutParams.apply {
                height = recyclerViewHeight
            }
        }
    }

    private fun handleToolbarLabelAsMarkedButtonVisible(type: RecordType) {
        binding.tvLabelAsMarked.isVisible = type != RecordType.LINK
    }

    private fun handleToolbarPushRecordButtonVisible(
        type: RecordType,
        taskStatus: TaskStatus
    ) {
        binding.tvPushRecord.isVisible = when (type) {
            RecordType.LINK -> false
            else -> taskStatus == TaskStatus.CLOSED
        }
    }

    private fun handleEndTaskButtonDisplayAndTimerBehavior(taskStatusInfo: TaskStatusInfo) {
        if (taskStatusInfo.status == TaskStatus.IN_PROGRESS) {
            binding.cslbEndTask.isVisible = true
            widgetModel.startTimer(
                taskId = taskStatusInfo.taskId,
                taskCreateTime = taskStatusInfo.createAt
            )
        } else {
            widgetModel.stopTimer()
            binding.tvTimer.text = context.getString(R.string.push_and_respond_status_task_ended)
            binding.cslbEndTask.isVisible = false
        }
    }

    private fun handleEndTaskClicked() {
        val currentFocusTaskId = listAdapter?.getCurrentSelectedTaskId() ?: ""
        if (currentFocusTaskId.isEmpty()) {
            return
        }
        eventListener?.onEndTaskDialogShow(
            onConfirm = {
                widgetModel.endTask(listOf(currentFocusTaskId))
            },
            onCancel = {
                eventListener?.onEndTaskDialogClose()
            }
        )
    }

    private fun handleEndAllTaskClicked() {

        eventListener?.onEndAllTaskDialogShow(
            onConfirm = {
                widgetModel.endAllTask()
            },
            onCancel = {
                eventListener?.onEndAllTaskDialogClose()
            }
        )
    }

    private fun handlePushRecordToAllClicked() {
        if (!widgetModel.isPushRecordMode()) return
        if (widgetModel.isLabelAsMarkMode()) return
        binding.cslbToAll.setState(LoadingButtonState.LOADING)
        widgetModel.pushRecordToAll()
    }

    private fun handlePushRecordFailed(
        successfullyCount: Int,
        failedCount: Int,
        data: List<TaskApiResult<TaskResultInfo>> // data will be used later, keep it for now.
    ) {

        binding.cslbToAll.setState(LoadingButtonState.ENABLE)
        binding.clPushRecord.isVisible = false
        showPushRecordButtonPanel(isShow = false)
        widgetModel.enablePushRecordMode(isEnable = false, isNeedUpdateData = true)

        eventListener?.onPushRecordsFailed(
            successCount = successfullyCount,
            failedCount = failedCount
        )

        binding.cslbToAll.let {
            it.setState(LoadingButtonState.ENABLE)
            it.setEnable()
        }
    }


    private fun showAddPointAnimation() {
        with(binding.tvAddPointAnimator) {
            alpha = 1f
            translationY = 0f
            visibility = VISIBLE

            val floatDistance = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics
            )

            animate()
                .translationY(-floatDistance)
                .alpha(0f) // Fade out
                .setDuration(RecordsConstants.ANIMATION_DURATION)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    visibility = INVISIBLE
                }
                .start()
        }
    }

    private fun updateEndAllTaskButtonStatus(info: TaskListUpdateInfo) {
        with(binding) {
            val inProgressTaskCount = info.data.count {
                it is RecordListInfo.TaskItem && it.taskStatus == TaskStatus.IN_PROGRESS
            }

            if (inProgressTaskCount > 0) {
                cslbEndAll.setState(LoadingButtonState.ENABLE)
            } else {
                cslbEndAll.setState(LoadingButtonState.DISABLE)
            }
        }
    }

    private fun showLoadRecordListFailedView(isShow: Boolean) {
        binding.clLoadTaskFailed.isVisible = isShow
    }

    private fun showLoadRecordContentFailedView(isShow: Boolean) {
        binding.clLoadRecordFailed.isVisible = isShow
    }

    private fun resetAllHintView() {
        if (binding.clLoadTaskFailed.isVisible) {
            showLoadRecordListFailedView(isShow = false)
        }

        if (binding.clLoadRecordFailed.isVisible) {
            showLoadRecordContentFailedView(isShow = false)
        }

        if (binding.clLoading.isVisible) {
            showLoading(isFullScreen = false, isShow = false)
        }
    }

    companion object {
        private const val GRID_SPAN_COUNT = 5
    }
}