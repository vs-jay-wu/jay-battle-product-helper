package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.viewsonic.classswift.data.info.QuizInCollectionInfo

/**
 * Audio detail view — VSFT-7268 AC-5.
 *
 * Reuses the base [MvbCollectionQuizDetailView] image preview rendering. Overrides
 * [bindOptions] to GONE (not the base default INVISIBLE) so the image preview claims the
 * full body width and centers via FIT_CENTER. Same pattern as the SA-text fix — when the
 * options column has no content, leaving it INVISIBLE makes the image render in the left
 * 60% body width with the right 40% blank, which reads as "image not centered" against
 * Figma node 3742-179332.
 */
class MvbCollectionAudioQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MvbCollectionQuizDetailView(context, attrs, defStyleAttr) {

    override fun bindOptions(info: QuizInCollectionInfo) {
        optionsSection.visibility = View.GONE
    }
}
