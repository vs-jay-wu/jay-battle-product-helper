package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.databinding.ItemQuizAnswerResultBinding
import com.viewsonic.classswift.databinding.ItemQuizMultipleAnswerResultBinding
import com.viewsonic.classswift.databinding.ItemQuizShortAnswerResultBinding
import com.viewsonic.classswift.ui.window.viewholder.QuizAnswerResultViewHolder
import com.viewsonic.classswift.ui.window.viewholder.QuizMultipleAnswerResultViewHolder
import com.viewsonic.classswift.ui.window.viewholder.QuizShortAnswerResultViewHolder

class QuizAnswerResultAdapter(
    private val onItemInteractionListener: OnItemInteractionListener = object : OnItemInteractionListener {}
) : ListAdapter<QuizAnswerResultInfo, RecyclerView.ViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            MULTIPLE_POLL,
            MULTIPLE_SELECTION -> {
                QuizMultipleAnswerResultViewHolder(
                    ItemQuizMultipleAnswerResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )
            }
            SHORT_ANSWER -> {
                QuizShortAnswerResultViewHolder(
                    ItemQuizShortAnswerResultBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    onItemInteractionListener
                )
            }
            else -> {
                QuizAnswerResultViewHolder(
                    ItemQuizAnswerResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val viewType = getItemViewType(position)
        when (viewType) {
            MULTIPLE_POLL,
            MULTIPLE_SELECTION -> {
                (holder as QuizMultipleAnswerResultViewHolder).onBind(item)
            }
            SHORT_ANSWER -> {
                (holder as QuizShortAnswerResultViewHolder).onBind(item)
            }
            else -> {
                (holder as QuizAnswerResultViewHolder).onBind(item)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).quizType) {
            QuizType.MULTIPLE_SELECT -> {
                MULTIPLE_SELECTION
            }
            QuizType.SHORT_ANSWER -> {
                SHORT_ANSWER
            }
            QuizType.MULTIPLE_POLL ->{
                MULTIPLE_POLL
            }
            else -> {
                SINGLE_SELECTION
            }
        }
    }

    interface OnItemInteractionListener {
        fun onItemClicked(info: QuizAnswerResultInfo, position: Int) {}
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<QuizAnswerResultInfo>() {
            override fun areItemsTheSame(
                oldItem: QuizAnswerResultInfo,
                newItem: QuizAnswerResultInfo
            ): Boolean {
                return oldItem.serialNumber == newItem.serialNumber
            }

            override fun areContentsTheSame(
                oldItem: QuizAnswerResultInfo,
                newItem: QuizAnswerResultInfo
            ): Boolean {
                return oldItem == newItem
            }
        }

        private const val SINGLE_SELECTION: Int = 1
        private const val MULTIPLE_SELECTION: Int = 2
        private const val SHORT_ANSWER: Int = 3
        private const val MULTIPLE_POLL: Int = 4
    }
}