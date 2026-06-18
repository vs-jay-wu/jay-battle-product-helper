package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.viewsonic.classswift.data.info.QuizInCollectionInfo

/**
 * Short answer detail view (image variant) — VSFT-7268 AC-4.
 *
 * Reuses the base [MvbCollectionQuizDetailView] image preview rendering. Overrides
 * [bindOptions] to GONE (not the base default INVISIBLE) so the image preview claims the
 * full body width and centers via FIT_CENTER. Same fix as `MvbCollectionAudioQuizDetailView`
 * — leaving options INVISIBLE renders the image in the left 60% body width with the right
 * 40% blank, which reads as "image not centered".
 */
class MvbCollectionShortAnswerImageQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MvbCollectionQuizDetailView(context, attrs, defStyleAttr) {

    override fun bindOptions(info: QuizInCollectionInfo) {
        optionsSection.visibility = View.GONE
    }
}
