package com.viewsonic.classswift.ui.window.viewholder

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.QuizCollectionFolderInfo
import com.viewsonic.classswift.databinding.ItemQuizCollectionFolderBinding
import com.viewsonic.classswift.ui.window.adapter.QuizCollectionFolderListAdapter

class QuizCollectionFolderNormalViewHolder(
    private val binding: ItemQuizCollectionFolderBinding,
    private val onItemInteractionListener: QuizCollectionFolderListAdapter.OnItemInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun onBind(quizCollectionFolderInfo: QuizCollectionFolderInfo) {
        with(binding) {
            tvTitle.text = quizCollectionFolderInfo.folder.name
            root.setOnClickListener {
                onItemInteractionListener.onNormalItemClicked(quizCollectionFolderInfo)
            }
        }
    }
}