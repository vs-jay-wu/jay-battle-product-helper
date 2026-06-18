package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.AnswerOptionInfo
import com.viewsonic.classswift.databinding.ItemQuizScoreBarChartBinding
import com.viewsonic.classswift.ui.window.viewholder.QuizAnswerOptionBarChartViewHolder

class QuizAnswerOptionAdapter(private var clickItemListener: OptionChartBarViewHolderListener) :
    ListAdapter<AnswerOptionInfo, RecyclerView.ViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return QuizAnswerOptionBarChartViewHolder(
            ItemQuizScoreBarChartBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), clickItemListener
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as QuizAnswerOptionBarChartViewHolder).onBind(getItem(position))
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<AnswerOptionInfo>() {
            override fun areItemsTheSame(
                oldItem: AnswerOptionInfo,
                newItem: AnswerOptionInfo
            ): Boolean {
                return oldItem.position == newItem.position && oldItem.answerType == newItem.answerType
            }

            override fun areContentsTheSame(
                oldItem: AnswerOptionInfo,
                newItem: AnswerOptionInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}

interface OptionChartBarViewHolderListener {
    fun itemClicked(position: Int)
}