package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.batchquiz.BatchQuizRecyclerViewUiData
import com.viewsonic.classswift.databinding.ItemBatchQuizHintHeaderBinding

class BatchQuizHintHeaderAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return HintHeaderViewHolder(
            ItemBatchQuizHintHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {}

    override fun getItemCount(): Int {
        return 1
    }

    override fun getItemViewType(position: Int): Int {
        return BatchQuizRecyclerViewUiData.QuizHintHeader.VIEW_TYPE
    }

    inner class HintHeaderViewHolder(viewBinding: ItemBatchQuizHintHeaderBinding) :
        RecyclerView.ViewHolder(viewBinding.root)
}
