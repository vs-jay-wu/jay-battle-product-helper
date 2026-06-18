package com.viewsonic.classswift.ui.window.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizCollectionFolderInfo
import com.viewsonic.classswift.databinding.ItemMvbQcFolderBinding

class MvbQuizCollectionFolderViewHolder(
    private val binding: ItemMvbQcFolderBinding,
    role: Role,
) : RecyclerView.ViewHolder(binding.root) {

    enum class Role { DEFAULT, FOLDER }

    init {
        // Padding is fixed per role; ViewHolders are pooled separately by viewType so each holder
        // keeps its role for its lifetime.
        val res = binding.root.resources
        val startPad = when (role) {
            Role.DEFAULT -> res.getDimensionPixelSize(R.dimen.mvb_spacing_400)
            Role.FOLDER -> res.getDimensionPixelSize(R.dimen.mvb_spacing_600)
        }
        val endPad = res.getDimensionPixelSize(R.dimen.mvb_spacing_400)
        binding.root.setPaddingRelative(startPad, binding.root.paddingTop, endPad, binding.root.paddingBottom)
    }

    fun bind(
        info: QuizCollectionFolderInfo,
        role: Role,
        onClick: (QuizCollectionFolderInfo) -> Unit,
    ) {
        binding.tvMqcfName.text = info.folder.name
        binding.root.isSelected = info.isSelected
        binding.ivMqcfIcon.setImageResource(iconFor(role, info.isSelected))
        applyTextColor(info.isSelected)
        binding.root.setOnClickListener { onClick(info) }
    }

    private fun applyTextColor(isSelected: Boolean) {
        val context = binding.root.context
        val colorRes = if (isSelected) R.color.violet_500 else R.color.neutral_900
        binding.tvMqcfName.setTextColor(context.getColor(colorRes))
    }

    private fun iconFor(role: Role, isSelected: Boolean): Int = when (role) {
        Role.DEFAULT -> R.drawable.ic_mvb_qc_default
        Role.FOLDER -> if (isSelected) R.drawable.ic_mvb_qc_folder_open else R.drawable.ic_mvb_qc_folder
    }
}
