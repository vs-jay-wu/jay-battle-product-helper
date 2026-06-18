package com.viewsonic.classswift.ui.window.viewholder

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.QuizCollectionFolderInfo
import com.viewsonic.classswift.databinding.ItemQuizCollectionFolderSelectedBinding
import com.viewsonic.classswift.ui.window.adapter.QuizCollectionFolderListAdapter

class QuizCollectionFolderSelectedViewHolder(
    private val binding: ItemQuizCollectionFolderSelectedBinding
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun onBind(quizCollectionFolderInfo: QuizCollectionFolderInfo) {
        with(binding) {
            tvTitle.text = quizCollectionFolderInfo.folder.name
        }
    }
}