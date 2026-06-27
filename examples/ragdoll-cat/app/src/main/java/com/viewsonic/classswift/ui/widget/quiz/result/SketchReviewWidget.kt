package com.viewsonic.classswift.ui.widget.quiz.result

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.children
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.response.UpdateTaskResult
import com.viewsonic.classswift.data.quiz.SketchReviewUiEvent
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.databinding.WidgetSketchReviewBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.ImageDownloadView
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus
import com.viewsonic.classswift.ui.widget.task.paint.PaintOptionButton
import com.viewsonic.classswift.ui.widget.task.records.RecordStatus
import com.viewsonic.classswift.ui.widgetmodel.quiz.SketchReviewWidgetModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

/**
 * Sketch Response 結果頁批改 widget（紅筆 / 橡皮擦 / undo / Save and hand back）。
 *
 * 從 `ui/widget/task/records/RecordMarkWidget.kt` 拷貝後砍：
 * - 切換學生作品（`ivLeftArrow` / `ivRightArrow` click listener）
 * - 學生離座 auto-replace（`RecordMarkUiEvent.ReleaseSeat` 訂閱）
 * - 學生訂正後狀態跳回（`RecordMarkUiEvent.UpdateCurrentRecord` 訂閱 + `setNotMarkedUi(isUpdate=true)` toast）
 * - 多筆 refresh / index API（`refreshRecordData(List<>)` / `setSelectedIndex`）→ 改 [setRecord]
 *
 * 保留：paint UI / mergeBitmap pipeline / setData 狀態 UI（4 case）/ retry / send。
 *
 * Save and hand back 走外層 caller：widget 透過 [SketchReviewWidgetEventListener.onSendMarkedResult]
 * 把 [TaskResultInfo.Content] 拋給 caller，caller 負責呼叫 `SketchMarkHandler.updateContentTaskResult`。
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8454">VSFT-8454</a>
 */
class SketchReviewWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var imageUrl: String = ""
    private var eventListener: SketchReviewWidgetEventListener? = null

    private val widgetModel: SketchReviewWidgetModel by inject(SketchReviewWidgetModel::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val binding: WidgetSketchReviewBinding = WidgetSketchReviewBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    interface SketchReviewWidgetEventListener {
        fun onSendMarkedResult(data: TaskResultInfo.Content)
        fun onClose()
        fun onRefetchStudentRecord(data: TaskResultInfo)
    }

    init {
        // Consume clicks on the overlay background to prevent touch-through to underlying views.
        setOnClickListener { }
        observeUiState()
        initColorPalette()
        initClickAction()
        setOnDownloadListener()
        setSendButtonEnable(enable = false)

        context.withStyledAttributes(attrs, R.styleable.SketchReviewWidget) {
            val backgroundRes = getResourceId(
                R.styleable.SketchReviewWidget_srMaskBackground,
                R.drawable.bg_mask_radius800
            )
            setBackgroundResource(backgroundRes)
        }
    }

    /**
     * 設定當前要 review 的學生作品（取代原 `refreshRecordData(List<>)` + `setSelectedIndex`）。
     */
    fun setRecord(data: TaskResultInfo) {
        widgetModel.setRecord(data)
    }

    private fun setData(data: TaskResultInfo) {
        Timber.d("Sketch bind data = $data")
        when (data) {
            is TaskResultInfo.Content -> {
                when (data.triggerType) {
                    SubmitStatus.UNSUBMITTED -> setNotSubmittedUi(data)
                    SubmitStatus.RESPONSE -> setNotMarkedUi(data)
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

    private fun setNotMarkedUi(data: TaskResultInfo.Content) {
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
            val seatNumber = data.seatNumber.ifEmpty { "-" }
            tvSeatNumber.text = seatNumber
            tvName.text = data.displayName.ifEmpty { context.getString(R.string.common_guest) }
        }
    }

    fun onMarkUnknownError() {
        widgetModel.getCurrentRecord()?.let { info ->
            val updatedInfo = if (info is TaskResultInfo.Content) {
                info.copy(version = if (info.version > 0) info.version - 1 else 0)
            } else {
                info
            }
            widgetModel.updateCurrentRecord(updatedInfo)
        }

        val errorMessage = context.getString(R.string.mvb_sketch_response_error_save_and_hand_back_failed)
        showToast(message = errorMessage)
        blockAllTouchEvent(isBlock = false)
        setSendButtonEnable(true)
    }

    /**
     * Caller 收到 [SketchMarkHandler] 失敗 / 部分失敗時呼叫。
     *
     * 從 `RecordMarkWidget.onMarkUpdateResultError(List<UpdateTaskResult>)` 拷貝後簡化：
     * Sketch 是 single-mark，參數從 list 改為單一 [UpdateTaskResult]。
     *
     * Note: failed == null（成功 / 無錯誤）時仍要重置 button + touch block，
     * 否則 UI 會卡在 loading 狀態。
     */
    fun onMarkUpdateResultError(failed: UpdateTaskResult?) {
        Timber.d("Sketch on mark update result error: $failed")
        if (failed != null) {
            widgetModel.getCurrentRecord()?.let { record ->
                val isCurrentRecord = failed.taskId == record.taskId &&
                        failed.studentId == record.studentId
                if (isCurrentRecord) {
                    val errorMessage = context.getString(R.string.mvb_sketch_response_error_save_and_hand_back_failed)
                    showToast(message = errorMessage)
                    updateRecordInfo(failed)
                }
            }
        }

        blockAllTouchEvent(isBlock = false)
        setSendButtonEnable(true)
    }

    /**
     * Caller 收到 [SketchMarkHandler] 成功時呼叫（Sprint 20 行為）：
     *   - 顯示成功 toast「Saved and handed back」
     *   - 解除 touch block + 重置 send button
     *   - dismiss overlay 回到 cards 畫面
     *
     * Sprint 21+ 改成「自動跳下一個有 Submitted 的學生」(per Confluence US 3-2)。
     */
    fun onMarkUpdateSuccess(success: UpdateTaskResult?) {
        Timber.d("Sketch on mark update success: $success")
        val msg = context.getString(R.string.mvb_sketch_response_save_success)
        showToast(message = msg, isSuccess = true)
        blockAllTouchEvent(isBlock = false)
        setSendButtonEnable(true)
        // 觸發外層 listener 收回 overlay；行為等同使用者按 [X]，但靜默不彈確認 dialog。
        eventListener?.onClose()
    }

    fun setImageUrl(url: String) {
        Timber.d("Sketch start load image: $url")
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
        widgetModel.updateCurrentRecord(null)  // 清除 stale record，避免舊資料被殘存 scope 取到
        CoroutineManager.cancelScope(widgetModel) // 取消 widgetModel scope，停止 uploadImageSharedFlow collect
        CoroutineManager.cancelScope(this)        // 取消 widget 自身 scope
    }

    /**
     * Toast auto-dismiss runnable 是 view-level Handler post 出來的；
     * 如果 widget detach 前 runnable 還在 queue 裡，View 的 attachInfo 已 null 仍會嘗試 invalidate
     * （只是 no-op）但邏輯上會 hold 住 outer this reference。明確 remove 比較乾淨（M13）。
     */
    override fun onDetachedFromWindow() {
        binding.cstToast.removeCallbacks(hideRunnable)
        super.onDetachedFromWindow()
    }

    fun setEventListener(listener: SketchReviewWidgetEventListener) {
        eventListener = listener
    }

    fun show() {
        visibility = VISIBLE
    }

    fun isShownOnScreen(): Boolean = isVisible

    fun dismiss() {
        with(binding) {
            setSendButtonEnable(true)
            pvPaintView.clearCanvas()
        }
        visibility = GONE
    }

    private fun updateRecordInfo(failed: UpdateTaskResult) {
        widgetModel.getCurrentRecord()?.let { oldRecordInfo ->
            val isMatch = failed.taskId == oldRecordInfo.taskId &&
                    failed.studentId == oldRecordInfo.studentId
            if (isMatch) {
                val updatedRecordInfo = if (oldRecordInfo is TaskResultInfo.Content) {
                    oldRecordInfo.copy(version = failed.version)
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
            // Default using red color
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

            ivUndo.setOnClickListener { pvPaintView.undo() }

            ivRemoveAll.setOnClickListener { pvPaintView.undoAll() }

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
            // Sprint 21+：學生切換（左右箭頭）— 本 widget 不實作
        }
    }

    private fun blockAllTouchEvent(isBlock: Boolean) {
        binding.viewBlocker.visibility = if (isBlock) VISIBLE else GONE
    }

    private fun setPaintViewVisibility(isShow: Boolean) {
        binding.pvPaintView.visibility = if (isShow) VISIBLE else INVISIBLE
    }

    private fun mergeBitmap() {
        val baseColor = ContextCompat.getColor(context, R.color.paint_merge_bitmap_bg_color)
        with(binding) {
            // Use measured size (.width/.height) rather than layout params, which may be -1
            // (MATCH_PARENT) before the first layout pass and cause Bitmap.createBitmap to throw.
            val width = pvPaintView.width.takeIf { it > 0 } ?: pvPaintView.layoutParams.width
            val height = pvPaintView.height.takeIf { it > 0 } ?: pvPaintView.layoutParams.height
            if (width <= 0 || height <= 0) {
                Timber.w("Sketch mergeBitmap: PaintView size not ready (w=$width h=$height)")
                return
            }
            widgetModel.createMarkedBitmap(
                baseWidth = width,
                baseHeight = height,
                baseColor = baseColor,
                overlays = listOf(ivImage.exportToBitmap(), pvPaintView.exportToBitmap())
            )
        }
    }

    private fun onUiEventUpdate(event: SketchReviewUiEvent) {
        blockAllTouchEvent(isBlock = false)

        when (event) {
            is SketchReviewUiEvent.UploadImageSuccess -> {
                updateTaskResult(imageUri = event.imgUrl)
            }
            is SketchReviewUiEvent.UploadImageFailed -> {
                val errorMessage = context.getString(R.string.mvb_sketch_response_error_save_and_hand_back_failed)
                showToast(message = errorMessage)
                setSendButtonEnable(true)
            }
            is SketchReviewUiEvent.SetContent -> {
                setData(event.info)
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
                        Timber.d("[Sketch download] retry")
                        setSendButtonEnable(enable = false)
                        retryDownload()
                    }

                    override fun onStartDownload() {
                        Timber.d("[Sketch download] start")
                        setSendButtonEnable(enable = false)
                        setPaintViewVisibility(isShow = false)
                        setCircleProgressbarVisibility(isShown = true)
                        setFailedContainerVisibility(isShown = false)
                        setMaskVisibility(isShown = false)
                        startProgressAnimation(fromPercentage = 0, toPercentage = 90)
                    }

                    override fun onDownloadSuccess() {
                        Timber.d("[Sketch download] success")
                        startProgressAnimation(fromPercentage = 0, toPercentage = 100)
                        setSendButtonEnable(enable = true)
                        setPaintViewVisibility(isShow = true)
                        setCircleProgressbarVisibility(isShown = false)
                        setFailedContainerVisibility(isShown = false)
                        setMaskVisibility(isShown = false)
                    }

                    override fun onDownloadCancel() {
                        Timber.d("[Sketch download] cancel")
                        setSendButtonEnable(enable = false)
                        setPaintViewVisibility(isShow = false)
                        setCircleProgressbarVisibility(isShown = false)
                        setFailedContainerVisibility(isShown = true)
                        setMaskVisibility(isShown = false)
                    }

                    override fun onDownloadError() {
                        Timber.d("[Sketch download] error")
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

    private fun showToast(message: String, isSuccess: Boolean = false) {
        Timber.d("Sketch showToast (isSuccess=$isSuccess): $message")
        with(binding.cstToast) {
            setText(message)
            setIsSuccess(isSuccess = isSuccess)

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
                        if (info.triggerType.isSaveAndSendStatus()) {
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
