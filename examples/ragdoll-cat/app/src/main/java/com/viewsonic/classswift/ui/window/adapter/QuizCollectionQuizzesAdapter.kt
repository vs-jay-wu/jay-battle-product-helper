package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.children
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.databinding.ItemMyContentQuizBinding
import com.viewsonic.classswift.databinding.ItemTextQuizBinding
import com.viewsonic.classswift.ui.window.viewholder.QuizCollectionMyContentViewHolder
import com.viewsonic.classswift.ui.window.viewholder.QuizCollectionTextViewHolder
import com.viewsonic.classswift.ui.widget.KatexView

class QuizCollectionQuizzesAdapter(private val onItemInteractionListener: OnItemInteractionListener) : PagingDataAdapter<QuizInCollectionInfo, RecyclerView.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TEXT -> QuizCollectionTextViewHolder(
                ItemTextQuizBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                onItemInteractionListener
            )
            else -> QuizCollectionMyContentViewHolder(
                ItemMyContentQuizBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                onItemInteractionListener
            )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        getItem(position)?.let { quizInCollectionInfo ->
            when (holder) {
                is QuizCollectionTextViewHolder -> holder.onBind(quizInCollectionInfo)
                is QuizCollectionMyContentViewHolder -> holder.onBind(quizInCollectionInfo)
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.children.forEach { child ->
            child.findViewById<KatexView>(R.id.cskv_question)?.release()
        }
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item?.isTextQuiz() == true) {
            VIEW_TYPE_TEXT
        } else {
            VIEW_TYPE_IMAGE
        }
    }

    interface OnItemInteractionListener {
        fun onQuizInfoItemClicked(quizInCollectionInfo: QuizInCollectionInfo)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<QuizInCollectionInfo>() {
            override fun areItemsTheSame(
                oldItem: QuizInCollectionInfo,
                newItem: QuizInCollectionInfo
            ): Boolean {
                return oldItem.quizData.id == newItem.quizData.id
            }

            override fun areContentsTheSame(
                oldItem: QuizInCollectionInfo,
                newItem: QuizInCollectionInfo
            ): Boolean {
                return oldItem == newItem
            }
        }

        private const val VIEW_TYPE_IMAGE = 0
        private const val VIEW_TYPE_TEXT = 1
    }
}
