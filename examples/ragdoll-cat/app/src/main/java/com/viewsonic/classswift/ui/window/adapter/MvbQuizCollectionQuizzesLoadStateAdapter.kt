package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.databinding.ItemMvbQcPagingFooterBinding

class MvbQuizCollectionQuizzesLoadStateAdapter :
    LoadStateAdapter<MvbQuizCollectionQuizzesLoadStateAdapter.FooterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): FooterViewHolder {
        val binding = ItemMvbQcPagingFooterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return FooterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        return loadState is LoadState.Loading
    }

    class FooterViewHolder(
        private val binding: ItemMvbQcPagingFooterBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(loadState: LoadState) {
            if (loadState is LoadState.Loading) {
                binding.laMqcpfLoading.visibility = View.VISIBLE
                binding.laMqcpfLoading.playAnimation()
            } else {
                binding.laMqcpfLoading.cancelAnimation()
                binding.laMqcpfLoading.visibility = View.GONE
            }
        }
    }
}
