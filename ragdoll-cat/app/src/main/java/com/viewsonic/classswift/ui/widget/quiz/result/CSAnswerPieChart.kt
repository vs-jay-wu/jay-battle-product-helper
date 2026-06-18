package com.viewsonic.classswift.ui.widget.quiz.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R

/**
 * Pie chart for Result Overview showing correct / incorrect / no-answer distribution.
 *
 * Three arcs clockwise from 12-o'clock:
 *   - correctCount → green (color_48720F) + dense dot pattern
 *   - incorrectCount → red (color_DB0025) + diagonal slash pattern
 *   - noAnswerCount → gray (neutral_500) + cross-hatch pattern
 *
 * Total == 0 → full gray circle (with cross-hatch).
 *
 * WCAG 2.1 SC 1.4.1 compliance (VSFT-7272): each segment uses a distinct tiled
 * pattern so grayscale / color-blind users can identify segments without color.
 * Patterns are built once per instance and shared with [CSResultOptionBarItem]
 * via [WcagPatternTiles] so identical colors carry identical patterns.
 */
class CSAnswerPieChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val correctPaint = createSolidPaint(R.color.color_48720F)
    private val incorrectPaint = createSolidPaint(R.color.color_DB0025)
    private val noAnswerPaint = createSolidPaint(R.color.neutral_500)
    private val segmentBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // vsds/sys/color/outline (#E5E5E5) per Figma node 6668-173835.
        color = ContextCompat.getColor(context, R.color.neutral_300)
        style = Paint.Style.STROKE
        strokeWidth = dp(SEGMENT_BORDER_WIDTH_DP)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val correctPatternPaint = buildPatternPaint(WcagPatternTiles.dots())
    private val incorrectPatternPaint = buildPatternPaint(WcagPatternTiles.slashes())
    private val noAnswerPatternPaint = buildPatternPaint(WcagPatternTiles.crossHatch())

    private val arcRect = RectF()
    private val reusablePath = android.graphics.Path()

    private var correctCount = 0
    private var incorrectCount = 0
    private var noAnswerCount = 0
    private var patternEnabled = true

    fun setData(correct: Int, incorrect: Int, noAnswer: Int) {
        correctCount = correct.coerceAtLeast(0)
        incorrectCount = incorrect.coerceAtLeast(0)
        noAnswerCount = noAnswer.coerceAtLeast(0)
        invalidate()
    }

    /**
     * Toggle WCAG pattern overlay (VSFT-7272). ON by default for WCAG compliance.
     * Pass false to render solid-color segments only (e.g. for legend swatches).
     */
    fun setPatternEnabled(enabled: Boolean) {
        if (patternEnabled == enabled) return
        patternEnabled = enabled
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = segmentBorderPaint.strokeWidth / 2f
        arcRect.set(inset, inset, width.toFloat() - inset, height.toFloat() - inset)
        val total = correctCount + incorrectCount + noAnswerCount
        if (total == 0) {
            canvas.drawArc(arcRect, 0f, FULL_CIRCLE_DEG, true, noAnswerPaint)
            if (patternEnabled) {
                canvas.drawArc(arcRect, 0f, FULL_CIRCLE_DEG, true, noAnswerPatternPaint)
            }
            canvas.drawOval(arcRect, segmentBorderPaint)
            return
        }

        val correctAngle = correctCount.toFloat() / total * FULL_CIRCLE_DEG
        val incorrectAngle = incorrectCount.toFloat() / total * FULL_CIRCLE_DEG
        val noAnswerAngle = FULL_CIRCLE_DEG - correctAngle - incorrectAngle
        val angles = listOf(correctAngle, incorrectAngle, noAnswerAngle)
        val solidPaints = listOf(correctPaint, incorrectPaint, noAnswerPaint)
        val patternPaints =
            listOf(correctPatternPaint, incorrectPatternPaint, noAnswerPatternPaint)

        var start = START_ANGLE_TOP
        for (i in angles.indices) {
            val angle = angles[i]
            if (angle <= 0f) continue
            canvas.drawArc(arcRect, start, angle, true, solidPaints[i])
            if (patternEnabled) {
                canvas.drawArc(arcRect, start, angle, true, patternPaints[i])
            }
            drawSegmentBorder(canvas, start, angle)
            start += angle
        }
    }

    private fun drawSegmentBorder(canvas: Canvas, startAngle: Float, sweepAngle: Float) {
        reusablePath.reset()
        val cx = arcRect.centerX()
        val cy = arcRect.centerY()
        reusablePath.moveTo(cx, cy)
        reusablePath.arcTo(arcRect, startAngle, sweepAngle, false)
        reusablePath.close()
        canvas.drawPath(reusablePath, segmentBorderPaint)
    }

    private fun createSolidPaint(colorRes: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, colorRes)
        style = Paint.Style.FILL
    }

    private fun buildPatternPaint(tile: Bitmap): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        style = Paint.Style.FILL
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        context.resources.displayMetrics,
    )

    companion object {
        private const val FULL_CIRCLE_DEG = 360f
        private const val START_ANGLE_TOP = -90f
        // Figma vsds/sys/border/xl = 6 Figma px / 1.5 = 4dp (per figma-design-tokens.md).
        private const val SEGMENT_BORDER_WIDTH_DP = 4f
    }
}
