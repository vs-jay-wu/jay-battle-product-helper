package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.databinding.ViewOptionButtonGroupBinding
import com.viewsonic.classswift.ui.widget.quiz.OptionButton.OnOptionButtonListener
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionState
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType
import timber.log.Timber


class OptionButtonGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: ViewOptionButtonGroupBinding =
        ViewOptionButtonGroupBinding.inflate(LayoutInflater.from(context), this)

    private val optionButtonList = mutableListOf<OptionButton>()
    var optionNumber: Int = DEFAULT_OPTION_NUMBER
        private set

    private val onOptionButtonListener = object : OnOptionButtonListener {
        override fun getOptionNumber(): Int {
            return optionNumber
        }

        override fun minusOneForOptionNumber(): Boolean {
            Timber.d("optionNumber: $optionNumber")
            if (optionNumber > MIN_OPTION_NUMBER) {
                optionNumber -= 1
                deleteOptionButton(optionNumber)
                return true
            }
            return false
        }

        override fun addOneForOptionNumber(): Boolean {
            Timber.d("optionNumber: $optionNumber")
            if (optionNumber < MAX_OPTION_NUMBER) {
                optionNumber += 1
                addOptionButton(optionNumber)
                return true
            }
            return false
        }

        override fun resetDeleteStatus() {
            resetDelete()
        }
    }

    init {
        binding.apply {
            // 預設四個選項：「1/A, 2/B, 3/C, 4/D, +」
            // option 1
            buttonOption1.setOnOptionButtonListener(onOptionButtonListener)
            optionButtonList.add(buttonOption1)
            // option 2
            buttonOption2.setOnOptionButtonListener(onOptionButtonListener)
            optionButtonList.add(buttonOption2)
            // option 3
            buttonOption3.setOnOptionButtonListener(onOptionButtonListener)
            optionButtonList.add(buttonOption3)
            // option 4
            buttonOption4.setOnOptionButtonListener(onOptionButtonListener)
            optionButtonList.add(buttonOption4)
            // option 5
            buttonOption5.setOnOptionButtonListener(onOptionButtonListener)
            buttonOption5.updateOptionState(OptionState.ADD)
            optionButtonList.add(buttonOption5)
            // option 6
            buttonOption6.setOnOptionButtonListener(onOptionButtonListener)
            buttonOption5.updateOptionState(OptionState.DELETED)
            optionButtonList.add(buttonOption6)
        }
    }

    private fun deleteOptionButton(optionNumber: Int) {
        for (index in 0..optionButtonList.size - 1) {
            if (index + 1 <= optionNumber) {
                optionButtonList[index].updateOptionState(OptionState.OPTION)
            }
            if (index + 1 > optionNumber) {
                if ((index + 1) - optionNumber == 1) {
                    optionButtonList[index].updateOptionState(OptionState.ADD)
                } else {
                    optionButtonList[index].updateOptionState(OptionState.DELETED)
                }
            }
        }
    }

    private fun addOptionButton(optionNumber: Int) {
        for (index in 0..optionButtonList.size - 1) {
            if (index + 1 == optionNumber) {
                optionButtonList[index].updateOptionState(OptionState.OPTION)
            }
            if (index + 1 > optionNumber) {
                if ((index + 1) - optionNumber == 1) {
                    optionButtonList[index].updateOptionState(OptionState.ADD)
                } else {
                    optionButtonList[index].updateOptionState(OptionState.DELETED)
                }
            }
        }
    }

    fun changeOptionValueType(optionValueType: OptionValueType) {
        resetDelete()
        binding.apply {
            buttonOption1.updateOptionValueType(optionValueType)
            buttonOption2.updateOptionValueType(optionValueType)
            buttonOption3.updateOptionValueType(optionValueType)
            buttonOption4.updateOptionValueType(optionValueType)
            buttonOption5.updateOptionValueType(optionValueType)
            buttonOption6.updateOptionValueType(optionValueType)
        }
    }

    fun setButtonOptions(count: Int) {
        this.optionNumber = count
        for (index in 0..<MAX_OPTION_NUMBER) {
            if (index < count) {
                optionButtonList[index].updateOptionState(OptionState.OPTION)
            } else if (index == count) {
                optionButtonList[index].updateOptionState(OptionState.ADD)
            } else {
                optionButtonList[index].updateOptionState(OptionState.DELETED)
            }

        }
    }


    fun resetDelete() {
        optionButtonList.firstOrNull { it.optionState == OptionState.DELETE || it.optionState == OptionState.DELETE_PRESSED }?.apply {
            this.resetDeleteStatus()
        }
    }


    private companion object {
        const val DEFAULT_OPTION_NUMBER = 4
        const val MIN_OPTION_NUMBER = 2
        const val MAX_OPTION_NUMBER = 6
    }

}