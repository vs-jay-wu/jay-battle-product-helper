package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.databinding.ItemMvbQcQuizBinding
import com.viewsonic.classswift.ui.window.viewholder.MvbQuizCollectionQuizViewHolder

class MvbQuizCollectionQuizzesAdapter(
    private val canUseStandards: Boolean,
    private val onQuizClick: (QuizInCollectionInfo) -> Unit,
) : PagingDataAdapter<QuizInCollectionInfo, MvbQuizCollectionQuizViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MvbQuizCollectionQuizViewHolder {
        val binding = ItemMvbQcQuizBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MvbQuizCollectionQuizViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MvbQuizCollectionQuizViewHolder, position: Int) {
        getItem(position)?.let { info -> holder.bind(info, canUseStandards, onQuizClick) }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<QuizInCollectionInfo>() {
            override fun areItemsTheSame(oldItem: QuizInCollectionInfo, newItem: QuizInCollectionInfo): Boolean =
                oldItem.quizData.id == newItem.quizData.id

            override fun areContentsTheSame(oldItem: QuizInCollectionInfo, newItem: QuizInCollectionInfo): Boolean =
                oldItem == newItem
        }
    }
}
