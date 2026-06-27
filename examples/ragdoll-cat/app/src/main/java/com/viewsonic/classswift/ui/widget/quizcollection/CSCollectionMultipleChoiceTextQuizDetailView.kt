package com.viewsonic.classswift.ui.widget.quizcollection

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.databinding.ViewCsCollectionMultipleChoiceTextQuizDetailBinding
import com.viewsonic.classswift.ui.widget.fastscroll.FastScrollerBuilder
import com.viewsonic.classswift.ui.widget.fastscroll.ScrollingViewOnApplyWindowInsetsListener
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType
import com.viewsonic.classswift.utils.extension.isLatexContent


class CSCollectionMultipleChoiceTextQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewCsCollectionMultipleChoiceTextQuizDetailBinding =
        ViewCsCollectionMultipleChoiceTextQuizDetailBinding.inflate(LayoutInflater.from(context), this)

    private val csctqovOptionList: List<CSCollectionTextQuizOptionView>

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
        csctqovOptionList = listOf(
            binding.csctqovOption1,
            binding.csctqovOption2,
            binding.csctqovOption3,
            binding.csctqovOption4,
            binding.csctqovOption5,
            binding.csctqovOption6,
        )
    }

    fun updateView(quizInCollectionInfo: QuizInCollectionInfo, isAnswerRevealed: Boolean) {
        val quizType = QuizType.entries.find { it.name == quizInCollectionInfo.quizData.quizType } ?: QuizType.UNSPECIFIED
        var optionValueType: OptionValueType = when (QuizOptionType.safeValueOf(quizInCollectionInfo.quizData.optionType)) {
            QuizOptionType.ALPHABET -> OptionValueType.ALPHABET
            else -> OptionValueType.NUMBER
        }

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
            tvQuizTypeOption.text = if (quizType == QuizType.SINGLE_SELECT) {
                context.getString(R.string.quiz_types_multiple_choice_single_answer)
            } else {
                context.getString(R.string.multiple_answer)
            }
            csctqovOptionList.forEach { it.isGone = true }
            quizInCollectionInfo.quizData.optionList.forEachIndexed { index, option ->
                val prefix = when (optionValueType) {
                    OptionValueType.NUMBER   -> (index + 1).toString()
                    OptionValueType.ALPHABET -> ('A' + index).toString()
                }
                val content = "($prefix) ${option.content}"
                csctqovOptionList[index].updateOptionInfo(option.optionId, content, option.reason, option.isAiAnswer || option.isAnswer)
                csctqovOptionList[index].isGone = false
                when (isAnswerRevealed) {
                    true -> {
                        csctqovOptionList[index].revealAnswer()
                    }
                    false -> {
                        csctqovOptionList[index].hideAnswer()
                    }
                }
            }
        }
    }

    fun release() {
        with(binding) {
            cskvQuizContent.release()
            csctqovOptionList.forEach {
                it.release()
            }
        }
    }
}
