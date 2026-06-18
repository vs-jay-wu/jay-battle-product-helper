package com.viewsonic.classswift.ui.window.viewholder

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.databinding.ItemTextQuizBinding
import com.viewsonic.classswift.ui.window.adapter.QuizCollectionQuizzesAdapter
import com.viewsonic.classswift.utils.extension.isLatexContent

class QuizCollectionTextViewHolder(
    private val binding: ItemTextQuizBinding,
    private val onItemInteractionListener: QuizCollectionQuizzesAdapter.OnItemInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind(quizInCollectionInfo: QuizInCollectionInfo) {
        val quizType = QuizType.entries.find { it.name == quizInCollectionInfo.quizData.quizType } ?: QuizType.UNSPECIFIED
        with(binding) {
            val isLatexContent = quizInCollectionInfo.quizData.content.isLatexContent()
            tvQuestion.isVisible = !isLatexContent
            cskvQuestion.isVisible = isLatexContent
            when (isLatexContent) {
                true -> {
                    cskvQuestion.setText(quizInCollectionInfo.quizData.content)
                    cskvQuestion.setOnClickListener {
                        onItemInteractionListener.onQuizInfoItemClicked(quizInCollectionInfo)
                    }
                }
                false -> {
                    tvQuestion.text = quizInCollectionInfo.quizData.content
                }
            }
            tvTag.isVisible = quizInCollectionInfo.quizData.subject.isNotBlank()
            if (tvTag.isVisible) {
                tvTag.text = quizInCollectionInfo.subjectDisplayName
            }
            tvQuizType.text = when (quizType) {
                QuizType.TRUE_FALSE -> root.context.getString(R.string.quiz_types_true_false)
                QuizType.SINGLE_SELECT,
                QuizType.MULTIPLE_SELECT -> root.context.getString(R.string.quiz_types_multiple_choice)
                QuizType.RECORD -> root.context.getString(R.string.quiz_types_audio)
                QuizType.SHORT_ANSWER -> root.context.getString(R.string.short_answer_capitalized_first_word)
                else -> ""
            }
            root.setOnClickListener {
                onItemInteractionListener.onQuizInfoItemClicked(quizInCollectionInfo)
            }
        }
    }
}
