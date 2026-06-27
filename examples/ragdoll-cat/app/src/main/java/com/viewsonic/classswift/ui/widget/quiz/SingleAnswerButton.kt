package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewSingleAnswerButtonBinding
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerButtonState
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType
import timber.log.Timber

class SingleAnswerButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onAnswerButtonListener: OnAnswerButtonListener? = null

    var answerButtonState: AnswerButtonState = AnswerButtonState.NOT_CHOSEN
    var optionValueType: OptionValueType = OptionValueType.NUMBER
    private var optionIndex: Int = 0
    private var isMultipleOption = false

    private var binding: ViewSingleAnswerButtonBinding =
        ViewSingleAnswerButtonBinding.inflate(LayoutInflater.from(context), this)

    init {
        // set Attr variable
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SingleAnswerButton,
            0,
            0
        ).let {
            val choiceValueIndex = it.getInt(R.styleable.SingleAnswerButton_optionIndex, 0)
            this.answerButtonState = AnswerButtonState.from(it.getInt(R.styleable.SingleAnswerButton_answerButtonState, 1))
            Timber.d("choiceValueIndex: $choiceValueIndex")
            optionIndex = choiceValueIndex
            background = ContextCompat.getDrawable(context, R.drawable.bg_white_radius400)
            foreground = ContextCompat.getDrawable(context, R.drawable.bg_transparent_radius400_line_white_border400)
            when (answerButtonState) {
                AnswerButtonState.CHOSEN -> setChosenState()
                AnswerButtonState.NOT_CHOSEN -> setNotChosenState()
                AnswerButtonState.DISABLED -> setDisableState()
            }
            it.recycle()
        }
    }

    fun setOnAnswerButtonListener(onAnswerButtonListener: OnAnswerButtonListener) {
        this.onAnswerButtonListener = onAnswerButtonListener
    }

    fun setSingleOrMultipleState(isMultipleOption: Boolean) {
        this.isMultipleOption = isMultipleOption
    }

    fun setChosenState() {
        answerButtonState = AnswerButtonState.CHOSEN
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.color_0A8CF0))
        foregroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.color_0A8CF0))
        binding.tvOption.setTextColor(ContextCompat.getColor(binding.root.context, R.color.neutral_0))
        updateOptionValueType(optionValueType)
    }

    fun setNotChosenState() {
        answerButtonState = AnswerButtonState.NOT_CHOSEN
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.neutral_0))
        foregroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.color_0A8CF0))
        binding.tvOption.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_0A8CF0))
        updateOptionValueType(optionValueType)
    }

    fun setDisableState() {
        answerButtonState = AnswerButtonState.DISABLED
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.neutral_0))
        foregroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.neutral_450))
        binding.tvOption.setTextColor(ContextCompat.getColor(binding.root.context, R.color.neutral_450))
        updateOptionValueType(optionValueType)
    }

    private val clickEventListener = object : OnClickListener {
        override fun onClick(v: View?) {
            Timber.d("[clickEventListener] - isMultipleOption: $isMultipleOption")
            when (isMultipleOption) {
                true -> {
                    if (answerButtonState == AnswerButtonState.NOT_CHOSEN) {
                        setChosenState()
                        onAnswerButtonListener?.enableDiscloseAnswerButton()
                    } else {
                        setNotChosenState()
                        onAnswerButtonListener?.disableDiscloseAnswerButton()
                    }
                }
                false -> {
                    if (answerButtonState == AnswerButtonState.NOT_CHOSEN) {
                        setChosenState()
                        onAnswerButtonListener?.setOtherButtonToNotChosen(optionIndex)
                        onAnswerButtonListener?.enableDiscloseAnswerButton()
                    }
                }
            }
        }
    }

    init {
        binding.apply {
            setOnClickListener(clickEventListener)
        }
    }


    /**
     * option button 的英文、數字轉換
     */
    fun updateOptionValueType(type: OptionValueType) {
        optionValueType = type
        when (optionValueType) {
            OptionValueType.NUMBER -> {
                binding.tvOption.text = optionIndex.toString()
            }
            OptionValueType.ALPHABET -> {
                val letter = 'A' + (optionIndex - 1)
                binding.tvOption.text = letter.toString()
            }
        }
    }

    interface OnAnswerButtonListener {
        fun setOtherButtonToNotChosen(chosenIndex: Int) {}
        fun enableDiscloseAnswerButton() {}
        fun disableDiscloseAnswerButton() {}
    }
}

