package com.viewsonic.classswift.ui.window.viewholder

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.batchquiz.BatchQuizRecyclerViewUiData
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.databinding.ItemBatchQuizTextQuizBinding
import com.viewsonic.classswift.ui.window.adapter.BatchQuizzesAdapter
import com.viewsonic.classswift.utils.extension.isLatexContent

class BatchQuizTextQuizViewHolder(
    private val binding: ItemBatchQuizTextQuizBinding,
    private val listener: BatchQuizzesAdapter.OnItemInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind(adapterPosition: Int, quizInfo: BatchQuizRecyclerViewUiData.QuizInfo) {
        val quizType = QuizType.entries.find { it.name == quizInfo.quizInCollectionInfo.quizData.quizType } ?: QuizType.UNSPECIFIED
        with(binding) {
            applyContent(quizInfo)
            applySelectionState(quizInfo)
            applyClickListeners(adapterPosition, quizInfo)

            tvTag.isVisible = quizInfo.quizInCollectionInfo.quizData.subject.isNotBlank()
            if (tvTag.isVisible) {
                tvTag.text = quizInfo.quizInCollectionInfo.subjectDisplayName
            }

            tvQuizType.text = when (quizType) {
                QuizType.TRUE_FALSE -> root.context.getString(R.string.quiz_types_true_false)
                QuizType.SINGLE_SELECT,
                QuizType.MULTIPLE_SELECT -> root.context.getString(R.string.quiz_types_multiple_choice)
                QuizType.RECORD -> root.context.getString(R.string.quiz_types_audio)
                QuizType.SHORT_ANSWER -> root.context.getString(R.string.short_answer_capitalized_first_word)
                else -> ""
            }
        }
    }

    fun onBindSelection(adapterPosition: Int, quizInfo: BatchQuizRecyclerViewUiData.QuizInfo) {
        with(binding) {
            applySelectionState(quizInfo)
            applyClickListeners(adapterPosition, quizInfo)
        }
    }

    private fun ItemBatchQuizTextQuizBinding.applyContent(quizInfo: BatchQuizRecyclerViewUiData.QuizInfo, isForced: Boolean = false) {
        val isLatexContent = quizInfo.quizInCollectionInfo.quizData.content.isLatexContent()
        tvQuestion.isVisible = !isLatexContent
        cskvQuestion.isVisible = isLatexContent
        when (isLatexContent) {
            true -> {
                cskvQuestion.setText(quizInfo.quizInCollectionInfo.quizData.content, isForced)
            }
            false -> {
                tvQuestion.text = quizInfo.quizInCollectionInfo.quizData.content
            }
        }
    }

    private fun ItemBatchQuizTextQuizBinding.applyClickListeners(adapterPosition: Int, quizInfo: BatchQuizRecyclerViewUiData.QuizInfo) {
        val isLatexContent = quizInfo.quizInCollectionInfo.quizData.content.isLatexContent()
        if (isLatexContent) {
            cskvQuestion.setOnClickListener {
                if (quizInfo.selectionState != BatchQuizRecyclerViewUiData.QuizInfo.SelectionState.STATE_DISABLED) {
                    listener.onQuizInfoItemClicked(adapterPosition, quizInfo)
                }
            }
        }
        root.setOnClickListener {
            if (quizInfo.selectionState != BatchQuizRecyclerViewUiData.QuizInfo.SelectionState.STATE_DISABLED) {
                listener.onQuizInfoItemClicked(adapterPosition, quizInfo)
            }
        }
    }


    private fun ItemBatchQuizTextQuizBinding.applySelectionState(quizInfo: BatchQuizRecyclerViewUiData.QuizInfo) {
        tvSelectedSequenceNumber.text = quizInfo.selectedSequenceNumber.toString()
        when (quizInfo.selectionState) {
            BatchQuizRecyclerViewUiData.QuizInfo.SelectionState.STATE_NORMAL -> {
                mcvRoot.strokeColor = root.context.getColor(R.color.neutral_450)
                mcvRoot.strokeWidth = root.context.resources.getDimensionPixelSize(R.dimen.border_200)
                tvSelectedSequenceNumber.background = root.context.getDrawable(R.drawable.bg_neural0_radius200_line_neutral500_border200)
                tvSelectedSequenceNumber.setTextColor(root.context.getColor(R.color.neutral_0))
            }
            BatchQuizRecyclerViewUiData.QuizInfo.SelectionState.STATE_SELECTED -> {
                mcvRoot.strokeColor = root.context.getColor(R.color.color_0A8CF0)
                mcvRoot.strokeWidth = root.context.resources.getDimensionPixelSize(R.dimen.border_400)
                tvSelectedSequenceNumber.background = root.context.getDrawable(R.drawable.bg_0a8cf0_radius200)
                tvSelectedSequenceNumber.setTextColor(root.context.getColor(R.color.neutral_0))
            }
            BatchQuizRecyclerViewUiData.QuizInfo.SelectionState.STATE_DISABLED -> {
                mcvRoot.strokeColor = root.context.getColor(R.color.neutral_450)
                mcvRoot.strokeWidth = root.context.resources.getDimensionPixelSize(R.dimen.border_200)
                tvSelectedSequenceNumber.background = root.context.getDrawable(R.drawable.bg_neural200_radius200_line_neutral450_border200)
                tvSelectedSequenceNumber.setTextColor(root.context.getColor(R.color.neutral_200))
            }
        }
    }
}
