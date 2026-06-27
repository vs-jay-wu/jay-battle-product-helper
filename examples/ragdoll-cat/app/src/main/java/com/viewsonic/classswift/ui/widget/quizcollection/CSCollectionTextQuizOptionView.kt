package com.viewsonic.classswift.ui.widget.quizcollection

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewCsCollectionTextQuizOptionBinding
import com.viewsonic.classswift.utils.extension.isLatexContent


class CSCollectionTextQuizOptionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewCsCollectionTextQuizOptionBinding =
        ViewCsCollectionTextQuizOptionBinding.inflate(LayoutInflater.from(context), this)

    private var optionId: Int = 0
    private var content: String = ""
    private var reason: String = ""
    private var isAnswer: Boolean = false

    fun updateOptionInfo(optionId: Int, content: String, reason: String, isAnswer: Boolean) {
        this.optionId = optionId
        this.content = content
        this.reason = reason
        this.isAnswer = isAnswer
        updateContent(content)
        updateReason(reason)
    }

    fun revealAnswer() {
        with(binding) {
            updateReason(reason, reason.isNotEmpty())
            when (isAnswer) {
                true -> {
                    ivCheck.isVisible = true
                    updateContent(content, Typeface.BOLD, context.getColor(R.color.green_500))
                }
                false ->  {
                    ivCheck.isInvisible = true
                    updateContent(content, Typeface.NORMAL, context.getColor(R.color.neutral_900))
                }
            }
        }
    }

    fun hideAnswer() {
        with(binding) {
            ivCheck.isVisible = false
            updateContent(content, Typeface.NORMAL, context.getColor(R.color.neutral_900))
            updateReason(reason, false)
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

    fun release() {
        binding.cskvContent.release()
        binding.cskvReason.release()
    }
}
