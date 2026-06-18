package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.databinding.ViewSingleMultipleAnswerButtonGroupBinding
import com.viewsonic.classswift.ui.widget.quiz.SingleAnswerButton.OnAnswerButtonListener
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerButtonState
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType


class SingleMultipleAnswerButtonGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onChangeDiscloseAnswerButtonListener: OnChangeDiscloseAnswerButtonListener? = null
    private var binding: ViewSingleMultipleAnswerButtonGroupBinding =
        ViewSingleMultipleAnswerButtonGroupBinding.inflate(LayoutInflater.from(context), this)

    private val optionButtonList = mutableListOf<SingleAnswerButton>()
    private var optionNumber: Int = DEFAULT_OPTION_NUMBER
    var optionValueType: OptionValueType = OptionValueType.NUMBER

    fun setOnChangeDiscloseAnswerButtonListener(onChangeDiscloseAnswerButtonListener: OnChangeDiscloseAnswerButtonListener) {
        this.onChangeDiscloseAnswerButtonListener = onChangeDiscloseAnswerButtonListener
    }

    fun setSingleOrMultipleState(isMultipleOption: Boolean) {
        binding.apply {
            buttonOption1.setSingleOrMultipleState(isMultipleOption)
            buttonOption2.setSingleOrMultipleState(isMultipleOption)
            buttonOption3.setSingleOrMultipleState(isMultipleOption)
            buttonOption4.setSingleOrMultipleState(isMultipleOption)
            buttonOption5.setSingleOrMultipleState(isMultipleOption)
            buttonOption6.setSingleOrMultipleState(isMultipleOption)

            val opAnswerButtonListener = if (isMultipleOption) {
                object : OnAnswerButtonListener {
                    override fun enableDiscloseAnswerButton() {
                        if (getChosenOption().isNotEmpty()) {
                            onChangeDiscloseAnswerButtonListener?.enableDiscloseAnswerButton()
                        }
                    }
                    override fun disableDiscloseAnswerButton() {
                        if (getChosenOption().isEmpty()) {
                            onChangeDiscloseAnswerButtonListener?.disableDiscloseAnswerButton()
                        }
                    }
                }
            } else {
                object : OnAnswerButtonListener {
                    override fun setOtherButtonToNotChosen(chosenIndex: Int) {
                        for (index in 0..optionButtonList.size - 1) {
                            if (index + 1 != chosenIndex) {
                                optionButtonList[index].setNotChosenState()
                            }
                        }
                    }
                    override fun enableDiscloseAnswerButton() {
                        onChangeDiscloseAnswerButtonListener?.enableDiscloseAnswerButton()
                    }
                }
            }

            buttonOption1.setOnAnswerButtonListener(opAnswerButtonListener)
            buttonOption2.setOnAnswerButtonListener(opAnswerButtonListener)
            buttonOption3.setOnAnswerButtonListener(opAnswerButtonListener)
            buttonOption4.setOnAnswerButtonListener(opAnswerButtonListener)
            buttonOption5.setOnAnswerButtonListener(opAnswerButtonListener)
            buttonOption6.setOnAnswerButtonListener(opAnswerButtonListener)
        }
    }

    init {
        // 控制按鈕的顯示數量 and 123/abc
        binding.apply {
            // 預設四個選項：「1/A, 2/B, 3/C, 4/D」
            // option 1
            buttonOption1.optionValueType = optionValueType
            buttonOption1.visibility = VISIBLE
            optionButtonList.add(buttonOption1)
            // option 2
            buttonOption2.optionValueType = optionValueType
            buttonOption2.visibility = VISIBLE
            optionButtonList.add(buttonOption2)
            // option 3
            buttonOption3.optionValueType = optionValueType
            buttonOption3.visibility = VISIBLE
            optionButtonList.add(buttonOption3)
            // option 4
            buttonOption4.optionValueType = optionValueType
            buttonOption4.visibility = VISIBLE
            optionButtonList.add(buttonOption4)
            // option 5
            buttonOption5.optionValueType = optionValueType
            buttonOption5.visibility = INVISIBLE
            optionButtonList.add(buttonOption5)
            // option 6
            buttonOption6.optionValueType = optionValueType
            buttonOption6.visibility = INVISIBLE
            optionButtonList.add(buttonOption6)
        }
    }

    fun updateOptionNumber(number: Int) {
        this.optionNumber = number

        for (index in 0..optionButtonList.size - 1) {
            if (index + 1 <= number) {
                optionButtonList[index].visibility = VISIBLE
            } else {
                optionButtonList[index].visibility = INVISIBLE
            }
        }
    }

    fun changeOptionValueType(optionValueType: OptionValueType) {
        binding.apply {
            optionButtonList.forEach { it.updateOptionValueType(optionValueType) }
        }
    }

    fun getChosenOption(): List<Int> {
        val optionList = mutableListOf<Int>()
        for (i in 0..<optionButtonList.size) {
            if (optionButtonList[i].answerButtonState == AnswerButtonState.CHOSEN) {
                optionList.add(i + 1)
            }
        }
        return optionList
    }

    private companion object {
        const val DEFAULT_OPTION_NUMBER = 4
    }

    interface OnChangeDiscloseAnswerButtonListener {
        fun enableDiscloseAnswerButton() {}
        fun disableDiscloseAnswerButton() {}
    }
}

