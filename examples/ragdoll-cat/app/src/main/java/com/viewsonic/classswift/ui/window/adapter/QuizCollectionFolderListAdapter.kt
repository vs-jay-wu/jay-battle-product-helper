package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.QuizCollectionFolderInfo
import com.viewsonic.classswift.databinding.ItemQuizCollectionFolderBinding
import com.viewsonic.classswift.databinding.ItemQuizCollectionFolderSelectedBinding
import com.viewsonic.classswift.ui.window.viewholder.QuizCollectionFolderNormalViewHolder
import com.viewsonic.classswift.ui.window.viewholder.QuizCollectionFolderSelectedViewHolder

class QuizCollectionFolderListAdapter(
    private val onItemInteractionListener: OnItemInteractionListener,
) : ListAdapter<QuizCollectionFolderInfo, RecyclerView.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (ViewType.entries[viewType]) {
            ViewType.NORMAL -> {
                QuizCollectionFolderNormalViewHolder(
                    ItemQuizCollectionFolderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    onItemInteractionListener
                )
            }
            ViewType.SELECTED -> {
                QuizCollectionFolderSelectedViewHolder(
                    ItemQuizCollectionFolderSelectedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (getItem(position).isSelected) {
            true -> {
                val quizCollectionFolderSelectedViewHolder = holder as QuizCollectionFolderSelectedViewHolder
                quizCollectionFolderSelectedViewHolder.onBind(getItem(position))
            }
            false -> {
                val quizCollectionFolderNormalViewHolder = holder as QuizCollectionFolderNormalViewHolder
                quizCollectionFolderNormalViewHolder.onBind(getItem(position))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).isSelected) {
            true -> ViewType.SELECTED.ordinal
            false -> ViewType.NORMAL.ordinal
        }
    }

    enum class ViewType {
        NORMAL,
        SELECTED
    }

    interface OnItemInteractionListener {
        fun onNormalItemClicked(quizCollectionFolderInfo: QuizCollectionFolderInfo)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<QuizCollectionFolderInfo>() {
            override fun areItemsTheSame(
                oldItem: QuizCollectionFolderInfo,
                newItem: QuizCollectionFolderInfo
            ): Boolean {
                return oldItem.folder.id == newItem.folder.id
            }

            override fun areContentsTheSame(
                oldItem: QuizCollectionFolderInfo,
                newItem: QuizCollectionFolderInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}