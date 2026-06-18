package com.viewsonic.classswift.ui.window.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.databinding.ItemBatchQuizTypeHeaderBinding

class BatchQuizTypeHeaderViewHolder(
    private val binding: ItemBatchQuizTypeHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind(quizType: QuizType) {
        binding.tvTypeHeader.text = when (quizType) {
            QuizType.TRUE_FALSE -> binding.root.context.getString(R.string.quiz_types_true_false)
            QuizType.SINGLE_SELECT,
            QuizType.MULTIPLE_SELECT -> binding.root.context.getString(R.string.quiz_types_multiple_choice)
            else -> "Not Supported"
        }
    }
}
