package com.viewsonic.classswift.ui.widget.task.records

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.task.TaskResultInfo
import timber.log.Timber

class RecordGroupGridAdapter : ListAdapter<TaskResultInfo, RecyclerView.ViewHolder>(diffCallback) {

    private var currentTaskId: String = ""
    private var itemEventListener: GroupItemEventListener? = null

    interface GroupItemEventListener {
        fun onThumbnailClicked(data: TaskResultInfo.Content)
        fun onRecordDataSelectUpdate(data: TaskResultInfo)
        fun onAddPointClicked(data: TaskResultInfo)
        fun onRefetchRecordData(data: TaskResultInfo.ApiFail)
    }

    enum class ItemEvent {
        THUMBNAIL_CLICK,
        SELECT_CHANGE,
        ADD_POINT
    }

    fun updateAll(taskId: String, data: List<TaskResultInfo>) {
        currentTaskId = taskId
        submitList(data)
    }

    fun setRecordItemEventListener(listener: GroupItemEventListener) {
        itemEventListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position]) {
            is TaskResultInfo.Content -> VIEW_TYPE_CONTENT
            is TaskResultInfo.Link -> VIEW_TYPE_LINK
            is TaskResultInfo.Guest -> VIEW_TYPE_GUEST
            is TaskResultInfo.ApiFail -> VIEW_API_FAILED
            else -> VIEW_TYPE_UNKNOWN
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CONTENT -> {
                Timber.e("VIEW_TYPE_CONTENT")
                val itemView = ContentRecordView(parent.context)

                ContentRecordViewHolder(itemView = itemView).apply {
                    setEventListener(listener = { event, data ->
                        Timber.d("Adapter Event: $event")

                        when (event) {
                            ItemEvent.THUMBNAIL_CLICK -> {
                                itemEventListener?.onThumbnailClicked(data = data)
                            }

                            ItemEvent.SELECT_CHANGE -> {
                                itemEventListener?.onRecordDataSelectUpdate(data = data)
                            }

                            ItemEvent.ADD_POINT -> {
                                itemEventListener?.onAddPointClicked(data = data)
                            }
                        }
                    })
                }
            }

            VIEW_TYPE_LINK -> {
                val itemView = LinkRecordView(parent.context)
                LinkRecordViewHolder(itemView = itemView).apply {

                    setEventListener(listener = { event, data ->
                        Timber.d("Adapter Event: $event")

                        when (event) {
                            ItemEvent.ADD_POINT -> {
                                itemEventListener?.onAddPointClicked(data = data)
                            }

                            else -> Unit
                        }
                    })
                }
            }

            VIEW_TYPE_GUEST -> {
                Timber.e("VIEW_TYPE_GUEST")
                val itemView = GuestRecordView(parent.context)
                GuestRecordViewHolder(itemView = itemView)
            }

            VIEW_API_FAILED -> {
                Timber.e("VIEW_API_FAILED")
                val itemView = ApiFailedView(parent.context)
                ApiFailedViewHolder(itemView = itemView).apply {
                    setEventListener(listener = { data ->
                        itemEventListener?.onRefetchRecordData(data)
                    })
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
        val data = currentList[position]
        Timber.d("onBindViewHolder: $data")
        when (data) {

            is TaskResultInfo.Content -> {
                if (holder is ContentRecordViewHolder) {
                    holder.bind(data = data)
                }
            }

            is TaskResultInfo.Link -> {
                if (holder is LinkRecordViewHolder) {
                    holder.bind(data = data)
                }
            }

            is TaskResultInfo.Guest -> {
                if (holder is GuestRecordViewHolder) {
                    holder.bind(data = data)
                }
            }

            is TaskResultInfo.ApiFail -> {
                if (holder is ApiFailedViewHolder) {
                    holder.bind(data = data)
                }
            }

            else -> Unit
        }
    }

    override fun getItemCount(): Int = currentList.size

    private class ContentRecordViewHolder(itemView: ContentRecordView) : RecyclerView.ViewHolder(itemView) {
        private var view: ContentRecordView = itemView
        private var eventListener: ((ItemEvent, TaskResultInfo.Content) -> Unit)? = null

        fun bind(data: TaskResultInfo.Content) {
            view.bindData(data = data)
        }

        fun setEventListener(listener: (ItemEvent, TaskResultInfo.Content) -> Unit) {
            eventListener = listener

            view.let {
                it.setThumbnailClickListener { data ->
                    eventListener?.invoke(ItemEvent.THUMBNAIL_CLICK, data)
                }

                it.setSelectedUpdateListener { data ->
                    eventListener?.invoke(ItemEvent.SELECT_CHANGE, data)
                }

                it.setAddPointListener { data ->
                    eventListener?.invoke(ItemEvent.ADD_POINT, data)
                }
            }
        }
    }

    private class LinkRecordViewHolder(itemView: LinkRecordView) : RecyclerView.ViewHolder(itemView) {
        private var view: LinkRecordView = itemView
        private var eventListener: ((ItemEvent, TaskResultInfo.Link) -> Unit)? = null

        fun bind(data: TaskResultInfo.Link) {
            view.bindData(data = data)
        }

        fun setEventListener(listener: (ItemEvent, TaskResultInfo.Link) -> Unit) {
            eventListener = listener

            view.let {
                it.setAddPointListener { data ->
                    eventListener?.invoke(ItemEvent.ADD_POINT, data)
                }
            }
        }
    }

    private class GuestRecordViewHolder(itemView: GuestRecordView) : RecyclerView.ViewHolder(itemView) {
        private var view: GuestRecordView = itemView

        fun bind(data: TaskResultInfo.Guest) {
            view.bindData(data = data)
        }
    }

    private class ApiFailedViewHolder(itemView: ApiFailedView) : RecyclerView.ViewHolder(itemView) {
        private var view: ApiFailedView = itemView
        private var eventListener: ((TaskResultInfo.ApiFail) -> Unit)? = null

        fun bind(data: TaskResultInfo.ApiFail) {
            view.bindData(data = data)
        }

        fun setEventListener(listener: (TaskResultInfo.ApiFail) -> Unit) {
            eventListener = listener
            view.setRefetchResultListener { data ->
                eventListener?.invoke(data)
            }
        }
    }

    private class FallbackViewHolder(view: View) : RecyclerView.ViewHolder(view)

    companion object {

        private const val VIEW_TYPE_GUEST = 0
        private const val VIEW_TYPE_CONTENT = 1
        private const val VIEW_TYPE_LINK = 2
        private const val VIEW_API_FAILED = 3
        private const val VIEW_TYPE_UNKNOWN = 4

        val diffCallback = object : DiffUtil.ItemCallback<TaskResultInfo>() {

            override fun areItemsTheSame(oldItem: TaskResultInfo, newItem: TaskResultInfo): Boolean {
                if (oldItem::class != newItem::class) return false

                return when (oldItem) {
                    is TaskResultInfo.Content -> {
                        val newContent = newItem as? TaskResultInfo.Content ?: return false
                        oldItem.isSameItemAs(newContent)
                    }

                    is TaskResultInfo.Link -> {
                        val newLink = newItem as? TaskResultInfo.Link ?: return false
                        oldItem.isSameItemAs(newLink)
                    }

                    is TaskResultInfo.Guest -> {
                        val newGuest = newItem as? TaskResultInfo.Guest ?: return false
                        oldItem.seatNumber == newGuest.seatNumber
                    }

                    is TaskResultInfo.ApiFail -> {
                        val newGuest = newItem as? TaskResultInfo.ApiFail ?: return false
                        oldItem.serialNumber == newGuest.serialNumber
                    }
                }
            }

            override fun areContentsTheSame(oldItem: TaskResultInfo, newItem: TaskResultInfo): Boolean {
                if (oldItem::class != newItem::class) return false

                return when (oldItem) {
                    is TaskResultInfo.Content -> {
                        val newContent = newItem as? TaskResultInfo.Content ?: return false
                        oldItem.isContentSameAs(newContent)
                    }

                    is TaskResultInfo.Link -> {
                        val newLink = newItem as? TaskResultInfo.Link ?: return false
                        oldItem.isContentSameAs(newLink)
                    }

                    is TaskResultInfo.Guest -> {
                        val newGuest = newItem as? TaskResultInfo.Guest ?: return false
                        oldItem == newGuest
                    }

                    is TaskResultInfo.ApiFail -> {
                        val newGuest = newItem as? TaskResultInfo.ApiFail ?: return false
                        oldItem == newGuest
                    }
                }
            }
        }
    }
}