package com.viewsonic.classswift.ui.widget.quizcollection

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.databinding.ViewCsTextQuizOptionsPanelBinding
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState


class CSTextQuizOptionsPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), CSTextQuizOption.Listener, CSTextQuizAnswerButton.Listener {
    private var binding: ViewCsTextQuizOptionsPanelBinding =
        ViewCsTextQuizOptionsPanelBinding.inflate(LayoutInflater.from(context), this)
    private val csTextQuizOptionList: MutableList<CSTextQuizOption> = mutableListOf()
    private var quizState: QuizState = QuizState.QUIZZING
    private var quizType: QuizType = QuizType.UNSPECIFIED
    private var optionValueType: OptionValueType = OptionValueType.NUMBER
    private val textQuizOptionList: MutableList<QuizOption> = mutableListOf()
    private val optionAndViewMap: MutableMap<QuizOption, CSTextQuizOption> = mutableMapOf()
    private var isMultipleMode: Boolean = false

    private var selectedOptionIdSet: MutableSet<Int> = mutableSetOf()
    private var presetAnswerOptionIdSet: MutableSet<Int> = mutableSetOf()

    init {
        with(binding) {
            csTextQuizOptionList.add(cstqoOption1)
            csTextQuizOptionList.add(cstqoOption2)
            csTextQuizOptionList.add(cstqoOption3)
            csTextQuizOptionList.add(cstqoOption4)
            csTextQuizOptionList.add(cstqoOption5)
            csTextQuizOptionList.add(cstqoOption6)
        }
        binding.cstqabViewAnswerButton.setListener(this@CSTextQuizOptionsPanel)
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
        val optionIndexMap = textQuizOptionList.associateBy { it.optionId - 1 }
        binding.cstqabViewAnswerButton.setIsAiAnswer(optionList.any {it.isAiAnswer})
        csTextQuizOptionList.forEachIndexed { index, view ->
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
                view.setListener(this@CSTextQuizOptionsPanel)
            }
            view.isVisible = option != null
        }
    }

    fun updateAnswerOptionIds(idList: List<Int>) {
        presetAnswerOptionIdSet.clear()
        presetAnswerOptionIdSet.addAll(idList)
    }

    fun updateQuizState(quizState: QuizState) {
        this.quizState = quizState
        with(binding) {
            llDiscloseArea.isVisible = quizState == QuizState.DISCLOSE_ANSWER
            optionAndViewMap.forEach { (option, view) ->
                view.updateQuizState(quizState)
            }
            when (quizState) {
                QuizState.QUIZZING -> {
                    llDiscloseArea.isVisible = false
                }
                QuizState.DISCLOSE_ANSWER -> {
                    llDiscloseArea.isVisible = true
                }
                QuizState.QUIZ_RESULTS -> {
                    llDiscloseArea.isVisible = false
                    val selectedOptionIdList = getSelectedOptionIdList()
                    csTextQuizOptionList.forEach { csTextQuizOption ->
                        if (selectedOptionIdList.contains(csTextQuizOption.getOptionId())) {
                            csTextQuizOption.selectAsAnswer()
                        } else {
                            csTextQuizOption.unselectAsAnswer()
                        }
                    }
                }
            }
        }
    }

    //region CSTextQuizOption.Listener
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
    }
    //endregion

    //region CSTextQuizOption.Listener
    override fun onButtonClicked(isOpened: Boolean) {
        optionAndViewMap.forEach { (option, view) ->
            if (isOpened) {
                view.revealAnswer()
            } else {
                view.hideAnswer()
            }
        }
    }
    //endregion

    fun release() {
        csTextQuizOptionList.forEach { it.release() }
    }
}
