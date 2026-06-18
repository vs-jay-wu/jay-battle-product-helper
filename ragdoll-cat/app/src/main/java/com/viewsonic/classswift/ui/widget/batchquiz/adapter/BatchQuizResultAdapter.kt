package com.viewsonic.classswift.ui.widget.batchquiz.adapter

import android.view.LayoutInflater
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.viewsonic.classswift.data.info.BatchQuizSummaryInfo
import com.viewsonic.classswift.databinding.ItemBatchQuizResultBinding
import com.viewsonic.classswift.ui.widget.batchquiz.viewholder.BatchQuizResultViewHolder


class BatchQuizResultAdapter(val listener: OnItemTitleClickedListener): ListAdapter<BatchQuizSummaryInfo, BatchQuizResultViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): BatchQuizResultViewHolder {
        return BatchQuizResultViewHolder(
            ItemBatchQuizResultBinding.inflate(LayoutInflater.from(parent.context),
                parent,
                false)
        )
    }

    override fun onBindViewHolder(holder: BatchQuizResultViewHolder, position: Int) {
        holder.onBind(getItem(position), listener)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<BatchQuizSummaryInfo>() {
            override fun areItemsTheSame(
                oldItem: BatchQuizSummaryInfo,
                newItem: BatchQuizSummaryInfo
            ): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(
                oldItem: BatchQuizSummaryInfo,
                newItem: BatchQuizSummaryInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    interface OnItemTitleClickedListener {
        fun onShowDetailsResult(summaryInfo: BatchQuizSummaryInfo) {}
    }
}