package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.state.SelectionOptionType
import com.viewsonic.classswift.databinding.ViewSingleMultipleAnswerSwitchBinding

class SingleMultipleAnswerSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var isMultipleAnswer: Boolean = false
    private var binding: ViewSingleMultipleAnswerSwitchBinding =
        ViewSingleMultipleAnswerSwitchBinding.inflate(LayoutInflater.from(context), this)

    var onChangeSingleMultipleAnswerStatusListener: OnChangeSingleMultipleAnswerStatusListener? = null

    init {
        binding.apply {
            switchAnswer.setOnClickListener {
                if (switchAnswer.isChecked) {
                    setStatus(SelectionOptionType.SINGLE)
                } else {
                    setStatus(SelectionOptionType.MULTIPLE)
                }
            }
        }
    }

    fun setStatus(quizType: SelectionOptionType) {
        when (quizType) {
            SelectionOptionType.SINGLE -> {
                binding.switchAnswer.thumbDrawable = ResourcesCompat.getDrawable(resources, R.drawable.switch_thumb_blue, null)
                binding.tvSingleAnswer.setTextColor(ContextCompat.getColor(context, R.color.multiple_choice_options_text_blue))
                binding.tvMultipleAnswers.setTextColor(ContextCompat.getColor(context, R.color.multiple_choice_options_text_gray))
                isMultipleAnswer = false
                binding.switchAnswer.isChecked = true
            }
            SelectionOptionType.MULTIPLE -> {
                binding.switchAnswer.thumbDrawable = ResourcesCompat.getDrawable(resources, R.drawable.switch_thumb_gray, null)
                binding.tvSingleAnswer.setTextColor(ContextCompat.getColor(context, R.color.multiple_choice_options_text_gray))
                binding.tvMultipleAnswers.setTextColor(ContextCompat.getColor(context, R.color.multiple_choice_options_text_blue))
                isMultipleAnswer = true
                binding.switchAnswer.isChecked = false
            }
            else -> {}
        }
        onChangeSingleMultipleAnswerStatusListener?.changeStatus(isMultipleAnswer)
    }

    fun setSingleDisplayString(displayString: String) {
        binding.tvSingleAnswer.text = displayString
    }

    fun setMultipleDisplayString(displayString: String) {
        binding.tvMultipleAnswers.text = displayString
    }
    
    interface OnChangeSingleMultipleAnswerStatusListener {
        fun changeStatus(isMultipleAnswer: Boolean)
    }
}