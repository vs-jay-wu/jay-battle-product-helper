package com.viewsonic.classswift.ui.widget.task.records

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.records.RecordListInfo
import timber.log.Timber

class RecordListAdapter : ListAdapter<RecordListInfo, RecyclerView.ViewHolder>(diffCallback) {
    private var currentSelectTaskId: String = ""
    private var itemEventListener: TaskItemEventListener? = null

    interface TaskItemEventListener {
        fun onTaskListItemSelected(task: RecordListInfo.TaskItem)
        fun onTaskListInitSelected(task: RecordListInfo.TaskItem)
        fun onAdapterDataCommited()
    }

    fun setTaskItemEventListener(listener: TaskItemEventListener) {
        itemEventListener = listener
    }

    fun getCurrentSelectedTaskId(): String {
        return currentSelectTaskId
    }

    fun clearFocusTask() {
        currentSelectTaskId = ""
    }

    fun updateAll(
        data: List<RecordListInfo>,
        // true = select the first item, false = keep the current selection (or none)
        selectFirstIfEmpty: Boolean = true
    ) {
        Timber.d("update all data : $data")

        val taskToSelect = if (selectFirstIfEmpty) {
            data.filterIsInstance<RecordListInfo.TaskItem>().firstOrNull()

        } else {
            data.filterIsInstance<RecordListInfo.TaskItem>()
                .find { it.id == currentSelectTaskId }
        }

        taskToSelect?.let {
            val updatedList = data.map { item ->
                when (item) {
                    is RecordListInfo.TaskItem -> {
                        item.copy(isSelected = item.id == it.id)
                    }

                    else -> item
                }
            }
            currentSelectTaskId = it.id
            submitList(updatedList) {
                itemEventListener?.onAdapterDataCommited()
            }

            itemEventListener?.onTaskListInitSelected(task = it)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val itemView = RecordsHeaderView(parent.context)
                HeaderViewHolder(itemView = itemView)
            }

            VIEW_TYPE_ITEM -> {
                val itemView = RecordsListItemView(parent.context)
                ItemViewHolder(itemView = itemView).apply {
                    setEventListener { task ->
                        itemEventListener?.onTaskListItemSelected(task = task)
                    }
                }
            }

            else -> {
                Timber.e("Unknown view type: $viewType")
                val view = View(parent.context)
                FallbackViewHolder(view = view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val data = currentList[position]) {

            is RecordListInfo.Header -> {
                if (holder is HeaderViewHolder) {
                    holder.bind(data = data)
                }
            }

            is RecordListInfo.TaskItem -> {
                if (holder is ItemViewHolder) {
                    holder.bind(data = data)
                }
            }

            else -> Unit
        }
    }

    override fun getItemCount(): Int = currentList.size

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position]) {
            is RecordListInfo.Header -> VIEW_TYPE_HEADER
            is RecordListInfo.TaskItem -> VIEW_TYPE_ITEM
            else -> VIEW_TYPE_UNKNOWN
        }
    }

    private class HeaderViewHolder(itemView: RecordsHeaderView) : RecyclerView.ViewHolder(itemView) {
        private var view: RecordsHeaderView = itemView

        fun bind(data: RecordListInfo.Header) {
            when (data.type) {
                RecordListInfo.HeaderType.RECEIVING -> view.setReceivingStyle()
                RecordListInfo.HeaderType.TASKENDED -> view.setTaskEndedStyle()
            }
        }
    }

    private class ItemViewHolder(itemView: RecordsListItemView) : RecyclerView.ViewHolder(itemView) {
        private var view: RecordsListItemView = itemView
        private var clickListener: ((RecordListInfo.TaskItem) -> Unit)? = null

        fun setEventListener(listener: (RecordListInfo.TaskItem) -> Unit) {
            clickListener = listener
        }

        fun bind(data: RecordListInfo.TaskItem) {
            view.apply {
                setData(data = data)
                setOnClickedListener { task ->
                    clickListener?.invoke(task)
                }
            }
        }
    }

    fun toggleSelectedState(taskId: String): Boolean {

        val targetItem = currentList.find {
            it is RecordListInfo.TaskItem && it.id == taskId
        } as? RecordListInfo.TaskItem

        if (targetItem?.isSelected == true) {
            return false
        }

        currentSelectTaskId = taskId

        val updatedList = currentList.map { item ->
            when (item) {
                is RecordListInfo.TaskItem -> {
                    val newSelected = item.id == taskId
                    if (newSelected == item.isSelected) item
                    else item.copy(isSelected = newSelected)
                }

                is RecordListInfo.Header -> {
                    item.copy()
                }

                else -> item
            }
        }.toList()

        submitList(updatedList) {
            itemEventListener?.onAdapterDataCommited()
        }

        return true
    }

    fun getAdapterIndexByTaskId(taskId: String): Int {
        return currentList.indexOfFirst {
            it is RecordListInfo.TaskItem && it.id == taskId
        }
    }

    private class FallbackViewHolder(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_UNKNOWN = 2

        val diffCallback = object : DiffUtil.ItemCallback<RecordListInfo>() {
            override fun areItemsTheSame(oldItem: RecordListInfo, newItem: RecordListInfo): Boolean {
                return when {
                    oldItem is RecordListInfo.TaskItem && newItem is RecordListInfo.TaskItem ->
                        oldItem.id == newItem.id

                    oldItem is RecordListInfo.Header && newItem is RecordListInfo.Header ->
                        oldItem.type == newItem.type

                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: RecordListInfo, newItem: RecordListInfo): Boolean {
                return oldItem == newItem
            }
        }
    }
}
