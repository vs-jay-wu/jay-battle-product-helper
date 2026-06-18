package com.viewsonic.classswift.ui.window.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.databinding.ItemMvbQcSidebarHeaderBinding

class MvbQuizCollectionSidebarHeaderViewHolder(
    private val binding: ItemMvbQcSidebarHeaderBinding,
) : RecyclerView.ViewHolder(binding.root) {

    // First bind snaps the chevron; subsequent binds animate so toggling shows motion.
    // Only one header instance exists in the list, so this ViewHolder is rebound on the same item.
    private var hasInitializedChevron = false

    fun bind(isExpanded: Boolean, onClick: () -> Unit) {
        val targetRotation = if (isExpanded) ROTATION_EXPANDED else ROTATION_COLLAPSED
        if (hasInitializedChevron) {
            binding.ivMqcshChevron.animate()
                .rotation(targetRotation)
                .setDuration(CHEVRON_ROTATION_DURATION_MS)
                .start()
        } else {
            binding.ivMqcshChevron.rotation = targetRotation
            hasInitializedChevron = true
        }
        binding.root.setOnClickListener { onClick() }
    }

    companion object {
        private const val ROTATION_EXPANDED = 180f
        private const val ROTATION_COLLAPSED = 0f
        private const val CHEVRON_ROTATION_DURATION_MS = 200L
    }
}
