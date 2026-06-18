package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.databinding.ViewMvbTextQuizDiscloseOptionsPanelBinding
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType


class MvbTextQuizDiscloseOptionsPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), MvbTextQuizDiscloseOption.Listener{
    private var binding: ViewMvbTextQuizDiscloseOptionsPanelBinding =
        ViewMvbTextQuizDiscloseOptionsPanelBinding.inflate(LayoutInflater.from(context), this)
    private val mvbTextQuizDiscloseOptionList: MutableList<MvbTextQuizDiscloseOption> = mutableListOf()
    private var quizType: QuizType = QuizType.UNSPECIFIED
    private var optionValueType: OptionValueType = OptionValueType.NUMBER
    private val textQuizOptionList: MutableList<QuizOption> = mutableListOf()
    private val optionAndViewMap: MutableMap<QuizOption, MvbTextQuizDiscloseOption> = mutableMapOf()
    private var isMultipleMode: Boolean = false
    private var selectedOptionIdSet: MutableSet<Int> = mutableSetOf()
    private var presetAnswerOptionIdSet: MutableSet<Int> = mutableSetOf()
    var onSelectionChanged: ((Set<Int>) -> Unit)? = null

    init {
        with(binding) {
            mvbTextQuizDiscloseOptionList.add(mvbDiscloseOption1)
            mvbTextQuizDiscloseOptionList.add(mvbDiscloseOption2)
        }
        binding.buttonSuggestedAnswer.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onEnableClicked() {
                optionAndViewMap.forEach { (option, view) ->
                    view.revealAnswer()
                }
            }

            override fun onClickedWithState(state: LoadingButtonState) {
                if (state == LoadingButtonState.ENABLE) {
                    binding.buttonSuggestedAnswer.setEnableText(context.getString(R.string.mvb_text_true_false_applied))
                    binding.buttonSuggestedAnswer.setDisable()
                }
            }
        })
    }

    fun getSelectedOptionIdList(): List<Int> {
        return if (selectedOptionIdSet.isEmpty()) {
            presetAnswerOptionIdSet.toList()
        } else {
            selectedOptionIdSet.toList()
        }
    }

    fun setQuizInfo(quizType: QuizType, optionValueType: OptionValueType, isMultipleMode: Boolean) {
        this.quizType = quizType
        this.optionValueType = optionValueType
        this.isMultipleMode = isMultipleMode
    }

    fun setQuizOptionList(optionList: List<QuizOption>) {
        textQuizOptionList.clear()
        textQuizOptionList.addAll(optionList)
        optionAndViewMap.clear()
        selectedOptionIdSet.clear()
        presetAnswerOptionIdSet.clear()
        val optionIndexMap = textQuizOptionList.associateBy { it.optionId - 1 }
        mvbTextQuizDiscloseOptionList.forEachIndexed { index, view ->
            val option = optionIndexMap[index]
            option?.let {
                if (option.isAnswer || option.isAiAnswer) {
                    presetAnswerOptionIdSet.add(option.optionId)
                }
                optionAndViewMap[it] = view
                val revisedContent = if (quizType == QuizType.SINGLE_SELECT || quizType == QuizType.MULTIPLE_SELECT) {
                    val prefix = when (optionValueType) {
                        OptionValueType.NUMBER   -> (index + 1).toString()
                        OptionValueType.ALPHABET -> ('A' + index).toString()
                    }
                    "($prefix) ${option.content}"
                } else {
                    option.content
                }
                view.updateOptionInfo(
                    option.optionId,
                    revisedContent,
                    option.reason ?: "",
                    option.isAnswer || option.isAiAnswer
                )
                view.setListener(this@MvbTextQuizDiscloseOptionsPanel)
            }
            view.isVisible = option != null
        }
        notifySelectionChanged()
    }

    fun hasSelectedOption(): Boolean = selectedOptionIdSet.isNotEmpty()

    private fun notifySelectionChanged() {
        onSelectionChanged?.invoke(selectedOptionIdSet.toSet())
    }

    //region MvbTextQuizDiscloseOption.Listener
    override fun onOptionSetAsAnswer(optionId: Int) {
        optionAndViewMap.forEach { (option, view) ->
            if (option.optionId == optionId) {
                view.selectAsAnswer()
                selectedOptionIdSet.add(option.optionId)
            } else {
                if (!isMultipleMode) {
                    view.unselectAsAnswer()
                    selectedOptionIdSet.remove(option.optionId)
                }
            }
        }
        notifySelectionChanged()
    }
    //endregion

    fun release() {
        mvbTextQuizDiscloseOptionList.forEach { it.release() }
    }
}
