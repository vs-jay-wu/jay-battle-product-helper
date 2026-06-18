package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.viewsonic.classswift.data.info.QuizInCollectionInfo

/**
 * Multiple Choice text-quiz detail view (mvb-text-quiz-detail US-2).
 *
 * Figma node `3333-39414`: inset white card preview + scrollable list of "(A) text..." rows.
 * Single vs multi mode is conveyed by the chip text only (chip mapping handled by base class).
 *
 * `open` so future Poll text variant can inherit identical rendering — same logic as image
 * variant Poll/MC sharing in VSFT-7268.
 */
open class MvbCollectionMultipleChoiceTextQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MvbCollectionQuizDetailView(context, attrs, defStyleAttr) {

    override fun bindPreview(info: QuizInCollectionInfo) {
        renderTextInsetCard(info)
    }

    override fun bindOptions(info: QuizInCollectionInfo) {
        optionsSection.visibility = View.VISIBLE
        val rowLabels = info.quizData.optionList.mapIndexed { index, option ->
            "(${'A' + index}) ${option.content}"
        }
        renderTextOptionList(rowLabels)
    }
}
