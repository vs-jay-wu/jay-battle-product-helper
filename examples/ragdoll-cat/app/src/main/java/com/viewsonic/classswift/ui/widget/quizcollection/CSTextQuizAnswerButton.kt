package com.viewsonic.classswift.ui.widget.quizcollection

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewCsTextQuizAnswerButtonBinding
import com.viewsonic.classswift.utils.extension.dpToPx


class CSTextQuizAnswerButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewCsTextQuizAnswerButtonBinding =
        ViewCsTextQuizAnswerButtonBinding.inflate(LayoutInflater.from(context), this)

    private var isOpened: Boolean = false
    private var isAiAnswer: Boolean = false
    private var listener: Listener? = null

    init {
        setPadding(
            5.33f.dpToPx().toInt(),
            6f.dpToPx().toInt(),
            8f.dpToPx().toInt(),
            5.33f.dpToPx().toInt()
        )
        setOnClickListener {
            isOpened = !isOpened
            updateUi()
            listener?.onButtonClicked(isOpened)
        }
        updateUi()
        setIsAiAnswer(isAiAnswer)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setIsAiAnswer(isAiAnswer: Boolean) {
        this.isAiAnswer = isAiAnswer
        binding.tvTitle.text = when (isAiAnswer) {
            true -> context.getString(R.string.ai_answer)
            false -> context.getString(R.string.view_answer)
        }
    }

    private fun updateUi() {
        with(binding) {
            when (isOpened) {
                true -> {
                    background = ContextCompat.getDrawable(context, R.drawable.selector_text_quiz_answer_button_closed)
                    ivIcon.setImageResource(R.drawable.ic_eye_opened_without_border)
                    ivIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.selector_text_quiz_answer_button_opened_tint)
                    tvTitle.setTextColor(ContextCompat.getColorStateList(context, R.color.selector_text_quiz_answer_button_opened_tint))
                }
                false -> {
                    background = ContextCompat.getDrawable(context, R.drawable.selector_text_quiz_answer_button_closed)
                    ivIcon.setImageResource(R.drawable.ic_eye_closed_without_border)
                    ivIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.neutral_900)
                    tvTitle.setTextColor(context.getColor(R.color.neutral_900))
                }
            }
        }
    }

    interface Listener {
        fun onButtonClicked(isOpened: Boolean)
    }
}

