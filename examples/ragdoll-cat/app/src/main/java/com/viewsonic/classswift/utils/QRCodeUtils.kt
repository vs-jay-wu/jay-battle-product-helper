package com.viewsonic.classswift.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

object QRCodeUtils {

    fun generateQRCode(text: String, qrSize: Int): Bitmap? {
        return try {
            val qrCodeWriter = QRCodeWriter()

            // Hints to reduce the QR code margins (optional)
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.MARGIN, 1)  // Smaller margin
            }

            // Generate BitMatrix from the input text
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, qrSize, qrSize, hints)

            // Convert BitMatrix to Bitmap
            val bitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
            for (x in 0 until qrSize) {
                for (y in 0 until qrSize) {
                    // Fill black for true bits, white for false
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    fun generateQRCodeWithBackground(
        text: String,
        qrSize: Int,
        bgRadius: Float,
        bgColor: Int = Color.WHITE,
        qrColor: Int = Color.BLACK
    ): Bitmap? {
        return try {
            val qrCodeWriter = QRCodeWriter()

            // Generate QR Code BitMatrix
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.MARGIN, 1)  // Minimum margins
            }
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, qrSize, qrSize, hints)

            // Create empty bitmap for QR code + background
            val bitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw rounded background
            val paint = Paint().apply {
                color = bgColor
                isAntiAlias = true
            }
            val rectF = RectF(0f, 0f, qrSize.toFloat(), qrSize.toFloat())
            canvas.drawRoundRect(rectF, bgRadius, bgRadius, paint)

            // Draw QR Code on top
            for (x in 0 until qrSize) {
                for (y in 0 until qrSize) {
                    if (bitMatrix[x, y]) {
                        bitmap.setPixel(x, y, qrColor)
                    }
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }
}