package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.utils.extension.dpToPx

/**
 * Short Answer text-quiz detail view (mvb-text-quiz-detail US-3).
 *
 * Figma node `3754-184747`: inset white card preview centered horizontally with fixed width
 * 736px ÷ 1.5 = 491dp. Options section is GONE (no answer rows for SA), so the centered card
 * visually occupies the body without the right-column reservation that image variants use.
 */
class MvbCollectionShortAnswerTextQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MvbCollectionQuizDetailView(context, attrs, defStyleAttr) {

    override fun bindPreview(info: QuizInCollectionInfo) {
        // Clear the default white preview bg drawable. With options GONE, the preview spans the
        // full body width — letting that white show would create a second white container around
        // the centered 491dp card. Letting it through reveals the body's neutral_100 fill instead.
        previewContainer.background = null
        renderTextInsetCard(
            info = info,
            cardWidthPx = SA_CARD_WIDTH_DP.dpToPx().toInt(),
            cardLayoutGravity = Gravity.CENTER_HORIZONTAL,
        )
    }

    override fun bindOptions(info: QuizInCollectionInfo) {
        optionsSection.visibility = View.GONE
    }

    companion object {
        // Figma node 3754-184747 spec: W Fixed 736px → 736 ÷ 1.5 = 490.67dp
        private const val SA_CARD_WIDTH_DP = 491f
    }
}
