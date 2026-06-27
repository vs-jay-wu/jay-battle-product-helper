package com.viewsonic.classswift.coordinator

import android.content.Context
import android.graphics.Bitmap
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.utils.PaintUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MarkToolHandler(
    private val applicationContext: Context
) {
    private val _bitmapUrlStateFlow = MutableStateFlow<String>("")
    val bitmapUriStateFlow = _bitmapUrlStateFlow.asStateFlow()
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    fun createMarkedBitmap(
        baseWidth: Int,
        baseHeight: Int,
        baseColor: Int,
        overlays: List<Bitmap>
    ) {
        coroutineScope.launch {
            val filename = DEFAULT_RECORD_MARK_FILE_NAME + "_${UUID.randomUUID()}"

            val mergeBitmap = PaintUtils.mergeBitmaps(
                baseWidth = baseWidth,
                baseHeight = baseHeight,
                baseColor = baseColor,
                overlays = overlays
            )
            val outputBitmapUri = PaintUtils.saveBitmapAndGetUri(
                context = applicationContext,
                bitmap = mergeBitmap,
                filename = filename
            )
            _bitmapUrlStateFlow.emit(outputBitmapUri.toString())
        }
    }

    companion object {
        private const val DEFAULT_RECORD_MARK_FILE_NAME = "temp_record_mark"
    }
}