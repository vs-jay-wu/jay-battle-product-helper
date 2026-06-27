package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import kotlin.math.min


class CircleProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var progress = 75
    private var rect: Rect = Rect()

    init {
        // set Attr variable
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircleProgressBar,
            0,
            0
        ).let {
            val progress = it.getInt(R.styleable.CircleProgressBar_progress, 0)
            setProgress(progress)
            it.recycle()
        }
    }

    private val circlePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f // Circle stroke width
        color = ContextCompat.getColor(context, R.color.window_edit_quiz_progress_bar)
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 1f // Text stroke width
        color = ContextCompat.getColor(context, R.color.window_edit_quiz_progress_bar)
        textSize = 20f // Adjust text size as needed
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = (min(w, h) / 2f) - circlePaint.strokeWidth / 2f
        centerX = w / 2f
        centerY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the progress arc
        val startAngle = -90f // Start from top
        val sweepAngle = progress/100f * 360f
        canvas.drawArc(
            centerX - radius, centerY - radius,
            centerX + radius, centerY + radius,
            startAngle, sweepAngle,
            false, circlePaint
        )

        // Draw the percentage text
        val formattedProgress = "$progress%"
        val textBounds = rect
        textPaint.getTextBounds(formattedProgress, 0, formattedProgress.length, textBounds)
        canvas.drawText(
            formattedProgress,
            centerX,
            centerY - textBounds.exactCenterY(), // Center text vertically
            textPaint
        )
    }

    /**
     * [progress]: from 0 to 100
     */
    fun setProgress(progress: Int) {
        this.progress = progress.coerceIn(0, 100)
        invalidate()
    }
}