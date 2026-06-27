package com.viewsonic.classswift.ui.widget.quiz.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WidgetResultOptionBarItemBinding
import com.viewsonic.classswift.utils.extension.dpToPx

/**
 * Single row of Result-stage option statistics bar.
 *
 * Layout: label chip + (optional) correct-answer chip + responses count chip on row 1,
 * background container + foreground ratio bar on row 2.
 *
 * Ratio bar is styled via a LayerDrawable (solid color + WCAG pattern) so the
 * pattern matches the pie chart segments for the same state (VSFT-7272):
 *   CORRECT    → green + dots
 *   INCORRECT  → red + slashes
 *   NEUTRAL    → gray + cross-hatch
 *
 * Tap toggles highlight selection; caller drives `isHighlighted` via `setData()`.
 */
class CSResultOptionBarItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class BarStyle { CORRECT, INCORRECT, NEUTRAL }

    data class Data(
        val optionId: Int,
        val label: String,
        val isCorrect: Boolean,
        val responseCount: Int,
        val maxCount: Int,
        val style: BarStyle,
        val isHighlighted: Boolean = false,
    )

    private val binding: WidgetResultOptionBarItemBinding
    private var optionId: Int = -1

    private val correctPatternTile = WcagPatternTiles.dots()
    private val incorrectPatternTile = WcagPatternTiles.slashes()
    private val neutralPatternTile = WcagPatternTiles.crossHatch()

    var onBarClick: ((optionId: Int) -> Unit)? = null

    init {
        orientation = VERTICAL
        binding = WidgetResultOptionBarItemBinding.inflate(LayoutInflater.from(context), this, true)
        setOnClickListener { onBarClick?.invoke(optionId) }
    }

    fun setData(data: Data) {
        optionId = data.optionId
        binding.tvResultBarLabelChip.text = data.label
        applyLabelChipShape(data.label)
        binding.tvResultBarCorrectChip.visibility = if (data.isCorrect) View.VISIBLE else View.GONE
        binding.tvResultBarResponsesChip.text = resources.getQuantityString(
            R.plurals.quiz_mvb_result_responses_count,
            data.responseCount,
            data.responseCount,
        )
        applyBarStyle(data.style)
        applyRatio(data.responseCount, data.maxCount)
        binding.llResultOptionBarRoot.alpha = if (data.isHighlighted) SELECTED_ALPHA else UNSELECTED_ALPHA
    }

    /**
     * Single-char labels render as a perfect circle (13.33dp square);
     * multi-char labels (e.g. "Not submitted") expand horizontally as a pill.
     */
    private fun applyLabelChipShape(label: String) {
        val chip = binding.tvResultBarLabelChip
        val lp = chip.layoutParams
        if (label.length <= 1) {
            lp.width = CHIP_CIRCLE_SIZE_DP.dpToPx().toInt()
            chip.setPadding(0, 0, 0, 0)
        } else {
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            val padPx = CHIP_MULTICHAR_PADDING_DP.dpToPx().toInt()
            chip.setPadding(padPx, 0, padPx, 0)
        }
        chip.layoutParams = lp
    }

    private fun applyBarStyle(style: BarStyle) {
        val colorRes = when (style) {
            BarStyle.CORRECT -> R.color.color_48720F
            BarStyle.INCORRECT -> R.color.color_DB0025
            BarStyle.NEUTRAL -> R.color.neutral_500
        }
        val patternTile = when (style) {
            BarStyle.CORRECT -> correctPatternTile
            BarStyle.INCORRECT -> incorrectPatternTile
            BarStyle.NEUTRAL -> neutralPatternTile
        }
        binding.vResultBarRatio.background = buildRatioBackground(colorRes, patternTile)
    }

    /**
     * Build a [LayerDrawable] with a rounded solid color base and a rounded
     * tiled-pattern overlay. Both layers share the same corner radii so the
     * pattern is clipped to the rounded rectangle.
     */
    private fun buildRatioBackground(colorRes: Int, patternTile: Bitmap): LayerDrawable {
        val radiusPx = RATIO_CORNER_RADIUS_DP.dpToPx()
        val radii = FloatArray(CORNER_RADII_SIZE) { radiusPx }
        val colorLayer = ShapeDrawable(RoundRectShape(radii, null, null)).apply {
            paint.color = ContextCompat.getColor(context, colorRes)
        }
        val patternLayer = ShapeDrawable(RoundRectShape(radii, null, null)).apply {
            paint.shader = BitmapShader(
                patternTile,
                Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT,
            )
        }
        return LayerDrawable(arrayOf(colorLayer, patternLayer))
    }

    private fun applyRatio(count: Int, max: Int) {
        binding.vResultBarRatio.post {
            val parentWidth = (binding.vResultBarRatio.parent as View).width
            val minVisibleBarWidthPx = MIN_VISIBLE_BAR_WIDTH_DP.dpToPx().toInt()
            val targetWidth = when {
                max <= 0 -> 0
                count <= 0 -> minVisibleBarWidthPx
                else -> (count.toFloat() / max * parentWidth).toInt().coerceAtLeast(minVisibleBarWidthPx)
            }
            val lp = binding.vResultBarRatio.layoutParams
            lp.width = targetWidth
            binding.vResultBarRatio.layoutParams = lp
        }
    }

    companion object {
        // Minimum visible bar width at 0/tiny counts (Figma: 8 Figma px -> 5.33dp).
        private const val MIN_VISIBLE_BAR_WIDTH_DP = 5.33f
        private const val SELECTED_ALPHA = 1.0f
        private const val UNSELECTED_ALPHA = 0.2f
        private const val CHIP_CIRCLE_SIZE_DP = 13.33f
        private const val CHIP_MULTICHAR_PADDING_DP = 5.33f
        // Matches the corner radius of the original bg_result_bar_* drawables.
        private const val RATIO_CORNER_RADIUS_DP = 5.33f
        private const val CORNER_RADII_SIZE = 8
    }
}
