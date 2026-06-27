package com.viewsonic.classswift.ui.widget.quizcollection

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import coil.load
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.databinding.ViewCsCollectionQuizDetailBinding
import com.viewsonic.classswift.ui.widget.quiz.SingleAnswerButton
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType
import timber.log.Timber


class CSCollectionQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: ViewCsCollectionQuizDetailBinding =
        ViewCsCollectionQuizDetailBinding.inflate(LayoutInflater.from(context), this)

    private val cssabButtonList: List<SingleAnswerButton>

    init {
        background = ContextCompat.getDrawable(context, R.drawable.bg_neutral0_radius800_line_neutral_500_border200)
        cssabButtonList = listOf(
            binding.cssabOption1,
            binding.cssabOption2,
            binding.cssabOption3,
            binding.cssabOption4,
            binding.cssabOption5,
            binding.cssabOption6,
        )
    }

    fun updateView(quizInCollectionInfo: QuizInCollectionInfo) {
        with(binding) {
            val optionValueType: OptionValueType = when (QuizOptionType.safeValueOf(quizInCollectionInfo.quizData.optionType)) {
                QuizOptionType.ALPHABET -> OptionValueType.ALPHABET
                else -> OptionValueType.NUMBER
            }

            clPlaceholder.isVisible = true
            ivContent.load(quizInCollectionInfo.quizData.imgUrl) {
                allowHardware(false)
                listener(
                    onSuccess = { request, result ->
                        clPlaceholder.isVisible = false
                    },
                    onError = { request, result ->
                        Timber.e("[CSCollectionQuizDetailView] : onError -> ${result.throwable.message}")
                    },
                )
            }
            val quizType = QuizType.entries.find { it.name == quizInCollectionInfo.quizData.quizType } ?: QuizType.UNSPECIFIED
            when (quizType) {
                QuizType.TRUE_FALSE -> {
                    tvQuizTypeOption.visibility = GONE
                    tvQuizType.text = context.getString(R.string.quiz_types_true_false)
                    llChoiceOption.isVisible = false
                    llTrueFalseOption.isVisible = true
                }
                QuizType.SINGLE_SELECT -> {
                    tvQuizTypeOption.visibility = VISIBLE
                    tvQuizType.text = context.getString(R.string.quiz_types_multiple_choice)
                    tvQuizTypeOption.text = context.getString(R.string.quiz_types_multiple_choice_single_answer)
                    llChoiceOption.isVisible = true
                    llTrueFalseOption.isVisible = false
                    val optionSize = quizInCollectionInfo.quizData.optionList.size
                    cssabButtonList.forEachIndexed { index, singleAnswerButton ->
                        singleAnswerButton.visibility = when (index < optionSize) {
                            true -> VISIBLE
                            false -> GONE
                        }
                        singleAnswerButton.updateOptionValueType(optionValueType)
                    }
                }
                QuizType.MULTIPLE_SELECT -> {
                    tvQuizTypeOption.visibility = VISIBLE
                    tvQuizType.text = context.getString(R.string.quiz_types_multiple_choice)
                    tvQuizTypeOption.text = context.getString(R.string.quiz_types_multiple_choice_multiple_answers)
                    llChoiceOption.isVisible = true
                    llTrueFalseOption.isVisible = false
                    val optionSize = quizInCollectionInfo.quizData.optionList.size
                    cssabButtonList.forEachIndexed { index, singleAnswerButton ->
                        singleAnswerButton.visibility = when (index < optionSize) {
                            true -> VISIBLE
                            false -> GONE
                        }
                        singleAnswerButton.updateOptionValueType(optionValueType)
                    }
                }
                QuizType.RECORD -> {
                    tvQuizTypeOption.visibility = GONE
                    tvQuizType.text = context.getString(R.string.quiz_types_audio)
                    llChoiceOption.isVisible = false
                    llTrueFalseOption.isVisible = false
                }
                QuizType.SHORT_ANSWER -> {
                    tvQuizTypeOption.visibility = GONE
                    tvQuizType.text = context.getString(R.string.short_answer_capitalized_first_word)
                    llChoiceOption.isVisible = false
                    llTrueFalseOption.isVisible = false
                }
                QuizType.SINGLE_POLL -> {
                    tvQuizTypeOption.visibility = VISIBLE
                    tvQuizType.text = context.getString(R.string.quiz_types_poll)
                    tvQuizTypeOption.text = context.getString(R.string.quiz_types_poll_single_vote)
                    llChoiceOption.isVisible = true
                    llTrueFalseOption.isVisible = false
                    val optionSize = quizInCollectionInfo.quizData.optionList.size
                    cssabButtonList.forEachIndexed { index, singleAnswerButton ->
                        singleAnswerButton.visibility = when (index < optionSize) {
                            true -> VISIBLE
                            false -> GONE
                        }
                        singleAnswerButton.updateOptionValueType(optionValueType)
                    }
                }
                QuizType.MULTIPLE_POLL -> {
                    tvQuizTypeOption.visibility = VISIBLE
                    tvQuizType.text = context.getString(R.string.quiz_types_poll)
                    tvQuizTypeOption.text = context.getString(R.string.quiz_types_poll_multiple_votes)
                    llChoiceOption.isVisible = true
                    llTrueFalseOption.isVisible = false
                    val optionSize = quizInCollectionInfo.quizData.optionList.size
                    cssabButtonList.forEachIndexed { index, singleAnswerButton ->
                        singleAnswerButton.visibility = when (index < optionSize) {
                            true -> VISIBLE
                            false -> GONE
                        }
                        singleAnswerButton.updateOptionValueType(optionValueType)
                    }
                }
                else -> {}
            }
        }
    }
}

