package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewTrueFalseAnswerButtonGroupBinding
import com.viewsonic.classswift.ui.widget.quiz.SingleAnswerButton.OnAnswerButtonListener
import com.viewsonic.classswift.ui.widget.quiz.SingleMultipleAnswerButtonGroup.OnChangeDiscloseAnswerButtonListener
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerButtonState
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionLanguageType
import com.viewsonic.classswift.utils.LanguageUtils

class TrueFalseAnswerButtonGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onChangeDiscloseAnswerButtonListener: OnChangeDiscloseAnswerButtonListener? = null
    private var binding: ViewTrueFalseAnswerButtonGroupBinding =
        ViewTrueFalseAnswerButtonGroupBinding.inflate(LayoutInflater.from(context), this)

    private val optionButtonList = mutableListOf<TrueFalseAnswerButton>()
    private var optionLanguageType: OptionLanguageType = LanguageUtils.getTfOptionLanguageType()
    private var chosenAnswer: Boolean = false

    private val opAnswerButtonListener = object : OnAnswerButtonListener {
        override fun setOtherButtonToNotChosen(chosenIndex: Int) {
            if (chosenIndex == 0) {
                optionButtonList[1].setNotChosenState()
            } else {
                optionButtonList[0].setNotChosenState()
            }
        }

        override fun enableDiscloseAnswerButton() {
            onChangeDiscloseAnswerButtonListener?.enableDiscloseAnswerButton()
        }
    }

    fun setOnChangeDiscloseAnswerButtonListener(onChangeDiscloseAnswerButtonListener: OnChangeDiscloseAnswerButtonListener) {
        this.onChangeDiscloseAnswerButtonListener = onChangeDiscloseAnswerButtonListener
    }

    fun getTrueFalseAnswer(): Int {
        chosenAnswer = (optionButtonList[0].answerButtonState == AnswerButtonState.CHOSEN)

        return if (chosenAnswer) {
            OPTION_INDEX_TRUE
        } else {
            OPTION_INDEX_FALSE
        }
    }

    init {
        binding.apply {
            buttonOptionTrue.optionIndex = 0
            buttonOptionTrue.optionContent = true
            buttonOptionTrue.setOnAnswerButtonListener(opAnswerButtonListener)
            optionButtonList.add(buttonOptionTrue)

            buttonOptionFalse.optionIndex = 1
            buttonOptionFalse.optionContent = false
            buttonOptionFalse.setOnAnswerButtonListener(opAnswerButtonListener)
            optionButtonList.add(buttonOptionFalse)

            when(optionLanguageType) {
                OptionLanguageType.ENGLISH -> {
                    buttonOptionTrue.setBackgroundDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_option_true, null))
                    buttonOptionFalse.setBackgroundDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_option_false, null))
                }
                OptionLanguageType.CHINESE -> {
                    buttonOptionTrue.setBackgroundDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_option_o, null))
                    buttonOptionFalse.setBackgroundDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_option_x, null))
                }
            }
        }
    }

    companion object {
        private const val OPTION_INDEX_TRUE: Int = 1
        private const val OPTION_INDEX_FALSE: Int = 2
    }
}