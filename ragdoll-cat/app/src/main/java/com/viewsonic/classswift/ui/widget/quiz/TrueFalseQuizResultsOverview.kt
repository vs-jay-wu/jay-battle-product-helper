package com.viewsonic.classswift.ui.widget.quiz

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewTrueFalseQuizResultsOverviewBinding
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionLanguageType
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel.Companion.TRUE_OPTION_INDEX
import com.viewsonic.classswift.utils.LanguageUtils

class TrueFalseQuizResultsOverview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: ViewTrueFalseQuizResultsOverviewBinding =
        ViewTrueFalseQuizResultsOverviewBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.apply {
            setCorrectAnswerDrawable(R.drawable.ic_true)
            setCorrectAnswerCount(0)
            setIncorrectAnswerCount(0)
            setNoAnswerCount(0)
        }
    }

    fun setCorrectAnswer(correctOptionId: Int) {
        val optionLanguageType = LanguageUtils.getTfOptionLanguageType()
        when(optionLanguageType) {
            OptionLanguageType.ENGLISH -> {
                if (correctOptionId == TRUE_OPTION_INDEX) {
                    setCorrectAnswerDrawable(R.drawable.ic_true)
                } else {
                    setCorrectAnswerDrawable(R.drawable.ic_false)
                }
            }
            OptionLanguageType.CHINESE -> {
                if (correctOptionId == TRUE_OPTION_INDEX) {
                    setCorrectAnswerDrawable(R.drawable.ic_true_tw)
                } else {
                    setCorrectAnswerDrawable(R.drawable.ic_false_tw)
                }
            }
        }
    }

    private fun setCorrectAnswerDrawable(@DrawableRes drawable: Int) {
        binding.ivCorrectAnswer.setImageDrawable(ResourcesCompat.getDrawable(context.resources, drawable, null))
    }

    @SuppressLint("SetTextI18n")
    fun setCorrectAnswerCount(count: Int) {
        binding.tvCorrectNumber.text = count.toString()
    }

    @SuppressLint("SetTextI18n")
    fun setIncorrectAnswerCount(count: Int) {
        binding.tvIncorrectNumber.text = count.toString()
    }

    @SuppressLint("SetTextI18n")
    fun setNoAnswerCount(count: Int) {
        binding.tvNoAnswerNumber.text = count.toString()
    }
}