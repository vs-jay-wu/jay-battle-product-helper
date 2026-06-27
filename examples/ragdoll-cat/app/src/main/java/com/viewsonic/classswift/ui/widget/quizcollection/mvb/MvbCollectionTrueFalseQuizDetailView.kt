package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.utils.extension.dpToPx

/**
 * True / False detail view — VSFT-7268 AC-1.
 *
 * Renders 2 option cards (T / F) horizontally inside the options content area.
 */
class MvbCollectionTrueFalseQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MvbCollectionQuizDetailView(context, attrs, defStyleAttr) {

    override fun bindOptions(info: QuizInCollectionInfo) {
        optionsSection.visibility = View.VISIBLE
        optionsContent.removeAllViews()

        val container = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
            orientation = LinearLayout.HORIZONTAL
        }

        val gap = OPTION_GAP_DP.dpToPx().toInt()
        val height = OPTION_SIZE_DP.dpToPx().toInt()
        listOf("T", "F").forEachIndexed { index, label ->
            val card = createOptionCard(label)
            val params = LinearLayout.LayoutParams(0, height, 1f)
            if (index > 0) params.marginStart = gap
            container.addView(card, params)
        }

        optionsContent.addView(container)
    }
}
