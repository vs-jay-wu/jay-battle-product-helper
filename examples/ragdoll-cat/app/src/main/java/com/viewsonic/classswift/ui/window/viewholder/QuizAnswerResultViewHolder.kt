package com.viewsonic.classswift.ui.window.viewholder

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.databinding.ItemQuizAnswerResultBinding
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.utils.QuizUtils

class QuizAnswerResultViewHolder(private val binding: ItemQuizAnswerResultBinding) : RecyclerView.ViewHolder(binding.root) {
    private val context = binding.root.context
    fun onBind(info: QuizAnswerResultInfo) {
        with(binding) {
            val correctColor = ContextCompat.getColor(context, R.color.quiz_correct_color)
            val incorrectColor = ContextCompat.getColor(context, R.color.quiz_incorrect_color)
            val noAnswerColor = ContextCompat.getColor(context, R.color.quiz_no_answer_color)
            val white = ContextCompat.getColor(context, R.color.neutral_0)
            val absentColor = ContextCompat.getColor(context, R.color.quiz_absent_text_color)
            when (info.answerResultState) {
                AnswerResultState.ANSWERED,
                AnswerResultState.CORRECT -> {
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(correctColor))
                    viewDivider.setBackgroundColor(correctColor)
                    tvNumberAndName.setTextColor(white)
                    tvNumberAndName.setBackgroundColor(correctColor)
                    tvNoAnswer.isVisible = false
                    tvAbsent.isVisible = false
                    ivAnswerIcon.isVisible = true
                    QuizUtils.getAnswerResultIcon(info)?.let { resID ->
                        ivAnswerIcon.background = ResourcesCompat.getDrawable(root.resources, resID, null)
                    }

                    viewBottomBg.background =
                        ResourcesCompat.getDrawable(binding.root.resources, R.color.quiz_correct_second_color, null)
                }

                AnswerResultState.INCORRECT -> {
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(incorrectColor))
                    viewDivider.setBackgroundColor(incorrectColor)
                    tvNumberAndName.setTextColor(white)
                    tvNumberAndName.setBackgroundColor(incorrectColor)
                    tvNoAnswer.isVisible = false
                    tvAbsent.isVisible = false
                    ivAnswerIcon.isVisible = true
                    QuizUtils.getAnswerResultIcon(info)?.let { resID ->
                        ivAnswerIcon.background = ResourcesCompat.getDrawable(root.resources, resID, null)
                    }
                    viewBottomBg.setBackgroundColor(ContextCompat.getColor(context, R.color.quiz_incorrect_second_color))
                }

                AnswerResultState.NO_ANSWER -> {
                    tvNumberAndName.setTextColor(ContextCompat.getColor(context, R.color.cs_main_black_text_color))
                    viewDivider.setBackgroundColor(noAnswerColor)
                    tvNumberAndName.setBackgroundColor(noAnswerColor)
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(noAnswerColor))
                    ivAnswerIcon.isVisible = false
                    tvAbsent.isVisible = false
                    tvNoAnswer.isVisible = true
                    viewBottomBg.setBackgroundColor(white)
                }

                AnswerResultState.ABSENT -> {
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(absentColor))
                    viewDivider.setBackgroundColor(absentColor)
                    tvNumberAndName.setTextColor(absentColor)
                    tvNumberAndName.setBackgroundColor(white)
                    ivAnswerIcon.isVisible = false
                    tvNoAnswer.isVisible = false
                    tvAbsent.isVisible = true
                    viewBottomBg.setBackgroundColor(white)
                }
            }
            val title = "${info.displaySeatNumber}  ${info.displayName}"
            tvNumberAndName.text = title
        }
    }
}
