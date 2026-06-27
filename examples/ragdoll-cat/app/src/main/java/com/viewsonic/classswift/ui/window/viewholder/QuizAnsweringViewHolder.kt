package com.viewsonic.classswift.ui.window.viewholder

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.databinding.ItemQuizAnsweringBinding
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.utils.QuizUtils
import timber.log.Timber

class QuizAnsweringViewHolder(
    private val binding: ItemQuizAnsweringBinding,
    private val viewHolderClickCallBack: (QuizAnsweringInfo) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private val context = binding.root.context

    fun onBind(info: QuizAnsweringInfo) {
        with(binding) {
            mcvRoot.setOnClickListener {
                Timber.d("QuizAnsweringViewHolder is clicked.")
                if (info.answeringState == AnsweringState.ANSWERED) {
                    viewHolderClickCallBack(info)
                }
            }
            val brandBlueColor = ContextCompat.getColor(context, R.color.brand_blue)
            val whiteColor = ContextCompat.getColor(context, R.color.white)
            val absentColor = ContextCompat.getColor(context, R.color.quiz_absent_text_color)
            val absentStrokeLineColor = ContextCompat.getColor(context, R.color.quiz_absent_text_color)
            when (info.answeringState) {
                AnsweringState.ANSWERED -> {
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(brandBlueColor))
                    viewDivider.setBackgroundColor(brandBlueColor)
                    tvNumberAndName.setTextColor(whiteColor)
                    tvNumberAndName.setBackgroundColor(brandBlueColor)
                    tvNotAnswer.isVisible = false
                    tvAbsent.isVisible = false
                    tvAnswered.isVisible = !info.canShowAnswer
                    ivAnswerIcon.isVisible = info.canShowAnswer
                    if (info.canShowAnswer) {
                        QuizUtils.getAnsweringIcon(info)?.let { resID ->
                            ivAnswerIcon.background = ResourcesCompat.getDrawable(root.resources, resID, null)
                        }
                    }
                    viewBottomBg.setBackgroundColor(ContextCompat.getColor(context, R.color.quiz_answered_second_color))
                }

                AnsweringState.NOT_ANSWER -> {
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(brandBlueColor))
                    viewDivider.setBackgroundColor(brandBlueColor)
                    tvNumberAndName.setTextColor(ContextCompat.getColor(context, R.color.cs_main_black_text_color))
                    tvNumberAndName.setBackgroundColor(whiteColor)
                    tvNotAnswer.isVisible = true
                    tvAbsent.isVisible = false
                    ivAnswerIcon.isVisible = false
                    tvAnswered.isVisible = false
                    viewBottomBg.setBackgroundColor(whiteColor)
                }

                AnsweringState.ABSENT -> {
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(absentStrokeLineColor))
                    viewDivider.setBackgroundColor(absentColor)
                    tvNumberAndName.setTextColor(absentColor)
                    tvNumberAndName.setBackgroundColor(whiteColor)
                    ivAnswerIcon.isVisible = false
                    tvNotAnswer.isVisible = false
                    tvAnswered.isVisible = false
                    tvAbsent.isVisible = true
                    viewBottomBg.setBackgroundColor(whiteColor)
                }
            }
            val title = "${info.displaySeatNumber}  ${info.displayName}"
            tvNumberAndName.text = title
        }
    }
}
