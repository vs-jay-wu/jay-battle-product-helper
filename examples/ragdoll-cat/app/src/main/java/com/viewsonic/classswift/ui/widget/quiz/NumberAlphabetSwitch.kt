package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewNumberAlphabetSwitchBinding
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType


class NumberAlphabetSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onChangeNumberAlphabetListener: OnChangeNumberAlphabetListener? = null
    private var binding: ViewNumberAlphabetSwitchBinding =
        ViewNumberAlphabetSwitchBinding.inflate(LayoutInflater.from(context), this)
    var optionValueType = OptionValueType.NUMBER
        private set

    init {
        binding.apply {
            buttonNumber.setOnClickListener {
                optionValueType = OptionValueType.NUMBER
                clickButtonNumber()
            }

            buttonAlphabet.setOnClickListener {
                optionValueType = OptionValueType.ALPHABET
                clickButtonAlphabet()
            }
        }
    }

    fun setOnChangeNumberAlphabetListener(onChangeNumberAlphabetListener: OnChangeNumberAlphabetListener) {
        this.onChangeNumberAlphabetListener = onChangeNumberAlphabetListener
    }

    private fun clickButtonNumber() {
        binding.apply {
            buttonNumber.setTextColor(ContextCompat.getColor(context, R.color.multiple_choice_options_text_blue))
            buttonNumber.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_cs_blue_round_button, null)

            buttonAlphabet.setTextColor(ContextCompat.getColor(context, R.color.multiple_choice_options_text_gray))
            buttonAlphabet.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_cs_transparent_round_button, null)
        }

        // 變更選項顯示內容 to number
        onChangeNumberAlphabetListener?.changeToNumberOptions()
    }

    private fun clickButtonAlphabet() {
        binding.apply {
            buttonAlphabet.setTextColor(ContextCompat.getColor(context, R.color.multiple_choice_options_text_blue))
            buttonAlphabet.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_cs_blue_round_button, null)

            buttonNumber.setTextColor(ContextCompat.getColor(context, R.color.multiple_choice_options_text_gray))
            buttonNumber.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_cs_transparent_round_button, null)
        }

        // 變更選項顯示內容 to alphabet
        onChangeNumberAlphabetListener?.changeToAlphabetOptions()
    }

    fun setSwitchButton(type: OptionValueType) {
        optionValueType = type
        when (type) {
            OptionValueType.NUMBER -> clickButtonNumber()
            OptionValueType.ALPHABET -> clickButtonAlphabet()
        }
    }

    interface OnChangeNumberAlphabetListener {
        fun changeToNumberOptions()
        fun changeToAlphabetOptions()
    }
}

