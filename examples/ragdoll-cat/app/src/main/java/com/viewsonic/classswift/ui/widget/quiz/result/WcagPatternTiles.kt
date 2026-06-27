package com.viewsonic.classswift.ui.widget.quiz.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.utils.extension.dpToPx

/**
 * Tile bitmaps for VSFT-7272 WCAG pattern overlays.
 *
 * Shared by [CSAnswerPieChart] and [CSResultOptionBarItem] so segments with the
 * same color carry the same pattern:
 *  - correct (green)    → dense dots
 *  - incorrect (red)    → single-direction slashes
 *  - no-answer (gray)   → cross-hatch
 *
 * Each pattern is a small square bitmap meant to be tiled via
 * [android.graphics.BitmapShader] with `TileMode.REPEAT`. Patterns are drawn
 * with a low-alpha black so the underlying arc/bar color remains vivid while
 * staying distinguishable in grayscale / color-blind modes.
 */
object WcagPatternTiles {

    /** Dense dots — associated with the correct / answered-correctly state. */
    fun dots(): Bitmap {
        val size = tileSizePx()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PATTERN_COLOR_ARGB
            style = Paint.Style.FILL
        }
        val r = DOT_RADIUS_DP.dpToPx().coerceAtLeast(MIN_DOT_RADIUS_PX)
        canvas.drawCircle(size * 0.25f, size * 0.25f, r, paint)
        canvas.drawCircle(size * 0.75f, size * 0.75f, r, paint)
        return bitmap
    }

    /** Single-direction diagonal slashes — associated with incorrect / wrong. */
    fun slashes(): Bitmap {
        val size = tileSizePx()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = linePaint()
        canvas.drawLine(0f, size.toFloat(), size.toFloat(), 0f, paint)
        canvas.drawLine(-size * 0.5f, size * 0.5f, size * 0.5f, -size * 0.5f, paint)
        canvas.drawLine(size * 0.5f, size * 1.5f, size * 1.5f, size * 0.5f, paint)
        return bitmap
    }

    /** Diagonal cross-hatch — associated with no-answer / not-submitted. */
    fun crossHatch(): Bitmap {
        val size = tileSizePx()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = linePaint()
        // Forward slashes
        canvas.drawLine(0f, size.toFloat(), size.toFloat(), 0f, paint)
        canvas.drawLine(-size * 0.5f, size * 0.5f, size * 0.5f, -size * 0.5f, paint)
        canvas.drawLine(size * 0.5f, size * 1.5f, size * 1.5f, size * 0.5f, paint)
        // Back slashes
        canvas.drawLine(0f, 0f, size.toFloat(), size.toFloat(), paint)
        canvas.drawLine(-size * 0.5f, -size * 0.5f, size * 0.5f, size * 0.5f, paint)
        canvas.drawLine(size * 0.5f, size * 0.5f, size * 1.5f, size * 1.5f, paint)
        return bitmap
    }

    /**
     * Pie-chart legend swatch (the small colored squares under the pie chart).
     * Combines the legend color with the matching WCAG pattern so the swatch
     * stays distinguishable in grayscale / color-blind modes — same convention
     * as [CSAnswerPieChart] arcs and [CSResultOptionBarItem] bars.
     */
    enum class LegendStyle { CORRECT, INCORRECT, NO_ANSWER }

    fun buildLegendSwatch(context: Context, style: LegendStyle): Drawable {
        val sizePx = LEGEND_SWATCH_SIZE_DP.dpToPx().toInt().coerceAtLeast(MIN_TILE_PX)
        val cornerPx = LEGEND_SWATCH_CORNER_DP.dpToPx()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())

        val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, swatchColorRes(style))
            this.style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cornerPx, cornerPx, colorPaint)

        val patternTile = when (style) {
            LegendStyle.CORRECT -> dots()
            LegendStyle.INCORRECT -> slashes()
            LegendStyle.NO_ANSWER -> crossHatch()
        }
        val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(patternTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
        canvas.drawRoundRect(rect, cornerPx, cornerPx, patternPaint)

        return BitmapDrawable(context.resources, bitmap).apply {
            setBounds(0, 0, sizePx, sizePx)
        }
    }

    private fun swatchColorRes(style: LegendStyle): Int = when (style) {
        LegendStyle.CORRECT -> R.color.color_48720F
        LegendStyle.INCORRECT -> R.color.color_DB0025
        LegendStyle.NO_ANSWER -> R.color.neutral_500
    }

    private fun tileSizePx(): Int =
        TILE_SIZE_DP.dpToPx().toInt().coerceAtLeast(MIN_TILE_PX)

    private fun linePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PATTERN_COLOR_ARGB
        style = Paint.Style.STROKE
        strokeWidth = LINE_WIDTH_DP.dpToPx().coerceAtLeast(MIN_LINE_WIDTH_PX)
    }

    private const val TILE_SIZE_DP = 8f
    private const val DOT_RADIUS_DP = 1f
    private const val LINE_WIDTH_DP = 1f
    private const val LEGEND_SWATCH_SIZE_DP = 16f
    private const val LEGEND_SWATCH_CORNER_DP = 1.33f
    private const val MIN_TILE_PX = 16
    private const val MIN_DOT_RADIUS_PX = 2f
    private const val MIN_LINE_WIDTH_PX = 2f
    // Black at ~8% alpha — keeps the arc/bar color vivid; pattern stays
    // just visible enough to aid grayscale / color-blind identification.
    private const val PATTERN_COLOR_ARGB = 0x14000000.toInt()
}
