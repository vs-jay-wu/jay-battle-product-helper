package com.viewsonic.classswift.ui.widgetmodel.quiz

import android.content.Context
import android.graphics.Bitmap
import com.viewsonic.classswift.coordinator.UploadFileHandler
import com.viewsonic.classswift.data.quiz.SketchReviewUiEvent
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.utils.PaintUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

/**
 * `SketchReviewWidget` 的 widget model。
 *
 * 從 `ui/widgetmodel/RecordMarkWidgetModel.kt` 拷貝後砍：
 * - `recordInfoList` / `currentRecordIndex`（多筆切換 — Sprint 21+ 行為）
 * - `updateStudentRecords(List<>)` → 改為 [setRecord]（單筆）
 * - `setSelectedIndex(index)` / `switchRecord(isNext)` / `switchCurrentRecord()`
 * - private `updateCurrentRecord()` 自動偵測 emit（含 `ReleaseSeat` / `UpdateCurrentRecord`）
 *
 * Sprint 20 bypass `MarkToolHandler`：直接呼叫 [PaintUtils] 完成 bitmap → URI → upload pipeline。
 * 原因：`MarkToolHandler` 在 `saveBitmapAndGetUri` 回傳 null 時會 emit 字串 `"null"`，
 * 加上 `MutableStateFlow` 會 dedup，造成第二次 retry 時 collect 不 fire → 按鈕無限 loading。
 * 直接 inline call 可以掌握 null URI 並 emit [SketchReviewUiEvent.UploadImageFailed]。
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8454">VSFT-8454</a>
 */
class SketchReviewWidgetModel(
    private val applicationContext: Context,
    private val uploadFileHandler: UploadFileHandler,
    private val classroomManager: ClassroomManager,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val lessonId = classroomManager.classroomDataStateFlow.value
        .selectedClassroomInfo.lessonId

    @Volatile
    private var recordInfo: TaskResultInfo? = null

    private val _uiEventFlow = MutableSharedFlow<SketchReviewUiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val uiEventFlow: SharedFlow<SketchReviewUiEvent> = _uiEventFlow.asSharedFlow()

    init {
        observeData()
    }

    private fun observeData() {
        coroutineScope.launch(mainDispatcher) {
            uploadFileHandler.uploadImageSharedFlow.collect { isSuccess ->
                Timber.d("Sketch upload image status: $isSuccess")
                when (isSuccess) {
                    true -> _uiEventFlow.emit(
                        SketchReviewUiEvent.UploadImageSuccess(
                            imgUrl = uploadFileHandler.awsPreSignedUrl.s3GetUrl
                        )
                    )
                    false -> _uiEventFlow.emit(SketchReviewUiEvent.UploadImageFailed)
                }
            }
        }
    }

    fun getCurrentRecord(): TaskResultInfo? = recordInfo

    fun updateCurrentRecord(info: TaskResultInfo?) {
        recordInfo = info
    }

    /**
     * 設定當前要 review 的學生作品。
     *
     * 取代原 `RecordMarkWidgetModel.updateStudentRecords(List<TaskResultInfo>)`
     * + `setSelectedIndex(index)` 多筆切換邏輯。
     */
    fun setRecord(info: TaskResultInfo) {
        recordInfo = info
        coroutineScope.launch {
            _uiEventFlow.emit(SketchReviewUiEvent.SetContent(info))
        }
    }

    /**
     * 合併 base color + ivImage + drawing bitmap，存到 MediaStore，再上傳 S3。
     *
     * 失敗情境（null URI / Exception）→ 立刻 emit [SketchReviewUiEvent.UploadImageFailed]
     * 讓 widget 重置 send button 並顯示 toast。
     */
    fun createMarkedBitmap(
        baseWidth: Int,
        baseHeight: Int,
        baseColor: Int,
        overlays: List<Bitmap>
    ) {
        coroutineScope.launch(ioDispatcher) {
            try {
                val merged = PaintUtils.mergeBitmaps(
                    baseWidth = baseWidth,
                    baseHeight = baseHeight,
                    baseColor = baseColor,
                    overlays = overlays
                )
                val uri = PaintUtils.saveBitmapAndGetUri(
                    context = applicationContext,
                    bitmap = merged,
                    filename = "${BITMAP_FILE_NAME_PREFIX}_${UUID.randomUUID()}"
                )
                if (uri == null) {
                    Timber.w("Sketch saveBitmapAndGetUri returned null (size=${baseWidth}x${baseHeight})")
                    withContext(mainDispatcher) {
                        _uiEventFlow.emit(SketchReviewUiEvent.UploadImageFailed)
                    }
                    return@launch
                }
                Timber.d("Sketch start upload to S3: $uri")
                withContext(mainDispatcher) {
                    uploadFileHandler.uploadImageToS3UsingContentUri(
                        lessonId = lessonId,
                        imageUrl = uri.toString()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Sketch createMarkedBitmap failed")
                withContext(mainDispatcher) {
                    _uiEventFlow.emit(SketchReviewUiEvent.UploadImageFailed)
                }
            }
        }
    }

    companion object {
        private const val BITMAP_FILE_NAME_PREFIX = "sketch_review_marked"
    }
}
