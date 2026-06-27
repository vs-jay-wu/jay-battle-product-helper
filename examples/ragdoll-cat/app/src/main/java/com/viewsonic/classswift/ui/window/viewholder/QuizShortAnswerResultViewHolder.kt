package com.viewsonic.classswift.ui.window.viewholder

import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.databinding.ItemQuizShortAnswerResultBinding
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.window.adapter.QuizAnswerResultAdapter

class QuizShortAnswerResultViewHolder(
    private val binding: ItemQuizShortAnswerResultBinding,
    private val onItemInteractionListener: QuizAnswerResultAdapter.OnItemInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    private val context = binding.root.context
    private val color78CB3D = ContextCompat.getColor(context, R.color.color_78CB3D)
    private val colorC3C7C7 = ContextCompat.getColor(context, R.color.color_C3C7C7)
    private val color2E3133 = ContextCompat.getColor(context, R.color.color_2E3133)
    private val colorF1FAEB = ContextCompat.getColor(context, R.color.color_F1FAEB)
    private val colorNeutral0 = ContextCompat.getColor(context, R.color.neutral_0)

    fun onBind(info: QuizAnswerResultInfo) {
        with(binding) {
            root.setOnClickListener {
                onItemInteractionListener.onItemClicked(info, bindingAdapterPosition)
            }
            when (info.isPartiallyVisible) {
                true -> updateUiWithPartiallyVisible(info)
                false -> updateUiWithFullVisible(info)
            }
        }
    }

    private fun ItemQuizShortAnswerResultBinding.updateUiWithPartiallyVisible(info: QuizAnswerResultInfo) {
        clFullResult.visibility = View.GONE
        clPartialResult.visibility = View.VISIBLE
        when (info.answerResultState) {
            AnswerResultState.ANSWERED -> {
                mcvRoot.setStrokeColor(ColorStateList.valueOf(color78CB3D))
                mcvRoot.setCardBackgroundColor(colorF1FAEB)
                tvAbsentPartial.visibility = View.GONE
                tvNoAnswerPartial.visibility = View.GONE
                tvShortAnswerPartial.visibility = View.VISIBLE
                tvShortAnswerPartial.text = info.answerStringData
            }
            AnswerResultState.NO_ANSWER -> {
                mcvRoot.setStrokeColor(ColorStateList.valueOf(colorC3C7C7))
                mcvRoot.setCardBackgroundColor(colorNeutral0)
                tvAbsentPartial.visibility = View.GONE
                tvNoAnswerPartial.visibility = View.VISIBLE
                tvShortAnswerPartial.visibility = View.GONE
            }
            AnswerResultState.ABSENT -> {
                mcvRoot.setStrokeColor(ColorStateList.valueOf(colorC3C7C7))
                mcvRoot.setCardBackgroundColor(colorNeutral0)
                tvAbsentPartial.visibility = View.VISIBLE
                tvNoAnswerPartial.visibility = View.GONE
                tvShortAnswerPartial.visibility = View.GONE
            }
            AnswerResultState.CORRECT,
            AnswerResultState.INCORRECT -> {}
        }
    }

    private fun ItemQuizShortAnswerResultBinding.updateUiWithFullVisible(info: QuizAnswerResultInfo) {
        clFullResult.visibility = View.VISIBLE
        clPartialResult.visibility = View.GONE
        val seatNumberAndName = "${info.displaySeatNumber}  ${info.displayName}"
        when (info.answerResultState) {
            AnswerResultState.ANSWERED -> {
                mcvRoot.setStrokeColor(ColorStateList.valueOf(color78CB3D))
                mcvRoot.setCardBackgroundColor(colorF1FAEB)
                viewDivider.setBackgroundColor(color78CB3D)
                tvNumberAndName.setTextColor(colorNeutral0)
                tvNumberAndName.setBackgroundColor(color78CB3D)
                tvNumberAndName.text = seatNumberAndName
                tvAbsentFull.visibility = View.GONE
                tvNoAnswerFull.visibility = View.GONE
                tvShortAnswerFull.visibility = View.VISIBLE
                tvShortAnswerFull.text = info.answerStringData
            }
            AnswerResultState.NO_ANSWER -> {
                mcvRoot.setStrokeColor(ColorStateList.valueOf(colorC3C7C7))
                mcvRoot.setCardBackgroundColor(colorNeutral0)
                viewDivider.setBackgroundColor(colorC3C7C7)
                tvNumberAndName.setTextColor(color2E3133)
                tvNumberAndName.setBackgroundColor(colorC3C7C7)
                tvNumberAndName.text = seatNumberAndName
                tvAbsentFull.visibility = View.GONE
                tvNoAnswerFull.visibility = View.VISIBLE
                tvShortAnswerFull.visibility = View.GONE
            }
            AnswerResultState.ABSENT -> {
                mcvRoot.setStrokeColor(ColorStateList.valueOf(colorC3C7C7))
                mcvRoot.setCardBackgroundColor(colorNeutral0)
                viewDivider.setBackgroundColor(colorC3C7C7)
                tvNumberAndName.setTextColor(colorC3C7C7)
                tvNumberAndName.setBackgroundColor(colorNeutral0)
                tvNumberAndName.text = seatNumberAndName
                tvAbsentFull.visibility = View.VISIBLE
                tvNoAnswerFull.visibility = View.GONE
                tvShortAnswerFull.visibility = View.GONE
            }
            AnswerResultState.CORRECT,
            AnswerResultState.INCORRECT -> {}
        }
    }
}
