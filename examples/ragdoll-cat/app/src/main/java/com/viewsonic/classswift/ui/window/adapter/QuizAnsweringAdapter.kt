package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.databinding.ItemQuizAnsweringBinding
import com.viewsonic.classswift.databinding.ItemQuizMultipleAnsweringBinding
import com.viewsonic.classswift.databinding.ItemQuizShortAnswerAnsweringBinding
import com.viewsonic.classswift.ui.window.viewholder.QuizAnsweringViewHolder
import com.viewsonic.classswift.ui.window.viewholder.QuizMultipleAnsweringViewHolder
import com.viewsonic.classswift.ui.window.viewholder.QuizShortAnswerAnsweringViewHolder

class QuizAnsweringAdapter(
    private val itemClickCallback: (QuizAnsweringInfo) -> Unit
) : ListAdapter<QuizAnsweringInfo, RecyclerView.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            MULTIPLE_POLL,
            MULTIPLE_SELECTION -> {
                QuizMultipleAnsweringViewHolder(
                    ItemQuizMultipleAnsweringBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ) {
                    itemClickCallback(it)
                }
            }
            SHORT_ANSWER -> {
                QuizShortAnswerAnsweringViewHolder(
                    ItemQuizShortAnswerAnsweringBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ) {
                    itemClickCallback(it)
                }
            }
            else -> {
                QuizAnsweringViewHolder(
                    ItemQuizAnsweringBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ) {
                    itemClickCallback(it)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val viewType = getItemViewType(position)
        when (viewType) {
            MULTIPLE_POLL,
            MULTIPLE_SELECTION -> {
                (holder as QuizMultipleAnsweringViewHolder).onBind(item)
            }
            SHORT_ANSWER -> {
                (holder as QuizShortAnswerAnsweringViewHolder).onBind(item)
            }
            else -> {
                (holder as QuizAnsweringViewHolder).onBind(item)
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
            QuizType.MULTIPLE_POLL-> {
                MULTIPLE_POLL
            }
            else -> {
                SINGLE_SELECTION
            }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<QuizAnsweringInfo>() {
            override fun areItemsTheSame(
                oldItem: QuizAnsweringInfo,
                newItem: QuizAnsweringInfo
            ): Boolean {
                return oldItem.serialNumber == newItem.serialNumber
            }

            override fun areContentsTheSame(
                oldItem: QuizAnsweringInfo,
                newItem: QuizAnsweringInfo
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