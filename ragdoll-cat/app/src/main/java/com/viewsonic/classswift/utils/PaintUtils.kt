package com.viewsonic.classswift.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.createBitmap

object PaintUtils {

    fun saveBitmapAndGetUri(
        context: Context,
        bitmap: Bitmap,
        filename: String = "bitmap_${System.currentTimeMillis()}.png"
    ): Uri? {
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ClassSwiftBitmaps")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        return uri
    }

    fun mergeBitmaps(
        baseWidth: Int,
        baseHeight: Int,
        baseColor: Int,
        overlays: List<Bitmap>
    ): Bitmap {

        val baseBitmap = createBitmap(baseWidth, baseHeight)
        val canvas = Canvas(baseBitmap)
        canvas.drawColor(baseColor)

        for (overlay in overlays) {
            canvas.drawBitmap(overlay, 0f, 0f, null)
        }

        return baseBitmap
    }

    fun removeBitmap(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver
        try {
            contentResolver.delete(uri, null, null)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}