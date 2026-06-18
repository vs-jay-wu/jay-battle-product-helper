package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewTrueFalseAnswerButtonBinding
import com.viewsonic.classswift.ui.widget.quiz.SingleAnswerButton.OnAnswerButtonListener
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerButtonState
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionLanguageType
import com.viewsonic.classswift.utils.LanguageUtils
import timber.log.Timber

class TrueFalseAnswerButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onAnswerButtonListener: OnAnswerButtonListener? = null

    var answerButtonState: AnswerButtonState = AnswerButtonState.NOT_CHOSEN
    var optionLanguageType: OptionLanguageType = LanguageUtils.getTfOptionLanguageType()
    var optionIndex: Int = 0
    var optionContent: Boolean = true

    private var binding: ViewTrueFalseAnswerButtonBinding =
        ViewTrueFalseAnswerButtonBinding.inflate(LayoutInflater.from(context), this)

    init {
        // set Attr variable
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TrueFalseOptionButton,
            0,
            0
        ).let {
            val answerOptionContent = it.getBoolean(R.styleable.TrueFalseOptionButton_optionContent, true)
            this.answerButtonState = AnswerButtonState.from(it.getInt(R.styleable.TrueFalseOptionButton_answerButtonState, 1))
            Timber.d("optionContent: $optionContent")
            optionContent = answerOptionContent
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

    fun setChosenState() {
        answerButtonState = AnswerButtonState.CHOSEN
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.color_0A8CF0))
        foregroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.color_0A8CF0))
        binding.ivIcon.setImageDrawable(getIconImageDrawable())
        binding.ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.neutral_0))
    }

    fun setNotChosenState() {
        answerButtonState = AnswerButtonState.NOT_CHOSEN
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.neutral_0))
        foregroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.color_0A8CF0))
        binding.ivIcon.setImageDrawable(getIconImageDrawable())
        binding.ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.color_0A8CF0))
    }

    fun setDisableState() {
        answerButtonState = AnswerButtonState.DISABLED
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.neutral_0))
        foregroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.neutral_450))
        binding.ivIcon.setImageDrawable(getIconImageDrawable())
        binding.ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.neutral_450))
    }

    private fun getIconImageDrawable() = when (optionLanguageType) {
        OptionLanguageType.ENGLISH -> {
            if (optionContent) {
                ContextCompat.getDrawable(context, R.drawable.ic_answer_font_t)
            } else {
                ContextCompat.getDrawable(context, R.drawable.ic_answer_font_f)
            }
        }
        OptionLanguageType.CHINESE -> {
            if (optionContent) {
                ContextCompat.getDrawable(context, R.drawable.ic_answer_font_o)
            } else {
                ContextCompat.getDrawable(context, R.drawable.ic_answer_font_x)
            }
        }
    }

    private val clickEventListener = object : OnClickListener {
        override fun onClick(v: View?) {
            if (answerButtonState == AnswerButtonState.NOT_CHOSEN) {
                setChosenState()
                onAnswerButtonListener?.setOtherButtonToNotChosen(optionIndex)
                onAnswerButtonListener?.enableDiscloseAnswerButton()
            }
        }
    }

    init {
        binding.apply {
            root.setOnClickListener(clickEventListener)
        }
    }
}
