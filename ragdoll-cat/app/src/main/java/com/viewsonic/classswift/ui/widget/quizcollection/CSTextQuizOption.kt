package com.viewsonic.classswift.ui.widget.quizcollection

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewCsTextQuizOptionBinding
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.utils.extension.isLatexContent
import timber.log.Timber


class CSTextQuizOption @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewCsTextQuizOptionBinding =
        ViewCsTextQuizOptionBinding.inflate(LayoutInflater.from(context), this)

    private var optionId: Int = 0
    private var content: String = ""
    private var reason: String = ""
    private var isPresetAnswer: Boolean = false // pre-selected by user or AI
    private var isAnswer: Boolean = false // selected now by the user
    private var isAnswerRevealed: Boolean = false
    private var quizState: QuizState = QuizState.QUIZZING

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun updateOptionInfo(optionId: Int, content: String, reason: String, isPresetAnswer: Boolean) {
        this.optionId = optionId
        this.content = content
        this.reason = reason
        this.isPresetAnswer = isPresetAnswer
        updateContent(content)
        updateReason(reason)
    }

    fun updateQuizState(quizState: QuizState) {
        Timber.d("[B][updateQuizState] : quizState = $quizState")
        this.quizState = quizState
        when (quizState) {
            QuizState.QUIZZING -> {
                setOnClickListener {}
                with(binding) {
                    cskvContent.setOnClickListener {}
                    cskvReason.setOnClickListener {}
                    tvReason.isVisible = false
                    viewHorizontalDivider.isVisible = true
                    background = ColorDrawable(Color.TRANSPARENT)
                    updateContent(content)
                    updateReason(reason)
                }
            }
            QuizState.DISCLOSE_ANSWER -> {
                setOnClickListener { handleOnClickEvent() }
                with(binding) {
                    cskvContent.setOnClickListener { handleOnClickEvent() }
                    cskvReason.setOnClickListener { handleOnClickEvent() }
                }
                updateUiInDisclosingPhase()
            }
            QuizState.QUIZ_RESULTS -> {
                setOnClickListener {}
                with(binding) {
                    cskvContent.setOnClickListener {}
                    cskvReason.setOnClickListener {}
                    tvReason.isGone = true
                    viewHorizontalDivider.isVisible = true
                    background = ColorDrawable(Color.TRANSPARENT)
                    when (isAnswer) {
                        true -> {
                            updateContent(content, Typeface.NORMAL, context.getColor(R.color.green_600))
                        }
                        false -> {
                            updateContent(content, Typeface.NORMAL, context.getColor(R.color.neutral_900))
                        }
                    }
                }
            }
        }
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
        if (quizState != QuizState.DISCLOSE_ANSWER) return
        with(binding) {
            viewHorizontalDivider.isVisible = false
            when (isAnswer) {
                true -> {
                    background = ContextCompat.getDrawable(context, R.drawable.bg_blue_radius400)
                    updateContent(content, Typeface.BOLD, context.getColor(R.color.neutral_0))
                    updateReason(reason, isAnswerRevealed && reason.isNotEmpty(), context.getColor(R.color.neutral_0))
                }
                false -> {
                    when (isPresetAnswer && isAnswerRevealed) {
                        true -> {
                            background = ContextCompat.getDrawable(context, R.drawable.bg_sky100_radius400)
                            updateContent(content, Typeface.BOLD, context.getColor(R.color.color_0A8CF0))
                            updateReason(reason, isAnswerRevealed && reason.isNotEmpty(), context.getColor(R.color.neutral_750))
                        }
                        false -> {
                            background = ContextCompat.getDrawable(context, R.drawable.bg_neutral0_radius400)
                            updateContent(content, Typeface.NORMAL, context.getColor(R.color.neutral_900))
                            updateReason(reason, isAnswerRevealed && reason.isNotEmpty(), context.getColor(R.color.neutral_750))
                        }
                    }
                }
            }
        }
    }

    private fun updateContent(content: String = this.content, typeFace: Int = Typeface.NORMAL, @ColorInt textColor: Int = context.getColor(R.color.neutral_900)) {
        with(binding) {
            val isLatexContent = content.isLatexContent()
            tvContent.isVisible = !isLatexContent
            cskvContent.isVisible = isLatexContent
            when (isLatexContent) {
                true -> {
                    cskvContent.setTextWithStyleAndColor(content, typeFace, textColor)
                }
                false -> {
                    tvContent.setTypeface(null, typeFace)
                    tvContent.setTextColor(textColor)
                    tvContent.text = content
                }
            }
        }
    }

    private fun updateReason(reason: String = this.reason, isVisible: Boolean = false, @ColorInt textColor: Int = context.getColor(R.color.neutral_750)) {
        with(binding) {
            val isLatexContent = reason.isLatexContent()
            val wasLatexVisible = cskvReason.isVisible
            tvReason.isVisible = !isLatexContent && isVisible
            cskvReason.isVisible = isLatexContent && isVisible
            when (isLatexContent) {
                true -> {
                    cskvReason.setTextWithStyleAndColor(reason, Typeface.NORMAL, textColor)
                    if (isVisible && !wasLatexVisible) {
                        cskvReason.refreshContentHeight()
                    }
                }
                false -> {
                    tvReason.setTypeface(null, Typeface.NORMAL)
                    tvReason.setTextColor(textColor)
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
