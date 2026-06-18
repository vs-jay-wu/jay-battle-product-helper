package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.children
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.response.UpdateTaskResult
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.databinding.WidgetRecordMarkBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.ImageDownloadView
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus
import com.viewsonic.classswift.ui.widget.task.paint.PaintOptionButton
import com.viewsonic.classswift.ui.widgetmodel.RecordMarkWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.records.state.RecordMarkUiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import kotlin.getValue
import androidx.core.view.isVisible
import kotlin.text.ifEmpty

class RecordMarkWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var imageUrl: String = ""
    private var outputBitmapUri: Uri? = null
    private var eventListener: RecordMarkWidgetEventListener? = null
    private var onMaskClick: (() -> Unit)? = null

    private val widgetModel: RecordMarkWidgetModel by inject(RecordMarkWidgetModel::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val binding: WidgetRecordMarkBinding = WidgetRecordMarkBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    interface RecordMarkWidgetEventListener {
        fun onSendMarkedResult(data: TaskResultInfo.Content)
        fun onClose()
        fun onToastShow(message: String)
        fun onRefetchStudentRecord(data: TaskResultInfo)
    }

    init {

        //Block clicks on the semi-transparent background
        setOnClickListener { onMaskClick?.invoke() }
        observeUiState()
        initColorPalette()
        initClickAction()
        setOnDownloadListener()
        setSendButtonEnable(enable = false)

        context.withStyledAttributes(attrs, R.styleable.RecordMarkWidget) {
            val backgroundRes = getResourceId(
                R.styleable.RecordMarkWidget_rmwMaskBackground,
                R.drawable.bg_mask_radius800
            )
            setBackgroundResource(backgroundRes)
        }
    }

    fun refreshRecordData(data: List<TaskResultInfo>) {
        widgetModel.updateStudentRecords(data)
    }

    fun setSelectedIndex(index: Int) {
        widgetModel.setSelectedIndex(index)
    }

    private fun setData(data: TaskResultInfo, isUpdate: Boolean = false) {
        Timber.d("bind data = $data")
        when (data) {
            is TaskResultInfo.Content -> {
                when (data.triggerType) {
                    SubmitStatus.UNSUBMITTED -> setNotSubmittedUi(data)
                    SubmitStatus.RESPONSE -> setNotMarkedUi(data, isUpdate)
                    SubmitStatus.GRADED -> setMarkedUi(data)
                    SubmitStatus.UNKNOWN -> Unit
                }
            }
            is TaskResultInfo.Guest -> setGuestUi(data)
            is TaskResultInfo.ApiFail -> setApiFailed(data)
            is TaskResultInfo.Link -> Unit
        }
        setSendButtonEnable(true)
    }

    private fun setNotSubmittedUi(data: TaskResultInfo.Content) {
        setSeatAndNameUi(data)
        with(binding) {
            pvPaintView.clearCanvas()
            flWithProblem.visibility = VISIBLE
            ivImage.visibility = VISIBLE
            clApiFailedArea.visibility = GONE
            clPaintOptions.visibility = GONE
            pvPaintView.visibility = INVISIBLE
            val notSubmittedMessage = context.getString(R.string.record_student_not_submitted_message)
            tvProblemMessage.text = notSubmittedMessage
            rsvStatus.setStatus(RecordStatus.NOT_SUBMITTED)
            setImageUrl(url = data.imgUrl)
        }
    }

    private fun setNotMarkedUi(data: TaskResultInfo.Content, isUpdate: Boolean) {
        setSeatAndNameUi(data)
        with(binding) {
            pvPaintView.clearCanvas()
            flWithProblem.visibility = GONE
            clApiFailedArea.visibility = GONE
            ivImage.visibility = VISIBLE
            clPaintOptions.visibility = VISIBLE
            pvPaintView.visibility = VISIBLE
            rsvStatus.setStatus(RecordStatus.NOT_MARKED)
            setImageUrl(url = data.imgUrl)
        }
        if (isUpdate) {
            val errorMessage = context.getString(R.string.push_and_respond_error_new_submission)
            showToast(message = errorMessage)
        }
    }

    private fun setMarkedUi(data: TaskResultInfo.Content) {
        setSeatAndNameUi(data)
        with(binding) {
            pvPaintView.clearCanvas()
            flWithProblem.visibility = GONE
            clApiFailedArea.visibility = GONE
            ivImage.visibility = VISIBLE
            clPaintOptions.visibility = VISIBLE
            pvPaintView.visibility = VISIBLE
            rsvStatus.setStatus(RecordStatus.MARKED)
            setImageUrl(url = data.imgUrl)
        }
    }

    private fun setGuestUi(data: TaskResultInfo.Guest) {
        setSeatAndNameUi(data)
        with(binding) {
            pvPaintView.clearCanvas()
            flWithProblem.visibility = VISIBLE
            clApiFailedArea.visibility = GONE
            ivImage.visibility = GONE
            clPaintOptions.visibility = GONE
            pvPaintView.visibility = INVISIBLE
            val recordStudentMessage = context.getString(R.string.record_student_absent_message)
            tvProblemMessage.text = recordStudentMessage
            rsvStatus.setStatus(RecordStatus.ABSENT)
        }
    }

    private fun setApiFailed(data: TaskResultInfo.ApiFail) {
        setSeatAndNameUi(data)
        with(binding) {
            pvPaintView.clearCanvas()
            flWithProblem.visibility = GONE
            clApiFailedArea.visibility = VISIBLE
            cvRetryArea.setOnClickListener {
                val currentRecord = widgetModel.getCurrentRecord()
                if (currentRecord is TaskResultInfo.ApiFail) {
                    eventListener?.onRefetchStudentRecord(data = currentRecord)
                }
            }
            ivImage.visibility = GONE
            clPaintOptions.visibility = GONE
            pvPaintView.visibility = INVISIBLE
            rsvStatus.setStatus(RecordStatus.INIT)
        }
    }

    private fun setSeatAndNameUi(data: TaskResultInfo) {
        with(binding) {
            val seatNumber = data.seatNumber.ifEmpty {
                "-"
            }
            tvSeatNumber.text = seatNumber
            tvName.text = data.displayName.ifEmpty { context.getString(R.string.common_guest) }
        }
    }

    fun onMarkUnknownError() {
        widgetModel.getCurrentRecord()?.let { info ->
            val updatedInfo = if (info is TaskResultInfo.Content) {
                info.copy(
                    version = if (info.version > 0) info.version - 1 else 0
                )
            } else {
                info
            }
            widgetModel.updateCurrentRecord(updatedInfo)
        }

        val errorMessage = context.getString(R.string.push_and_respond_error_save_and_send_failed)
        showToast(message = errorMessage)
        blockAllTouchEvent(isBlock = false)
        setSendButtonEnable(true)
    }

    fun onMarkUpdateResultError(failedData: List<UpdateTaskResult>) {
        Timber.d("on mark update result error")

        widgetModel.getCurrentRecord()?.let { record ->
            val hasError = failedData.any {
                it.taskId == record.taskId &&
                        it.studentId == record.studentId
            }

            if (hasError) {
                val errorMessage = context.getString(R.string.push_and_respond_error_save_and_send_failed)
                showToast(message = errorMessage)
                updateRecordInfo(failedData = failedData)
            }
        }

        blockAllTouchEvent(isBlock = false)
        setSendButtonEnable(true)
    }

    fun setImageUrl(url: String) {
        Timber.d("start load image: $url")
        setPaintViewVisibility(isShow = false)
        imageUrl = url
        binding.ivImage.let {
            it.setCircleProgressbarVisibility(isShown = false)
            it.setFailedContainerVisibility(isShown = false)
            it.setMaskVisibility(isShown = false)
            it.setImage(uri = url)
        }
    }

    fun release() {
        imageUrl = ""
        eventListener = null
        outputBitmapUri = null
    }

    fun setRecordMarkWidgetEventListener(listener: RecordMarkWidgetEventListener) {
        eventListener = listener
    }

    fun show() {
        visibility = VISIBLE
    }

    fun isShownOnScreen(): Boolean {
        return isVisible
    }

    fun dismiss() {
        with(binding) {
            setSendButtonEnable(true)
            pvPaintView.clearCanvas()
        }
        visibility = GONE
    }

    private fun updateRecordInfo(failedData: List<UpdateTaskResult>) {
        widgetModel.getCurrentRecord()?.let { oldRecordInfo ->
            val failedItem = failedData.find {
                it.taskId == oldRecordInfo.taskId &&
                        it.studentId == oldRecordInfo.studentId
            }
            failedItem?.let { item ->
                val updatedRecordInfo = if (oldRecordInfo is TaskResultInfo.Content) {
                    oldRecordInfo.copy(
                        version = item.version
                    )
                } else {
                    oldRecordInfo
                }
                widgetModel.updateCurrentRecord(updatedRecordInfo)
            }
        }
    }

    private fun observeUiState() {
        with(widgetModel) {
            coroutineScope.launch {
                uiEventFlow.collect { event ->
                    withContext(Dispatchers.Main) {
                        onUiEventUpdate(event)
                    }
                }
            }
        }
    }

    private fun initColorPalette() {

        with(binding) {

            //Default using red color
            handlePaintOptionSelected(selectedView = pobRed)

            pobRed.setOnClickListener {
                pvPaintView.setBrushColor(R.color.paint_color_red)
                handlePaintOptionSelected(selectedView = pobRed)
            }

            pobYellow.setOnClickListener {
                pvPaintView.setBrushColor(R.color.paint_color_yellow)
                handlePaintOptionSelected(selectedView = pobYellow)
            }

            pobGreen.setOnClickListener {
                pvPaintView.setBrushColor(R.color.paint_color_green)
                handlePaintOptionSelected(selectedView = pobGreen)
            }

            pobBlue.setOnClickListener {
                pvPaintView.setBrushColor(R.color.paint_color_blue)
                handlePaintOptionSelected(selectedView = pobBlue)
            }

            pobWhite.setOnClickListener {
                pvPaintView.setBrushColor(R.color.paint_color_white)
                handlePaintOptionSelected(selectedView = pobWhite)
            }

            pobBlack.setOnClickListener {
                pvPaintView.setBrushColor(R.color.paint_color_black)
                handlePaintOptionSelected(selectedView = pobBlack)
            }

            pobEraser.setOnClickListener {
                pvPaintView.setEraserMode()
                handlePaintOptionSelected(selectedView = pobEraser)
            }

            ivUndo.setOnClickListener {
                pvPaintView.undo()
            }

            ivRemoveAll.setOnClickListener {
                pvPaintView.undoAll()
            }

            clbSendAndSave.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    blockAllTouchEvent(isBlock = true)
                    clbSendAndSave.setLoading()
                    mergeBitmap()
                }
            })
        }
    }

    private fun handlePaintOptionSelected(selectedView: View) {
        binding.clPaintOptions.children
            .filterIsInstance<PaintOptionButton>()
            .forEach { it.setOptionSelected(it.id == selectedView.id) }
    }

    private fun initClickAction() {
        with(binding) {
            ivClose.setOnClickListener {
                eventListener?.onClose()
                pvPaintView.clearCanvas()
            }
            ivRightArrow.setOnClickListener { widgetModel.switchRecord(true) }
            ivLeftArrow.setOnClickListener { widgetModel.switchRecord(false) }
        }
    }

    private fun blockAllTouchEvent(isBlock: Boolean) {
        binding.viewBlocker.visibility = if (isBlock) VISIBLE else GONE
    }

    private fun setPaintViewVisibility(isShow: Boolean) {
        binding.pvPaintView.visibility = if (isShow) VISIBLE else INVISIBLE
    }

    private fun mergeBitmap() {
        val baseColor = ContextCompat.getColor(
            context,
            R.color.paint_merge_bitmap_bg_color
        )

        with(binding) {
            val width = pvPaintView.layoutParams.width
            val height = pvPaintView.layoutParams.height
            val sourceBitmap = ivImage.exportToBitmap()
            val drawingBitmap = pvPaintView.exportToBitmap()
            val bitmapList = mutableListOf(sourceBitmap, drawingBitmap).toList()

            widgetModel.createMarkedBitmap(
                baseWidth = width,
                baseHeight = height,
                baseColor = baseColor,
                overlays = bitmapList
            )
        }
    }

    private fun onUiEventUpdate(event: RecordMarkUiEvent) {
        blockAllTouchEvent(isBlock = false)

        when (event) {
            is RecordMarkUiEvent.UploadImageSuccess -> {
                updateTaskResult(imageUri = event.imgUrl)
            }

            is RecordMarkUiEvent.UploadImageFailed -> {
                val errorMessage = context.getString(R.string.push_and_respond_error_save_and_send_failed)
                showToast(message = errorMessage)
                setSendButtonEnable(true)
            }
            is RecordMarkUiEvent.SwitchContent -> {
                setData(event.info)
            }
            is RecordMarkUiEvent.ReleaseSeat -> {
                setData(event.newinfo)
                showToast(context.getString(
                    R.string.push_and_respond_student_left,
                    event.oldInfo.seatNumber,
                    event.oldInfo.displayName
                ))
            }
            is RecordMarkUiEvent.UpdateCurrentRecord -> {
                setData(event.info, true)
            }

        }
    }

    private fun updateTaskResult(imageUri: String) {
        if (imageUri.isEmpty()) return
        widgetModel.getCurrentRecord()?.let { info ->
            if (info is TaskResultInfo.Content) {
                val newData = info.copy(
                    imgUrl = imageUri,
                    version = info.version + 1,
                    triggerType = SubmitStatus.GRADED
                )
                eventListener?.onSendMarkedResult(data = newData)
            } else {
                info
            }
        }
    }

    private fun retryDownload() {
        binding.ivImage.let {
            it.setImage(uri = imageUrl)
            it.setCircleProgressbarVisibility(isShown = true)
            it.setFailedContainerVisibility(isShown = false)
            it.setMaskVisibility(isShown = false)
        }
    }

    private fun setSendButtonEnable(enable: Boolean) {
        if (enable) {
            setSendAndSaveBtnStatus()
        } else {
            binding.clbSendAndSave.setDisable()
        }
    }

    private fun setOnDownloadListener() {

        with(binding.ivImage) {
            setOnDownloadImageListener(
                object : ImageDownloadView.DownloadImageListener {
                    override fun onReDownload() {
                        Timber.d("[Download Image] - retry download")
                        setSendButtonEnable(enable = false)
                        retryDownload()
                    }

                    override fun onStartDownload() {
                        Timber.d("[Download Image] - start download")
                        setSendButtonEnable(enable = false)
                        setPaintViewVisibility(isShow = false)
                        setCircleProgressbarVisibility(isShown = true)
                        setFailedContainerVisibility(isShown = false)
                        setMaskVisibility(isShown = false)
                        startProgressAnimation(
                            fromPercentage = 0,
                            toPercentage = 90
                        )
                    }

                    override fun onDownloadSuccess() {
                        Timber.d("[Download Image] - download success")

                        startProgressAnimation(
                            fromPercentage = 0,
                            toPercentage = 100
                        )
                        setSendButtonEnable(enable = true)
                        setPaintViewVisibility(isShow = true)
                        setCircleProgressbarVisibility(isShown = false)
                        setFailedContainerVisibility(isShown = false)
                        setMaskVisibility(isShown = false)
                    }

                    override fun onDownloadCancel() {
                        Timber.d("[Download Image] - cancel")
                        setSendButtonEnable(enable = false)
                        setPaintViewVisibility(isShow = false)
                        setCircleProgressbarVisibility(isShown = false)
                        setFailedContainerVisibility(isShown = true)
                        setMaskVisibility(isShown = false)
                    }

                    override fun onDownloadError() {
                        Timber.d("[Download Image] - download error")
                        setSendButtonEnable(enable = false)
                        setPaintViewVisibility(isShow = false)
                        setCircleProgressbarVisibility(isShown = false)
                        setFailedContainerVisibility(isShown = true)
                        setMaskVisibility(isShown = false)
                    }
                }
            )
        }
    }

    private fun showToast(message: String) {
        Timber.d("ShowToast[Message: $message]")
        with(binding.cstToast) {
            setText(message)
            setIsSuccess(isSuccess = false)

            visibility = VISIBLE
            removeCallbacks(hideRunnable)
            postDelayed(hideRunnable, 3000L)
        }
    }

    private val hideRunnable = Runnable {
        binding.cstToast.visibility = GONE
    }

    private fun setSendAndSaveBtnStatus() {
        binding.clbSendAndSave.apply {
            widgetModel.getCurrentRecord()?.let { info ->
                when (info) {
                    is TaskResultInfo.Content -> {
                        if(info.triggerType.isSaveAndSendStatus()) {
                            setState(LoadingButtonState.ENABLE)
                        } else {
                            setState(LoadingButtonState.DISABLE)
                        }
                    }
                    is TaskResultInfo.Guest -> setState(LoadingButtonState.DISABLE)
                    is TaskResultInfo.ApiFail -> setState(LoadingButtonState.DISABLE)
                    is TaskResultInfo.Link -> Unit
                }
            }
        }
    }

}