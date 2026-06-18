package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

class MaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var maskPaint: Paint = Paint().apply {
        color = Color.WHITE
        alpha = 128 // Adjust for desired transparency (0-255)
    }

    fun setMaskColor(@ColorRes colorRes: Int, alphaInt: Int = 255) {
        val colorInt: Int = ContextCompat.getColor(context, colorRes)
        maskPaint = Paint().apply {
            color = colorInt
            alpha = alphaInt
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
    }
}