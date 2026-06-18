package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.state.SelectionOptionType
import com.viewsonic.classswift.databinding.ViewOptionPanelBinding
import com.viewsonic.classswift.ui.widget.quiz.NumberAlphabetSwitch.OnChangeNumberAlphabetListener
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType


class OptionPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: ViewOptionPanelBinding =
        ViewOptionPanelBinding.inflate(LayoutInflater.from(context), this)

    private val onChangeNumberAlphabetListener = object : OnChangeNumberAlphabetListener {
        override fun changeToNumberOptions() {
            binding.csOptionButtonGroup.changeOptionValueType(OptionValueType.NUMBER)
        }
        override fun changeToAlphabetOptions() {
            binding.csOptionButtonGroup.changeOptionValueType(OptionValueType.ALPHABET)
        }
    }

    init {
        binding.csNumberAlphabetSwitch.setOnChangeNumberAlphabetListener(onChangeNumberAlphabetListener)
    }

    /**
     * 取得選項數量
     */
    fun getOptionNumber(): Int {
        return binding.csOptionButtonGroup.optionNumber
    }

    /**
     * 取得選項類型 123 or ABC
     */
    fun getOptionValueType(): OptionValueType {
        return binding.csNumberAlphabetSwitch.optionValueType
    }

    fun setChangeOptionValueType(optionValueType: OptionValueType){
        binding.csNumberAlphabetSwitch.setSwitchButton(optionValueType)
        binding.csOptionButtonGroup.changeOptionValueType(optionValueType)
    }

    fun setButtonOptions(count : Int){
        binding.csOptionButtonGroup.setButtonOptions(count)
    }

    fun setSingleMultipleAnswerSwitch(type: SelectionOptionType) {
        binding.csSingleMultipleAnswerSwitch.setStatus(type)
    }

    fun setOnChangeSingleMultipleAnswerStatusListener(listener: SingleMultipleAnswerSwitch.OnChangeSingleMultipleAnswerStatusListener) {
        binding.csSingleMultipleAnswerSwitch.onChangeSingleMultipleAnswerStatusListener = listener
    }

    fun setPollUi() {
        binding.apply {
            csSingleMultipleAnswerSwitch.setSingleDisplayString(context.getString(R.string.quiz_types_poll_single_vote))
            csSingleMultipleAnswerSwitch.setMultipleDisplayString(context.getString(R.string.quiz_types_poll_multiple_votes))
        }
    }
}