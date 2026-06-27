package com.viewsonic.classswift.ui.window.quiz.start

import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.viewsonic.classswift.databinding.WindowMvbTextQuizBinding
import com.viewsonic.classswift.ui.widget.KatexView
import kotlin.math.roundToInt

/**
 * Positions the pre-styled native [KatexView]s in [WindowMvbTextQuizBinding] over the transparent
 * holes the Compose panel ([com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizStartScreen])
 * leaves for LaTeX — needed because this overlay window is not hardware-accelerated, so a WebView
 * only paints as a direct native child (not nested inside the ComposeView).
 *
 * Keys: `question` (the fixed-height question box) and `content_<i>` / `reason_<i>` for the two
 * disclose option rows. The disclose holes are WRAP_CONTENT; each KatexView's measured height is fed
 * back into [heights] so the Compose hole grows to fit the rendered LaTeX. Shared by the live
 * [MvbTextTrueFalseStartWindow] and the debug window.
 */
class MvbTextQuizKatexOverlay(private val binding: WindowMvbTextQuizBinding) {

    /** Measured LaTeX heights (dp) for the dynamic holes; read by the Compose `latexHeight` lambda. */
    val heights = mutableStateMapOf<String, Dp>()

    private val density = binding.root.resources.displayMetrics.density
    private val listenerKeys = mutableSetOf<String>()
    private val lastRect = mutableMapOf<String, IntArray>() // [left, top, width]

    // While an answer popup (a Compose overlay inside the ComposeView) is open, the native KatexViews
    // — siblings drawn ON TOP of the ComposeView — must be hidden, else they bleed over the popup scrim.
    private var suppressed = false

    private fun viewFor(key: String): KatexView? = when (key) {
        "question" -> binding.cskvQuestion
        "content_0" -> binding.cskvContent0
        "content_1" -> binding.cskvContent1
        "reason_0" -> binding.cskvReason0
        "reason_1" -> binding.cskvReason1
        else -> null
    }

    /**
     * Place [key]'s KatexView at [pos] with [widthPx]. [fixedHeightPx] != null → fixed box height
     * (the question), null → WRAP_CONTENT with the measured height fed back into [heights].
     */
    fun position(key: String, text: String, pos: Offset, widthPx: Int, fixedHeightPx: Int?) {
        val view = viewFor(key) ?: return
        val left = pos.x.roundToInt()
        val top = pos.y.roundToInt()
        val wantVisible = if (suppressed) View.GONE else View.VISIBLE
        val prev = lastRect[key]
        val moved = prev == null || prev[0] != left || prev[1] != top || prev[2] != widthPx
        if (moved || view.visibility != wantVisible) {
            lastRect[key] = intArrayOf(left, top, widthPx)
            val lp = view.layoutParams as FrameLayout.LayoutParams
            lp.width = widthPx
            lp.height = fixedHeightPx ?: FrameLayout.LayoutParams.WRAP_CONTENT
            lp.leftMargin = left
            lp.topMargin = top
            view.layoutParams = lp
            view.visibility = wantVisible
        }
        view.setText(text)
        if (fixedHeightPx == null && listenerKeys.add(key)) {
            view.addOnLayoutChangeListener { _, _, t, _, b, _, _, _, _ ->
                val measured = ((b - t) / density).dp
                if (heights[key] != measured) heights[key] = measured
            }
        }
    }

    fun hide(key: String) {
        viewFor(key)?.visibility = View.GONE
        lastRect.remove(key)
    }

    /** Hide all overlays while a Compose answer popup is open (they would otherwise draw over its
     *  scrim); restore the previously-positioned ones when it closes. */
    fun setSuppressed(value: Boolean) {
        if (suppressed == value) return
        suppressed = value
        val visibility = if (value) View.GONE else View.VISIBLE
        lastRect.keys.forEach { viewFor(it)?.visibility = visibility }
    }

    fun release() {
        listOf("question", "content_0", "content_1", "reason_0", "reason_1").forEach { viewFor(it)?.release() }
    }
}
