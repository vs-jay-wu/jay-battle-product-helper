package com.viewsonic.classswift.ui.window.viewholder

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.databinding.ItemMyContentQuizBinding
import com.viewsonic.classswift.ui.window.adapter.QuizCollectionQuizzesAdapter
import timber.log.Timber

class QuizCollectionMyContentViewHolder(
    private val binding: ItemMyContentQuizBinding,
    private val onItemInteractionListener: QuizCollectionQuizzesAdapter.OnItemInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun onBind(quizInCollectionInfo: QuizInCollectionInfo) {
        val quizType = QuizType.entries.find { it.name == quizInCollectionInfo.quizData.quizType } ?: QuizType.UNSPECIFIED
        with(binding) {
            tvTitle.text = when (quizType) {
                QuizType.TRUE_FALSE -> root.context.getString(R.string.quiz_types_true_false)
                QuizType.SINGLE_SELECT,
                QuizType.MULTIPLE_SELECT -> root.context.getString(R.string.quiz_types_multiple_choice)
                QuizType.RECORD -> root.context.getString(R.string.quiz_types_audio)
                QuizType.SHORT_ANSWER -> root.context.getString(R.string.short_answer_capitalized_first_word)
                QuizType.SINGLE_POLL,
                QuizType.MULTIPLE_POLL -> root.context.getString(R.string.quiz_types_poll)
                else -> ""
            }
            clPlaceholder.isVisible = true
            ivContent.load(quizInCollectionInfo.quizData.imgUrl) {
                allowHardware(false)
                listener(
                    onSuccess = { request, result ->
                        clPlaceholder.isVisible = false
                    },
                    onError = { request, result ->
                        Timber.e("[QuizCollectionMyContentViewHolder] : onError -> ${result.throwable.message}")
                    },
                )
            }
            root.setOnClickListener {
                onItemInteractionListener.onQuizInfoItemClicked(quizInCollectionInfo)
            }
        }
    }
}