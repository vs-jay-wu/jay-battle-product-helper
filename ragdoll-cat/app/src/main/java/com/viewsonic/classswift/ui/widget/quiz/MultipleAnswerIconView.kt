package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.databinding.ViewMultipleAnswerIconBinding
import com.viewsonic.classswift.utils.QuizUtils
import timber.log.Timber

class MultipleAnswerIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: ViewMultipleAnswerIconBinding =
        ViewMultipleAnswerIconBinding.inflate(LayoutInflater.from(context), this)

    private var answerOptions = mutableListOf<Int>()
    private var isNumberOrAlphabet: QuizOptionType = QuizOptionType.NUMBER

    fun setAnswerOptions(answerOptions: List<Int>, isNumberOrAlphabet: QuizOptionType) {
        this.answerOptions.clear()
        this.answerOptions.addAll(answerOptions)
        this.isNumberOrAlphabet = isNumberOrAlphabet
        Timber.d("[setAnswerOptions] - answerOptions: ${answerOptions}, isNumberOrAlphabet: $isNumberOrAlphabet")

        // 判斷答案數量
        if (answerOptions.size <= ROW_LIMIT_SIZE) {
            binding.llRowFirst.visibility = View.VISIBLE
            binding.llRowSecond.visibility = View.GONE
            processFirstRow(false)
        } else {
            binding.llRowFirst.visibility = View.VISIBLE
            binding.llRowSecond.visibility = View.VISIBLE
            processFirstRow(true)
            processSecondRow()
        }
    }

    private fun processFirstRow(hasTwoRow: Boolean) {
        val optionSize = if (hasTwoRow) OPTION_NUMBER_THREE else answerOptions.size
        when (optionSize) {
            OPTION_NUMBER_ONE -> {
                binding.ivAnswerIconPlaceholder1.visibility = View.VISIBLE
                binding.ivAnswerIconPlaceholder2.visibility = View.GONE
                binding.ivAnswerIconPlaceholder3.visibility = View.GONE

                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[0])?.let { resID ->
                    binding.ivAnswerIconPlaceholder1.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
            }
            OPTION_NUMBER_TWO -> {
                binding.ivAnswerIconPlaceholder1.visibility = View.VISIBLE
                binding.ivAnswerIconPlaceholder2.visibility = View.VISIBLE
                binding.ivAnswerIconPlaceholder3.visibility = View.GONE

                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[0])?.let { resID ->
                    binding.ivAnswerIconPlaceholder1.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[1])?.let { resID ->
                    binding.ivAnswerIconPlaceholder2.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
            }
            OPTION_NUMBER_THREE -> {
                binding.ivAnswerIconPlaceholder1.visibility = View.VISIBLE
                binding.ivAnswerIconPlaceholder2.visibility = View.VISIBLE
                binding.ivAnswerIconPlaceholder3.visibility = View.VISIBLE

                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[0])?.let { resID ->
                    binding.ivAnswerIconPlaceholder1.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[1])?.let { resID ->
                    binding.ivAnswerIconPlaceholder2.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[2])?.let { resID ->
                    binding.ivAnswerIconPlaceholder3.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
            }
        }
    }

    private fun processSecondRow() {
        when (answerOptions.size) {
            OPTION_NUMBER_FOUR -> {
                binding.ivAnswerIconPlaceholder4.visibility = View.VISIBLE
                binding.ivAnswerIconPlaceholder5.visibility = View.GONE
                binding.ivAnswerIconPlaceholder6.visibility = View.GONE

                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[3])?.let { resID ->
                    binding.ivAnswerIconPlaceholder4.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
            }
            OPTION_NUMBER_FIVE -> {
                binding.ivAnswerIconPlaceholder4.visibility = View.VISIBLE
                binding.ivAnswerIconPlaceholder5.visibility = View.VISIBLE
                binding.ivAnswerIconPlaceholder6.visibility = View.GONE

                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[3])?.let { resID ->
                    binding.ivAnswerIconPlaceholder4.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[4])?.let { resID ->
                    binding.ivAnswerIconPlaceholder5.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
            }
            OPTION_NUMBER_SIX -> {
                binding.ivAnswerIconPlaceholder4.visibility = View.VISIBLE
                binding.ivAnswerIconPlaceholder5.visibility = View.VISIBLE
                binding.ivAnswerIconPlaceholder6.visibility = View.VISIBLE

                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[3])?.let { resID ->
                    binding.ivAnswerIconPlaceholder4.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[4])?.let { resID ->
                    binding.ivAnswerIconPlaceholder5.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, answerOptions[5])?.let { resID ->
                    binding.ivAnswerIconPlaceholder6.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
                }
            }
        }
    }

    private companion object {
        const val ROW_LIMIT_SIZE: Int = 3

        const val OPTION_NUMBER_ONE: Int = 1
        const val OPTION_NUMBER_TWO: Int = 2
        const val OPTION_NUMBER_THREE: Int = 3
        const val OPTION_NUMBER_FOUR: Int = 4
        const val OPTION_NUMBER_FIVE: Int = 5
        const val OPTION_NUMBER_SIX: Int = 6
    }
}