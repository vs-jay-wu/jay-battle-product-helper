package com.viewsonic.classswift.ui.widget.quizcollection

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.databinding.ViewCsCollectionShortAnswerTextQuizDetailBinding
import com.viewsonic.classswift.ui.widget.fastscroll.FastScrollerBuilder
import com.viewsonic.classswift.ui.widget.fastscroll.ScrollingViewOnApplyWindowInsetsListener
import com.viewsonic.classswift.utils.extension.isLatexContent


class CSCollectionShortAnswerTextQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewCsCollectionShortAnswerTextQuizDetailBinding =
        ViewCsCollectionShortAnswerTextQuizDetailBinding.inflate(LayoutInflater.from(context), this)


    init {
        background = ContextCompat.getDrawable(context, R.drawable.bg_neutral0_radius800_line_neutral_500_border200)
        binding.fsvScrollView.setOnApplyWindowInsetsListener(ScrollingViewOnApplyWindowInsetsListener())
        val trackDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.selector_quiz_collection_detail_scroll_track
        )

        val thumbDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.selector_quiz_collection_detail_scroll_thumb
        )

        if (trackDrawable != null && thumbDrawable != null) {
            FastScrollerBuilder(
                context,
                binding.fsvScrollView,
                trackDrawable,
                thumbDrawable
            ).build()
        }
    }

    fun updateView(quizInCollectionInfo: QuizInCollectionInfo, isAnswerRevealed: Boolean) {
        with(binding) {
            tvQuizTag.text = quizInCollectionInfo.subjectDisplayName
            val isContentLatexContent = quizInCollectionInfo.quizData.content.isLatexContent()
            tvQuizContent.isVisible = !isContentLatexContent
            cskvQuizContent.isVisible = isContentLatexContent
            when (isContentLatexContent) {
                true -> {
                    cskvQuizContent.setText(quizInCollectionInfo.quizData.content)
                }
                false -> {
                    tvQuizContent.text = quizInCollectionInfo.quizData.content
                }
            }
            val isReasonLatexContent = quizInCollectionInfo.quizData.shortAnswer.answer.isLatexContent()
            val wasReasonLatexVisible = cskvReason.isVisible
            tvReason.isVisible = isAnswerRevealed && !isReasonLatexContent
            cskvReason.isVisible = isAnswerRevealed && isReasonLatexContent
            when (isReasonLatexContent) {
                true -> {
                    cskvReason.setText(quizInCollectionInfo.quizData.shortAnswer.answer)
                    if (isAnswerRevealed && !wasReasonLatexVisible) {
                        cskvReason.refreshContentHeight()
                    }
                }
                false -> {
                    tvReason.text = quizInCollectionInfo.quizData.shortAnswer.answer
                }
            }
        }
    }

    fun release() {
        with(binding) {
            cskvQuizContent.release()
            cskvReason.release()
        }
    }
}
