package com.viewsonic.classswift.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import com.viewsonic.classswift.utils.extension.doRecycle
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object ImageUtil {
    /**
     * 處理圖片成為相對應的大小
     */
    fun resizeAndPadBitmap(source: Bitmap, targetWidth: Int = 1920, targetHeight: Int = 1080): Bitmap {
        // 計算縮放比例
        val scale = minOf(
            targetWidth.toFloat() / source.width,
            targetHeight.toFloat() / source.height
        )

        // 計算縮放後的寬高
        val scaledWidth = (source.width * scale).toInt()
        val scaledHeight = (source.height * scale).toInt()

        // 創建縮放後的 Bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)

        // 創建目標大小的空白 Bitmap
        val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

        // 計算偏移量（居中位置）
        val offsetX = (targetWidth - scaledWidth) / 2
        val offsetY = (targetHeight - scaledHeight) / 2

        // 在空白 Bitmap 上繪製縮放後的圖片
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE) // 填充背景色（可更改為其他顏色）
        canvas.drawBitmap(scaledBitmap, offsetX.toFloat(), offsetY.toFloat(), null)

        return resultBitmap
    }


    fun cropImageAndResizeToStorage(cropImagePath: String, bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Boolean {
        val validX = max(0, min(x, bitmap.width - 1))  // 確保 x 不超出
        val validY = max(0, min(y, bitmap.height - 1))  // 確保 y 不超出
        val validWidth = max(0, min(width, bitmap.width - validX))  // 確保 width 不超出
        val validHeight = max(0, min(height, bitmap.height - validY))  // 確保 height 不超出
        if (validWidth <= 0 || validHeight <= 0) {
            return false
        }
        Timber.d("[**][ImageUtil][59] : valid : x:$validX, y:$validY, width:$validWidth, height:$validHeight")
        val croppedBitmap = Bitmap.createBitmap(validWidth, validHeight, Bitmap.Config.ARGB_8888)
        val rect = Rect(validX, validY, validX + validWidth, validY + validHeight)
        val dstRect = Rect(0, 0, validWidth, validHeight)
        val croppedCanvas = Canvas(croppedBitmap)
        croppedCanvas.drawBitmap(bitmap, rect, dstRect, null)
        var reSizeBitmap: Bitmap? = null
        try {
            Timber.d("[**][ImageUtil][63] : start resize")
            reSizeBitmap = resizeAndPadBitmap(croppedBitmap)
            Timber.d("[**][ImageUtil][65] : finish resize")
            FileOutputStream(cropImagePath).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    reSizeBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                }
            }
            return true
        } catch (e: Exception) {
            Timber.e("** Error in crop completion", e)
        } finally {
            reSizeBitmap?.doRecycle()
            croppedBitmap.doRecycle()
            bitmap.doRecycle()
        }
        return false
    }
}