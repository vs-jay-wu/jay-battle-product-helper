package com.viewsonic.classswift.ui.widget.task.content

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.task.TaskApiResult
import com.viewsonic.classswift.data.task.TaskInfo
import com.viewsonic.classswift.data.task.UrlPreviewInfo
import com.viewsonic.classswift.databinding.WidgetContentTaskBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.task.enums.CreateTaskOption
import com.viewsonic.classswift.ui.widget.task.link.UrlMetaPreviewDialog
import com.viewsonic.classswift.ui.widgetmodel.task.ContentTaskWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.task.state.PushRespondUiEvent
import com.viewsonic.classswift.ui.widgetmodel.task.state.PushRespondUiState
import com.viewsonic.classswift.ui.window.CSSystemDialogWindow
import com.viewsonic.classswift.ui.window.ToastWindow
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class ContentTaskWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), TaskGridAdapter.TaskItemEventListener {

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val widgetModel: ContentTaskWidgetModel by inject(ContentTaskWidgetModel::class.java)
    private var containerWindowTag: WindowTag = WindowTag.NONE
    private var adapter: TaskGridAdapter? = null
    private var blockAllCallback: Boolean = false
    private var dialogWindow: CSSystemDialogWindow? = null
    private var urlMetaPreviewDialog: UrlMetaPreviewDialog? = null
    private var blockPushClick: Boolean = false
    private var eventListener: ContentTaskWidgetEventListener? = null
    private var autoTabJob: Job? = null

    private val binding: WidgetContentTaskBinding = WidgetContentTaskBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    interface ContentTaskWidgetEventListener {
        fun onEndQuizAndBatchQuizzesTipDialogShow(
            onConfirm: () -> Unit = {},
            onCancel: () -> Unit = {}
        )

        fun onEndQuizAndBatchQuizzesTipDialogDismiss()
        fun onToastShow(isError: Boolean, message: String)
        fun onStartPushTask()
        fun onPushTaskCompleted()
        fun onPushTaskPartialCompleted()
    }

    init {
        observeUiState()
        initClickAction()
        initRecyclerView()
    }

    fun init() {
        updateSelectedCountDisplayText(0)
    }

    fun release() {
        urlMetaPreviewDialog = null
        coroutineScope.cancel()
    }

    fun setContentTaskWidgetEventListener(listener: ContentTaskWidgetEventListener) {
        eventListener = listener
    }

    /**
     * Setting the window tag is required."
     */
    fun setContainerWindowTag(tag: WindowTag) {
        containerWindowTag = tag
    }

    fun setUrlMetaPreviewDialogWidget(dialog: UrlMetaPreviewDialog) {
        urlMetaPreviewDialog = dialog
        initUrlMetaPreviewDialog()
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
                errorFlow.collect { event ->
                    withContext(Dispatchers.Main) {
                        onUiEventUpdate(event)
                    }
                }
            }
        }
    }

    private fun onUiStateUpdate(state: PushRespondUiState) {
        Timber.d("[ContentTaskWidget] on UI State Update : $state")
        when (state) {
            is PushRespondUiState.TaskListUpdate -> handleTaskListUpdate(data = state.data)
            is PushRespondUiState.PushTaskCompleted -> handlePushTaskCompleted(data = state.data)
            else -> Unit
        }
    }

    private fun onUiEventUpdate(event: PushRespondUiEvent) {
        Timber.d("[ContentTaskWidget] on UI Event Update : $event")

        when (event) {
            is PushRespondUiEvent.ScreenshotFail -> {
                //Screenshot error need show error msg using ToastWindow
                ToastWindow.MakeText(
                    context = context,
                    context.getString(R.string.quiz_error_msg_screenshot),
                    3000
                ).build().show()
            }

            is PushRespondUiEvent.EndQuizFail -> handleEndQuizFail()

            is PushRespondUiEvent.PushTaskFail -> handlePushTaskFailed(
                successfullyCount = event.successfullyCount,
                failedCount = event.failedCount,
                successTasks = event.successTasks,
                data = event.data // data will be used later, keep it for now.
            )
        }
    }

    private fun initClickAction() {
        with(binding) {
            btnCancelEdit.setOnClickListener {
                updatePushButtonStatus(0)
                widgetModel.unselectAllItemSelect()
                blockAllCallback = true
            }

            ivDelete.setOnClickListener {
                val deleteCount = widgetModel.getSelectedCount()
                showDeleteConfirmDialog(
                    deleteCount = deleteCount,
                    onConfirm = {
                        widgetModel.deletedSelectedItem()
                        dialogWindow?.dismiss()
                    },
                    onCancel = {
                        dialogWindow?.dismiss()
                    }
                )
            }

            cslbCustomPush.setState(LoadingButtonState.DISABLE)

            cslbToAll.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    handlePushToAllButtonClicked()
                }
            })
        }
    }

    private fun initRecyclerView() {
        val spaceHorizontal = context.resources.getDimensionPixelSize(
            R.dimen.push_respond_task_grid_item_horizontal_space
        )

        val spaceVertical = context.resources.getDimensionPixelSize(
            R.dimen.push_respond_task_grid_item_vertical_space
        )

        val itemDecoration = TaskItemDecoration(
            spanCount = GRID_SPAN_COUNT,
            spacingStart = spaceHorizontal,
            spacingEnd = spaceHorizontal,
            spacingTop = spaceVertical,
            spacingBottom = spaceVertical,
            includeEdge = false
        )

        binding.recyclerview.apply {
            layoutManager = GridLayoutManager(context, GRID_SPAN_COUNT)
            addItemDecoration(itemDecoration)
            itemAnimator = null
        }
    }

    private fun handleTaskListUpdate(data: List<TaskInfo>) {
        if (data.isEmpty()) return
        Timber.d("[ContentTaskWidget] handleTaskListUpdate : ${data.toString()}")

        if (adapter == null) {
            adapter = TaskGridAdapter()
            adapter?.setTaskItemEventListener(listener = this@ContentTaskWidget)
            binding.recyclerview.adapter = adapter
        }

        val isAllDataUnSelected = isAllUnselectedAndUneditable(data)
        if (isAllDataUnSelected) {
            blockAllCallback = false
        }

        adapter?.updateAll(data = data)
        updateEditBarVisibility(data = data)

        val selectCount = widgetModel.getSelectedCount()
        updateSelectedCountDisplayText(selectedTaskCount = selectCount)
        updatePushButtonStatus(selectedTaskCount = selectCount)
    }

    private fun handlePushTaskCompleted(data: List<TaskApiResult<TaskInfo>>) {
        Timber.d("Push Task Completed: ${data.toString()}")
        eventListener?.onPushTaskCompleted()

        coroutineScope.launch(Dispatchers.Main) {
            val unclosedMissions = unclosedMissionUiManager.getUnclosedMissions()
                .filter {
                    it == MissionType.QUIZ || it == MissionType.BATCH_QUIZZES
                }
            val isUnclosedMissionExisted = unclosedMissions.isNotEmpty()

            if (isUnclosedMissionExisted) {
                val isAllUnclosedMissionClosed = unclosedMissions
                    .map { unclosedMissionUiManager.closeMission(it) }
                    .all { it }
                if (isAllUnclosedMissionClosed) {
                    handleEndQuizSuccess()
                } else {
                    handleEndQuizFail()
                }
            } else {
                binding.cslbToAll.setState(LoadingButtonState.ENABLE)
                blockPushClick = false
            }

            eventListener?.onToastShow(
                isError = false,
                message = context.getString(
                    R.string.push_and_respond_success_msg_push_all_successful,
                    data.size
                )
            )

            updatePushButtonStatus(0)
            widgetModel.unselectAllItemSelect()
            blockAllCallback = true
        }
    }

    private fun handlePushTaskFailed(
        successfullyCount: Int,
        failedCount: Int,
        successTasks: List<TaskInfo>,
        data: List<TaskApiResult<TaskInfo>> // data will be used later, keep it for now.
    ) {
        val message = if (successfullyCount == 0) {
            context.getString(
                R.string.push_and_respond_error_msg_push_all_failed,
                failedCount
            )
        } else {
            eventListener?.onPushTaskPartialCompleted()
            context.getString(
                R.string.push_and_respond_error_msg_push_partial_failed,
                successfullyCount,
                failedCount
            )
        }

        binding.cslbToAll.let {
            it.setState(LoadingButtonState.ENABLE)
            it.setEnable()
            blockPushClick = false
        }
        eventListener?.let {
            it.onEndQuizAndBatchQuizzesTipDialogDismiss()
            it.onToastShow(isError = true, message = message)
        }

        widgetModel.unselectTasks(data = successTasks)
    }

    private fun handleEndQuizSuccess() {
        eventListener?.onEndQuizAndBatchQuizzesTipDialogDismiss()
        binding.cslbToAll.let {
            it.setState(LoadingButtonState.ENABLE)
            it.setEnable()
            blockPushClick = false
        }
    }

    private fun handleEndQuizFail() {

        binding.cslbToAll.let {
            it.setState(LoadingButtonState.ENABLE)
            it.setEnable()
            blockPushClick = false
        }

        eventListener?.onEndQuizAndBatchQuizzesTipDialogDismiss()

        //End quiz error need show error msg using CSToast
        eventListener?.onToastShow(
            isError = true,
            message = context.getString(R.string.dialog_end_quiz_end_and_push_task_failed)
        )
    }

    private fun isAllUnselectedAndUneditable(data: List<TaskInfo>): Boolean {
        val selectableTasks = data.filterIsInstance<TaskInfo.EditableTask>()
        return selectableTasks.isNotEmpty() &&
                selectableTasks.all { !it.isSelected && !it.isEditable }
    }

    private fun updateEditBarVisibility(data: List<TaskInfo>) {
        val hasSelectedTask = data.any { it is TaskInfo.EditableTask && it.isSelected }
        val visibility = if (hasSelectedTask) VISIBLE else INVISIBLE
        with(binding) {
            clEditToolbar.visibility = visibility
            tvSelectedCount.visibility = visibility
        }
    }

    private fun updatePushButtonStatus(selectedTaskCount: Int) {
        with(binding) {
            if (selectedTaskCount > 0) {
                //TODO custom push not in Q3 scope
                cslbCustomPush.setState(LoadingButtonState.DISABLE)
                cslbToAll.setState(LoadingButtonState.ENABLE)
            } else {
                //TODO custom push not in Q3 scope
                cslbCustomPush.setState(LoadingButtonState.DISABLE)
                cslbToAll.setState(LoadingButtonState.DISABLE)
            }
        }
    }

    private fun updateSelectedCountDisplayText(selectedTaskCount: Int) {
        val displayText = context.getString(R.string.push_and_respond_info_item_selected, selectedTaskCount)
        binding.tvSelectedCount.text = displayText
    }

    override fun onCreateContentSelected(option: CreateTaskOption) {
        val currentCount = widgetModel.getCurrentItemCount()
        if (currentCount >= MAX_ITEM_COUNT) return

        when (option) {
            CreateTaskOption.SELECT_CONTENT -> {
                widgetModel.createScreenShotContent()
            }

            CreateTaskOption.LINK -> {
                urlMetaPreviewDialog?.show()
            }
        }
    }

    private fun showDeleteConfirmDialog(
        deleteCount: Int,
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {

        with(context) {

            val message = getString(
                R.string.dialog_push_and_respond_delete_content_message,
                deleteCount
            )

            dialogWindow = CSSystemDialogWindow.Builder(this)
                .setTitle(getString(R.string.dialog_push_and_respond_delete_content_title))
                .setMessage(message)
                .setNegativeButton(
                    text = getString(R.string.common_cancel),
                    color = ContextCompat.getColor(this, R.color.color_2E3133),
                    listener = onCancel
                )
                .setPositiveButton(
                    text = getString(R.string.common_confirm),
                    color = ContextCompat.getColor(this, R.color.color_F02B2B),
                    listener = onConfirm
                )
                .build()

            dialogWindow?.show()
        }
    }

    private fun initUrlMetaPreviewDialog() {
        urlMetaPreviewDialog?.apply {
            setUrlMetaPreviewDialogEventListener(
                object : UrlMetaPreviewDialog.UrlMetaPreviewDialogEventListener {

                    override fun onConfirmClick(data: UrlPreviewInfo) {
                        Timber.d("Url preview dialog onConfirmClick : $data")
                        dismiss()
                        widgetModel.addLinkTask(data = data)
                    }

                    override fun onCancelClick() {
                        dismiss()
                    }
                })
        }
    }

    private fun handlePushToAllButtonClicked() {
        if (!blockPushClick) {
            blockPushClick = true
            binding.cslbToAll.setState(LoadingButtonState.LOADING)
            coroutineScope.launch(Dispatchers.Main) {
                when (unclosedMissionUiManager.getLastUnclosedMission()) {
                    MissionType.QUIZ,
                    MissionType.BATCH_QUIZZES -> {
                        eventListener?.onEndQuizAndBatchQuizzesTipDialogShow(
                            onConfirm = {
                                eventListener?.onStartPushTask()
                                widgetModel.pushTasksToStudent()
                            },

                            onCancel = {
                                blockPushClick = false
                                binding.cslbToAll.setEnable()
                                eventListener?.onEndQuizAndBatchQuizzesTipDialogDismiss()
                            })
                    }
                    else -> {
                        eventListener?.onStartPushTask()
                        widgetModel.pushTasksToStudent()
                    }
                }
            }
        }
    }

    override fun onTaskItemDelete(data: TaskInfo) {
        if (blockAllCallback) return
        showDeleteConfirmDialog(
            deleteCount = 1,
            onConfirm = {
                widgetModel.deleteItem(data = data)
                val selectedCount = widgetModel.getSelectedCount()
                updatePushButtonStatus(selectedTaskCount = selectedCount)
                dialogWindow?.dismiss()
            },
            onCancel = {
                dialogWindow?.dismiss()
            }
        )
    }

    override fun onImageUploadSuccess(data: TaskInfo) {
        if (blockAllCallback) return
        Timber.d("[ContentTaskWidget] onTaskDataPropertyUpdate : ${data.toString()}")
        widgetModel.updateItemImageUploadSuccess(data = data)
        val selectedCount = widgetModel.getSelectedCount()
        updatePushButtonStatus(selectedTaskCount = selectedCount)
    }

    override fun onTaskDataSelectUpdate(data: TaskInfo) {
        if (blockAllCallback) return
        Timber.d("[ContentTaskWidget] onTaskDataSelectUpdate : ${data.toString()}")
        widgetModel.updateItemSelectStatus(data = data)
        val selectedCount = widgetModel.getSelectedCount()
        updatePushButtonStatus(selectedTaskCount = selectedCount)
    }

    companion object {
        private const val GRID_SPAN_COUNT = 5
        private const val MAX_ITEM_COUNT = 100
    }
}