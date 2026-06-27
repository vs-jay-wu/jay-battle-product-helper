package com.viewsonic.classswift.ui.widget.quiz.viewholder

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.databinding.ItemQuizShortAnswerResultDetailBinding
import com.viewsonic.classswift.ui.widget.quiz.adapter.ShortAnswerResultDetailAdapter

class ShortAnswerResultDetailViewHolder(
    private val binding: ItemQuizShortAnswerResultDetailBinding,
    private val onItemInteractionListener: ShortAnswerResultDetailAdapter.OnItemInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind(quizAnswerResultInfo: QuizAnswerResultInfo, itemCount: Int) {
        with(binding) {
            updatePartiallyVisibleState(quizAnswerResultInfo)
            tvShortAnswerDetail.text = quizAnswerResultInfo.answerStringData
            val title = "${quizAnswerResultInfo.displaySeatNumber}  ${quizAnswerResultInfo.displayName}"
            tvNumberAndName.text = title
            ivLeftArrow.setOnClickListener {
                onItemInteractionListener.onLeftButtonClicked()
            }
            ivRightArrow.setOnClickListener {
                onItemInteractionListener.onRightButtonClicked()
            }
            ivClose.setOnClickListener { onItemInteractionListener.onCloseButtonClicked() }
            ivLeftArrow.isEnabled = itemCount > 1
            ivRightArrow.isEnabled = itemCount > 1
        }
    }

    fun updatePartiallyVisibleState(quizAnswerResultInfo: QuizAnswerResultInfo) {
        with(binding) {
            if (quizAnswerResultInfo.isPartiallyVisible) {
                tvNumberAndName.visibility = View.GONE
                ivClose.setImageDrawable(ContextCompat.getDrawable(binding.root.context, R.drawable.selector_short_answer_result_detail_partial_close_button_image))
            } else {
                tvNumberAndName.visibility = View.VISIBLE
                ivClose.setImageDrawable(ContextCompat.getDrawable(binding.root.context, R.drawable.selector_short_answer_result_detail_full_close_button_image))
            }
        }
    }
}