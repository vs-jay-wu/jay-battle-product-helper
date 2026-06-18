package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import com.viewsonic.classswift.data.info.QuizInCollectionInfo

/**
 * Multiple choice detail view (single + multi answer) — VSFT-7268 AC-2.
 *
 * Renders A-F option cards in a 3-column grid (count driven by `quizData.optionList.size`).
 * Single vs multi mode is conveyed by the chip text only (chip mapping handled by base class).
 *
 * `open` so Poll variant can inherit identical rendering without re-declaring `bindOptions`
 * (avoids S4144 — Functions should not have identical implementations).
 */
open class MvbCollectionMultipleChoiceQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MvbCollectionQuizDetailView(context, attrs, defStyleAttr) {

    override fun bindOptions(info: QuizInCollectionInfo) {
        renderOptionGrid(info)
    }
}
