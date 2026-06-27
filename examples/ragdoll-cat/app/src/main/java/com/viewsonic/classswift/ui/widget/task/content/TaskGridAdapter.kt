package com.viewsonic.classswift.ui.widget.task.content

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.task.TaskInfo
import com.viewsonic.classswift.ui.widget.task.enums.CreateTaskOption
import com.viewsonic.classswift.ui.widget.task.link.LinkTaskView
import timber.log.Timber

class TaskGridAdapter : ListAdapter<TaskInfo, RecyclerView.ViewHolder>(diffCallback) {

    private var itemEventListener: TaskItemEventListener? = null

    interface TaskItemEventListener {
        fun onCreateContentSelected(option: CreateTaskOption)
        fun onTaskItemDelete(data: TaskInfo)
        fun onImageUploadSuccess(data: TaskInfo)
        fun onTaskDataSelectUpdate(data: TaskInfo)
    }

    enum class ItemEvent {
        DELETE,
        LOAD_IMAGE_SUCCESS,
        SELECT_CHANGE
    }

    fun updateAll(data: List<TaskInfo>) {
        submitList(data.toList())
    }

    fun setTaskItemEventListener(listener: TaskItemEventListener) {
        itemEventListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        parent.context ?: throw IllegalArgumentException("Create ViewHolder parent context null!")

        return when (viewType) {
            VIEW_TYPE_CREATE -> {
                val itemView = CreateTaskView(parent.context)
                CreateTaskViewHolder(itemView = itemView)
            }

            VIEW_TYPE_CONTENT -> {
                val itemView = ContentTaskView(parent.context)
                ContentTaskViewHolder(itemView = itemView).apply {

                    setEventListener(listener = { event, newData ->
                        Timber.d("Adapter Event: ${event.toString()}")
                        when (event) {
                            ItemEvent.DELETE -> {
                                itemEventListener?.onTaskItemDelete(data = newData)
                            }

                            ItemEvent.SELECT_CHANGE -> {
                                itemEventListener?.onTaskDataSelectUpdate(data = newData)
                            }

                            ItemEvent.LOAD_IMAGE_SUCCESS -> {
                                itemEventListener?.onImageUploadSuccess(data = newData)
                            }
                        }
                    })
                }
            }

            VIEW_TYPE_LINK -> {
                val itemView = LinkTaskView(parent.context)
                LinkTaskViewHolder(itemView = itemView).apply {
                    setEventListener(listener = { event, newData ->
                        Timber.d("Adapter Event: ${event.toString()}")
                        when (event) {
                            ItemEvent.DELETE -> {
                                itemEventListener?.onTaskItemDelete(data = newData)
                            }

                            ItemEvent.SELECT_CHANGE -> {
                                itemEventListener?.onTaskDataSelectUpdate(data = newData)
                            }

                            else -> Unit
                        }
                    })
                }
            }

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val data = currentList[position]) {

            is TaskInfo.TaskCreate -> {
                if (holder is CreateTaskViewHolder) {
                    holder.bind(eventListener = itemEventListener)
                }
            }

            is TaskInfo.Content -> {
                if (holder is ContentTaskViewHolder) {
                    holder.bind(data = data)
                }
            }

            is TaskInfo.Link -> {
                if (holder is LinkTaskViewHolder) {
                    holder.bind(data = data)
                }
            }

            else -> Unit
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position]) {
            is TaskInfo.TaskCreate -> VIEW_TYPE_CREATE
            is TaskInfo.Content -> VIEW_TYPE_CONTENT
            is TaskInfo.Link -> VIEW_TYPE_LINK
            else -> VIEW_TYPE_UNKNOWN
        }
    }

    override fun getItemCount(): Int = currentList.size

    private class CreateTaskViewHolder(itemView: CreateTaskView) : RecyclerView.ViewHolder(itemView) {
        private var view: CreateTaskView = itemView

        fun bind(eventListener: TaskItemEventListener? = null) {
            view.setOnOptionSelectedListener { option ->
                eventListener?.onCreateContentSelected(option = option)
            }
        }
    }

    private class ContentTaskViewHolder(itemView: ContentTaskView) : RecyclerView.ViewHolder(itemView) {
        private var view: ContentTaskView = itemView
        private var eventListener: ((ItemEvent, TaskInfo.Content) -> Unit)? = null

        fun setEventListener(listener: (ItemEvent, TaskInfo.Content) -> Unit) {
            eventListener = listener

            view.let {
                it.setDeleteClickListener { data ->
                    eventListener?.invoke(ItemEvent.DELETE, data)
                }

                it.setImageUploadSuccessListener { data ->
                    eventListener?.invoke(ItemEvent.LOAD_IMAGE_SUCCESS, data)
                }

                it.setSelectedUpdateListener { data ->
                    eventListener?.invoke(ItemEvent.SELECT_CHANGE, data)
                }
            }
        }

        fun bind(data: TaskInfo.Content) {
            view.setData(data = data)
        }
    }

    private class LinkTaskViewHolder(itemView: LinkTaskView) : RecyclerView.ViewHolder(itemView) {
        private var view: LinkTaskView = itemView
        private var eventListener: ((ItemEvent, TaskInfo.Link) -> Unit)? = null

        fun setEventListener(listener: (ItemEvent, TaskInfo.Link) -> Unit) {
            eventListener = listener

            view.let {
                it.setDeleteClickListener { data ->
                    eventListener?.invoke(ItemEvent.DELETE, data)
                }

                it.setSelectedUpdateListener { data ->
                    eventListener?.invoke(ItemEvent.SELECT_CHANGE, data)
                }
            }
        }

        fun bind(data: TaskInfo.Link) {
            view.setData(data = data)
        }
    }

    companion object {
        private const val VIEW_TYPE_CREATE = 0
        private const val VIEW_TYPE_CONTENT = 1
        private const val VIEW_TYPE_LINK = 2
        private const val VIEW_TYPE_UNKNOWN = -1

        val diffCallback = object : DiffUtil.ItemCallback<TaskInfo>() {
            override fun areItemsTheSame(oldItem: TaskInfo, newItem: TaskInfo): Boolean {
                return when {
                    oldItem is TaskInfo.Content && newItem is TaskInfo.Content ->
                        oldItem.id == newItem.id

                    else -> oldItem::class == newItem::class
                }
            }

            override fun areContentsTheSame(oldItem: TaskInfo, newItem: TaskInfo): Boolean {
                return when {
                    oldItem is TaskInfo.Content && newItem is TaskInfo.Content -> {
                        oldItem.id == newItem.id &&
                                oldItem.isSelected == newItem.isSelected &&
                                oldItem.isEditable == newItem.isEditable &&
                                oldItem.isUploadImageSuccess == newItem.isUploadImageSuccess &&
                                oldItem.screenshotImgUrl == newItem.screenshotImgUrl
                    }

                    else -> oldItem == newItem
                }
            }
        }
    }
}