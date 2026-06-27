package com.viewsonic.classswift.ui.widgetmodel

import android.graphics.Bitmap
import com.viewsonic.classswift.coordinator.MarkToolHandler
import com.viewsonic.classswift.coordinator.UploadFileHandler
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widgetmodel.records.state.RecordMarkUiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.collections.mutableListOf

class RecordMarkWidgetModel(
    private val uploadFileHandler: UploadFileHandler,
    private val markToolHandler: MarkToolHandler,
    private val classroomManager: ClassroomManager,
) {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val lessonId = classroomManager.classroomDataStateFlow.value
        .selectedClassroomInfo.lessonId

    private var recordInfo: TaskResultInfo? = null
    private val recordInfoList = mutableListOf<TaskResultInfo>()
    private var currentRecordIndex = 0

    private val _uiEventFlow = MutableSharedFlow<RecordMarkUiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val uiEventFlow = _uiEventFlow.asSharedFlow()

    init {
        observeData()
    }

    private fun observeData() {
        coroutineScope.launch(Dispatchers.Main) {
            uploadFileHandler.uploadImageSharedFlow.collect { isSuccess ->

                Timber.d("Upload image status: $isSuccess")
                when (isSuccess) {
                    true -> {
                        _uiEventFlow.emit(
                            RecordMarkUiEvent.UploadImageSuccess(
                                imgUrl = uploadFileHandler.awsPreSignedUrl.s3GetUrl
                            )
                        )
                    }

                    false -> {
                        _uiEventFlow.emit(RecordMarkUiEvent.UploadImageFailed)
                    }
                }
            }
        }

        coroutineScope.launch {
            markToolHandler.bitmapUriStateFlow.collect { uri ->
                if (uri.isNotEmpty()) {
                    startUploadImageToS3(uri = uri)
                }
            }
        }
    }

    fun getCurrentRecord(): TaskResultInfo? {
        return recordInfo
    }

    fun updateCurrentRecord(info: TaskResultInfo) {
        recordInfo = info
    }

    fun updateStudentRecords(infoList: List<TaskResultInfo>) {
        recordInfoList.clear()
        recordInfoList.addAll(infoList)
        val safeIndex = coerceIndexOrNull(recordInfoList.size, currentRecordIndex)
        if (safeIndex == null) {
            recordInfo = null
            currentRecordIndex = 0
            return
        }
        currentRecordIndex = safeIndex
        updateCurrentRecord()
    }

    fun setSelectedIndex(index: Int) {
        val safeIndex = coerceIndexOrNull(recordInfoList.size, index) ?: return
        currentRecordIndex = safeIndex
        switchCurrentRecord()
    }

    fun switchRecord(isNext: Boolean) {
        if (recordInfoList.isEmpty()) return
        currentRecordIndex = when {
            isNext && currentRecordIndex == recordInfoList.lastIndex -> 0
            !isNext && currentRecordIndex == 0 -> recordInfoList.lastIndex
            isNext -> currentRecordIndex + 1
            else -> currentRecordIndex - 1
        }
        switchCurrentRecord()
    }

    private fun switchCurrentRecord() {
        val newRecord = recordInfoList.getOrNull(currentRecordIndex)
        if (newRecord == null) {
            recordInfo = null
            return
        }
        recordInfo = newRecord
        coroutineScope.launch {
            _uiEventFlow.emit(
                RecordMarkUiEvent.SwitchContent(newRecord)
            )
        }
    }

    private fun updateCurrentRecord() {
        if (recordInfoList.isEmpty()) {
            recordInfo = null
            return
        }
        val updateRecord = recordInfoList[currentRecordIndex]
        recordInfo?.let { currentInfo ->
            if (currentInfo is TaskResultInfo.Content && updateRecord is TaskResultInfo.Guest) {
                coroutineScope.launch {
                    _uiEventFlow.emit(
                        RecordMarkUiEvent.ReleaseSeat(currentInfo, updateRecord)
                    )
                }
                return@let
            }
            Timber.tag("updateRecord").d("currentInfo: $currentInfo")
            Timber.tag("updateRecord").d("updateRecord: $updateRecord")
            if (currentInfo != updateRecord) {
                coroutineScope.launch {
                    Timber.tag("updateRecord").d("emit UpdateCurrentRecord")
                    _uiEventFlow.emit(
                        RecordMarkUiEvent.UpdateCurrentRecord(updateRecord)
                    )
                }
            }
        }
        recordInfo = updateRecord
    }

    fun createMarkedBitmap(
        baseWidth: Int,
        baseHeight: Int,
        baseColor: Int,
        overlays: List<Bitmap>
    ) {
        markToolHandler.createMarkedBitmap(
            baseWidth = baseWidth,
            baseHeight = baseHeight,
            baseColor = baseColor,
            overlays = overlays
        )
    }

    private fun startUploadImageToS3(uri: String) {
        Timber.d("Start Upload ImageTo S3, localImagePath = $uri")
        uploadFileHandler.uploadImageToS3UsingContentUri(
            lessonId = lessonId,
            imageUrl = uri
        )
    }

    companion object {
        /**
         * 把 [index] 夾到合法區間 `[0, listSize - 1]` 後回傳；
         * 當 [listSize] `<= 0`（空 list）時回傳 `null`，呼叫端可據此判斷需清空狀態。
         */
        internal fun coerceIndexOrNull(listSize: Int, index: Int): Int? =
            if (listSize <= 0) null else index.coerceIn(0, listSize - 1)
    }
}
