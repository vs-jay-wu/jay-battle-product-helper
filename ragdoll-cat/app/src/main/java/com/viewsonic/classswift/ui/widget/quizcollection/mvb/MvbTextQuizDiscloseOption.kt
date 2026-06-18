package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewMvbTextQuizDiscloseOptionBinding
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.isLatexContent


class MvbTextQuizDiscloseOption @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewMvbTextQuizDiscloseOptionBinding =
        ViewMvbTextQuizDiscloseOptionBinding.inflate(LayoutInflater.from(context), this)

    private var optionId: Int = 0
    private var content: String = ""
    private var reason: String = ""
    private var isPresetAnswer: Boolean = false // pre-selected by user or AI
    private var isAnswer: Boolean = false // selected now by the user
    private var isAnswerRevealed: Boolean = false

    private var listener: Listener? = null

    init {
        minHeight = 41.32f.dpToPx().toInt()
        setOnClickListener { handleOnClickEvent() }
        with(binding) {
            llSuggestedAnswer.setOnClickListener { handleOnClickEvent() }
            mvbRadioButton.setOnClickListener { handleOnClickEvent() }
            cskvContent.setOnClickListener { handleOnClickEvent() }
            cskvReason.setOnClickListener { handleOnClickEvent() }
        }
        updateUiInDisclosingPhase()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun updateOptionInfo(optionId: Int, content: String, reason: String, isPresetAnswer: Boolean) {
        this.optionId = optionId
        this.content = content
        this.reason = reason
        this.isPresetAnswer = isPresetAnswer
        binding.llSuggestedAnswer.isVisible = isPresetAnswer
        updateContent(content)
        updateReason(reason)
        updateUiInDisclosingPhase()
    }

    fun selectAsAnswer() {
        isAnswer = true
        updateUiInDisclosingPhase()
    }

    fun unselectAsAnswer() {
        isAnswer = false
        updateUiInDisclosingPhase()
    }

    fun revealAnswer() {
        isAnswerRevealed = true
        updateUiInDisclosingPhase()
    }

    fun hideAnswer() {
        isAnswerRevealed = false
        updateUiInDisclosingPhase()
    }

    fun getOptionId() = optionId

    private fun handleOnClickEvent() {
        if (!isAnswer) {
            selectAsAnswer()
            listener?.onOptionSetAsAnswer(optionId)
        }
    }

    private fun updateUiInDisclosingPhase() {
        with(binding) {
            llSuggestedAnswer.isVisible = isPresetAnswer && isAnswerRevealed
            when (isAnswer) {
                true -> {
                    background = ResourcesCompat.getDrawable(resources, R.drawable.bg_violet50_radius400_line_violet500_border200, null)
                    mvbRadioButton.isChecked = true
                }
                false -> {
                    background = ResourcesCompat.getDrawable(resources, R.drawable.bg_neutral100_radius400_line_neutral300_border200, null)
                    mvbRadioButton.isChecked = false
                }
            }
            updateContent(content)
            updateReason(reason, isAnswerRevealed && reason.isNotEmpty())
        }
    }

    private fun updateContent(content: String = this.content) {
        with(binding) {
            val isLatexContent = content.isLatexContent()
            tvContent.isVisible = !isLatexContent
            cskvContent.isVisible = isLatexContent
            when (isLatexContent) {
                true -> {
                    cskvContent.setText(content)
                }
                false -> {
                    tvContent.text = content
                }
            }
        }
    }

    private fun updateReason(reason: String = this.reason, isVisible: Boolean = false) {
        with(binding) {
            val isLatexContent = reason.isLatexContent()
            val wasLatexVisible = cskvReason.isVisible
            tvReason.isVisible = !isLatexContent && isVisible
            cskvReason.isVisible = isLatexContent && isVisible
            when (isLatexContent) {
                true -> {
                    cskvReason.setText(reason)
                    if (isVisible && !wasLatexVisible) {
                        cskvReason.refreshContentHeight()
                    }
                }
                false -> {
                    tvReason.text = reason
                }
            }
        }
    }

    interface Listener {
        fun onOptionSetAsAnswer(optionId: Int)
    }

    fun release() {
        binding.cskvContent.release()
        binding.cskvReason.release()
    }
}
