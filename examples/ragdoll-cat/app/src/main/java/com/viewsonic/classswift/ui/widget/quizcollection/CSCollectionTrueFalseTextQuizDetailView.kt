package com.viewsonic.classswift.ui.widget.quizcollection

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.databinding.ViewCsCollectionTrueFalseTextQuizDetailBinding
import com.viewsonic.classswift.ui.widget.fastscroll.FastScrollerBuilder
import com.viewsonic.classswift.ui.widget.fastscroll.ScrollingViewOnApplyWindowInsetsListener
import com.viewsonic.classswift.utils.extension.isLatexContent
import timber.log.Timber


class CSCollectionTrueFalseTextQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewCsCollectionTrueFalseTextQuizDetailBinding =
        ViewCsCollectionTrueFalseTextQuizDetailBinding.inflate(LayoutInflater.from(context), this)

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
            when (quizInCollectionInfo.quizData.content.isLatexContent()) {
                true -> {
                    binding.tvQuizContent.isVisible = false
                    binding.cskvQuizContent.isVisible = true
                    binding.cskvQuizContent.setText(quizInCollectionInfo.quizData.content)
                }
                false -> {
                    binding.tvQuizContent.isVisible = true
                    binding.cskvQuizContent.isVisible = false
                    binding.tvQuizContent.text = quizInCollectionInfo.quizData.content
                }
            }

            // True Option
            quizInCollectionInfo.quizData.optionList.getOrNull(0)?.let { option ->
                csctqovTrueOption.updateOptionInfo(option.optionId, option.content, option.reason, option.isAiAnswer || option.isAnswer)
            }
            // False Option
            quizInCollectionInfo.quizData.optionList.getOrNull(1)?.let { option ->
                csctqovFalseOption.updateOptionInfo(option.optionId, option.content, option.reason, option.isAiAnswer || option.isAnswer)
            }
            when (isAnswerRevealed) {
                true -> {
                    csctqovTrueOption.revealAnswer()
                    csctqovFalseOption.revealAnswer()
                }
                false -> {
                    csctqovTrueOption.hideAnswer()
                    csctqovFalseOption.hideAnswer()
                }
            }
        }
    }
    
    fun release() {
        with(binding) {
            cskvQuizContent.release()
            csctqovTrueOption.release()
            csctqovFalseOption.release()
        }
    }
}
