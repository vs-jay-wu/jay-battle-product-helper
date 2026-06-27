package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizInCollectionInfo

/**
 * True/False text-quiz detail view (mvb-text-quiz-detail US-1).
 *
 * Figma node `3333-40325`: inset white card preview + 2 vertical text rows ("True" / "False").
 * Reuses base [renderTextInsetCard] + [renderTextOptionList] helpers.
 */
class MvbCollectionTrueFalseTextQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MvbCollectionQuizDetailView(context, attrs, defStyleAttr) {

    override fun bindPreview(info: QuizInCollectionInfo) {
        renderTextInsetCard(info)
    }

    override fun bindOptions(info: QuizInCollectionInfo) {
        optionsSection.visibility = View.VISIBLE
        // Text variant uses full word "true" / "false" labels, distinct from the image variant's
        // single-letter "T" / "F" cards (R.string.quiz_disclose_option_label_*).
        renderTextOptionList(
            listOf(
                context.getString(R.string.mvb_qc_detail_text_tf_true_label),
                context.getString(R.string.mvb_qc_detail_text_tf_false_label),
            ),
        )
    }
}
