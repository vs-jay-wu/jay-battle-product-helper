package com.viewsonic.classswift.ui.window.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.ClassroomInfo
import com.viewsonic.classswift.databinding.ViewItemSelectOrgClassBinding

class SelectOrgAndClassAdapter(
    val context: Context,
    private val onItemInteractionListener: OnItemInteractionListener
) : ListAdapter<ClassroomInfo, RecyclerView.ViewHolder>(diffCallback) {
    // Adapter position 0 is the header (create-class button); class items occupy
    // positions HEADER_OFFSET..currentList.size, plus an optional loading placeholder
    // at the tail when showLoadingPlaceholder is true.
    private var selectedPosition: Int = HEADER_OFFSET

    inner class ViewHolder(val binding: ViewItemSelectOrgClassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(classroomInfo: ClassroomInfo, position: Int) {
            binding.tvClassName.text = classroomInfo.displayName

            val isSelected = position == selectedPosition
            binding.clSelectClass.isSelected = isSelected

            configureItemState(classroomInfo)

            binding.clSelectClass.setOnClickListener {
                // Use bindingAdapterPosition so list mutations (e.g. addItemToTopOfNonOngoing)
                // never leave a stale captured `position` pointing at the wrong row — or worse,
                // at the header at position 0.
                val current = bindingAdapterPosition
                if (current == RecyclerView.NO_POSITION) return@setOnClickListener
                val previousPosition = selectedPosition
                selectedPosition = current
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onItemInteractionListener.onItemSelected(classroomInfo)
            }
        }

        private fun configureItemState(classroomInfo: ClassroomInfo) {
            val isRoster = classroomInfo.isRoster()
            val isOngoing = classroomInfo.isLessonOnGoing()
            val isDeletable = currentList.size > 1 && !isOngoing && !isRoster

            when {
                isOngoing -> {
                    binding.llOngoingBadge.isVisible = true
                    binding.llActions.isVisible = false
                }
                isRoster -> {
                    binding.llOngoingBadge.isVisible = false
                    binding.llActions.isVisible = false
                    binding.ivIcon.isVisible = true
                    setRosterIcon(classroomInfo)
                }
                else -> {
                    binding.llOngoingBadge.isVisible = false
                    binding.llActions.isVisible = true
                    binding.ivIcon.isVisible = false
                    binding.ivRename.setOnClickListener {
                        onItemInteractionListener.onItemRename(classroomInfo)
                    }
                    binding.ivDelete.isEnabled = isDeletable
                    binding.ivDelete.alpha = if (isDeletable) 1f else 0.3f
                    binding.ivDelete.setOnClickListener {
                        if (isDeletable) {
                            onItemInteractionListener.onItemDelete(classroomInfo)
                        }
                    }
                }
            }
        }

        private fun setRosterIcon(classroomInfo: ClassroomInfo) {
            when (classroomInfo.originType) {
                ClassroomInfo.OriginType.GOOGLE_CLASSROOM -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_google_classroom)
                }
                ClassroomInfo.OriginType.CLASS_LINK -> {
                    binding.ivIcon.setImageResource(R.drawable.selector_class_classlink)
                }
                else -> {
                    binding.ivIcon.isVisible = false
                }
            }
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)
    inner class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    var showLoadingPlaceholder = false
        set(value) {
            if (field == value) return
            field = value
            // Placeholder occupies only the tail slot — toggling it is a single
            // insert/remove, avoiding notifyDataSetChanged and preserving animations.
            if (value) {
                notifyItemInserted(itemCount - 1)
            } else {
                // After clearing the field, itemCount no longer counts the placeholder,
                // so the removed adapter position equals the new itemCount.
                notifyItemRemoved(itemCount)
            }
        }

    fun addItem(item: ClassroomInfo) {
        val resultList = currentList.toMutableList()
        resultList.add(item)
        submitList(resultList) {
            // isDeletable depends on currentList.size transitioning 1 → 2+.
            // DiffUtil sees existing items as unchanged content and skips re-bind,
            // so force re-bind to refresh the delete icon enabled state.
            notifyItemRangeChanged(HEADER_OFFSET, currentList.size)
        }
    }

    /**
     * Inserts [newItem] at the top of the non-ongoing block AND selects it as one atomic step.
     *
     * The select must run inside [submitList]'s callback: ListAdapter dispatches DiffUtil on a
     * background thread when both the old and new lists are non-empty, so a synchronous
     * selectItem after submitList would not yet see [newItem] in currentList and fail silently.
     */
    fun addItemToTopOfNonOngoingAndSelect(
        newItem: ClassroomInfo,
        onCommitted: (() -> Unit)? = null
    ) {
        val (ongoing, nonOngoing) = currentList.partition { it.isLessonOnGoing() }
        submitList(ongoing + newItem + nonOngoing) {
            selectItem(newItem)
            // isDeletable depends on currentList.size transitioning 1 → 2+;
            // force re-bind so the delete icon's enabled state refreshes.
            notifyItemRangeChanged(HEADER_OFFSET, currentList.size)
            onCommitted?.invoke()
        }
    }

    private fun selectItem(item: ClassroomInfo) {
        val dataPosition = currentList.indexOfFirst { it.id == item.id }
        if (dataPosition == -1) return
        val adapterPosition = toAdapterPosition(dataPosition)
        val previousPosition = selectedPosition
        selectedPosition = adapterPosition
        if (previousPosition != adapterPosition) notifyItemChanged(previousPosition)
        notifyItemChanged(adapterPosition)
        onItemInteractionListener.onItemSelected(item)
    }

    fun addItems(list: List<ClassroomInfo>) {
        val resultList = currentList.toMutableList()
        resultList.addAll(list)
        submitList(resultList) {
            // Same rationale as addItem — size-dependent isDeletable may change
            // for pre-existing items when crossing the size > 1 boundary.
            notifyItemRangeChanged(HEADER_OFFSET, currentList.size)
        }
    }

    fun setItems(list: List<ClassroomInfo>, onCommitted: (() -> Unit)? = null) {
        selectedPosition = HEADER_OFFSET
        submitList(list.toList()) {
            onCommitted?.invoke()
        }
    }

    override fun getItemCount(): Int = computeItemCount(super.getItemCount(), showLoadingPlaceholder)

    override fun getItemViewType(position: Int): Int =
        resolveViewType(position, itemCount, showLoadingPlaceholder)

    /**
     * Returns the currently selected class, or null when the list is empty.
     *
     * Empty list is possible immediately after [setItems] with an empty list (e.g. on org
     * switch) where selectedPosition is reset to HEADER_OFFSET but no data row exists yet.
     * Callers must handle null rather than relying on external guards, so that the empty-list
     * invariant lives on the adapter rather than being scattered across windows.
     */
    fun getSelectedItemOrNull(): ClassroomInfo? {
        if (currentList.isEmpty()) return null
        val dataPosition = toDataPosition(selectedPosition).coerceIn(0, currentList.lastIndex)
        return getItem(dataPosition)
    }

    fun removeItem(item: ClassroomInfo) {
        val dataPosition = currentList.indexOf(item)
        if (dataPosition == -1) return
        val resultList = currentList.toMutableList()
        selectedPosition = computeSelectedAfterRemoval(selectedPosition, dataPosition)
        resultList.removeAt(dataPosition)
        submitList(resultList) {
            // isDeletable depends on currentList.size, which changes after removal.
            // DiffUtil sees remaining items as unchanged content and skips re-bind,
            // so force re-bind to refresh the delete icon enabled state.
            notifyItemRangeChanged(HEADER_OFFSET, currentList.size)
        }
    }

    fun updateSelectItem(item: ClassroomInfo) {
        val resultList = currentList.toMutableList()
        resultList[toDataPosition(selectedPosition)] = item
        submitList(resultList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_item_create_class, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_LOADING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_item_class_loading, parent, false)
                LoadingViewHolder(view)
            }
            else -> {
                val binding = ViewItemSelectOrgClassBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val isEnabled = !showLoadingPlaceholder
                holder.itemView.isEnabled = isEnabled
                holder.itemView.alpha = if (isEnabled) 1f else 0.5f
                holder.itemView.setOnClickListener {
                    if (!showLoadingPlaceholder) {
                        onItemInteractionListener.onCreateClass()
                    }
                }
            }
            is ViewHolder -> holder.bind(getItem(position - HEADER_OFFSET), position)
            is LoadingViewHolder -> Unit
        }
    }

    interface OnItemInteractionListener {
        fun onItemSelected(classroomInfo: ClassroomInfo)
        fun onItemDelete(classroomInfo: ClassroomInfo)
        fun onItemRename(classroomInfo: ClassroomInfo)
        fun onCreateClass()
    }

    companion object {
        const val TYPE_CLASS = 0
        const val TYPE_HEADER = 1
        const val TYPE_LOADING = 2
        const val HEADER_OFFSET = 1

        /** Translate a data-list index into the RecyclerView adapter position (header at 0 shifts everything by HEADER_OFFSET). */
        fun toAdapterPosition(dataPosition: Int): Int = dataPosition + HEADER_OFFSET

        /** Inverse of [toAdapterPosition]: translate an adapter position back into the data-list index. */
        fun toDataPosition(adapterPosition: Int): Int = adapterPosition - HEADER_OFFSET

        /** Total adapter item count = data items + optional loading placeholder + the always-present header. */
        fun computeItemCount(dataSize: Int, showLoadingPlaceholder: Boolean): Int =
            dataSize + (if (showLoadingPlaceholder) 1 else 0) + HEADER_OFFSET

        /** Resolve view type for [position]: header at 0, loading at the tail when shown, class otherwise. */
        fun resolveViewType(position: Int, itemCount: Int, showLoadingPlaceholder: Boolean): Int = when {
            position == 0 -> TYPE_HEADER
            showLoadingPlaceholder && position == itemCount - 1 -> TYPE_LOADING
            else -> TYPE_CLASS
        }

        /**
         * Returns the new selectedPosition after removing the data item at [removedDataPosition].
         * Decrements when the removed item sits at or before the selected position (in adapter
         * coordinates) AND we still have a class to fall back to (selectedPosition > HEADER_OFFSET).
         */
        fun computeSelectedAfterRemoval(
            selectedAdapterPosition: Int,
            removedDataPosition: Int
        ): Int {
            val removedAdapterPosition = toAdapterPosition(removedDataPosition)
            return if (removedAdapterPosition <= selectedAdapterPosition &&
                selectedAdapterPosition > HEADER_OFFSET
            ) {
                selectedAdapterPosition - 1
            } else {
                selectedAdapterPosition
            }
        }

        private val diffCallback = object : DiffUtil.ItemCallback<ClassroomInfo>() {
            override fun areItemsTheSame(oldItem: ClassroomInfo, newItem: ClassroomInfo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ClassroomInfo,
                newItem: ClassroomInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
