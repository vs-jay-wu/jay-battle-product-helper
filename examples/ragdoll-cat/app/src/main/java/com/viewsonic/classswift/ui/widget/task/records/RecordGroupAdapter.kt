package com.viewsonic.classswift.ui.widget.task.records

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.RecordGroupInfo
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.ui.widget.task.records.RecordGroupView.GroupItemEventListener
import timber.log.Timber
import kotlin.collections.mutableListOf

class RecordGroupAdapter : ListAdapter<RecordGroupInfo, RecyclerView.ViewHolder>(diffCallback) {

    private var currentTaskId: String = ""
    private var eventListener: RecordGroupItemEventListener? = null
    private val recordResultList = mutableListOf<TaskResultInfo>()


    interface RecordGroupItemEventListener {
        fun onGroupItemThumbnailClicked(data: TaskResultInfo)
        fun onGroupItemSelectUpdate(data: TaskResultInfo)
        fun onAddGroupMemberPointClicked(data: TaskResultInfo)
        fun onAddGroupPointClicked(data: RecordGroupInfo)
        fun onSubtractGroupPointClicked(data: RecordGroupInfo)
        fun onRefetchRecordData(data: TaskResultInfo)
    }

    enum class ItemEvent {
        THUMBNAIL_CLICK,
        SELECT_CHANGE,
        ADD_POINT,
        ADD_GROUP_POINT,
        SUBTRACT_GROUP_POINT,
        REFETCH_STUDENT_RECORD
    }

    fun clearAll() {
        submitList(emptyList<RecordGroupInfo>())
    }

    fun updateAll(taskId: String, data: List<TaskResultInfo>) {
        currentTaskId = taskId
        recordResultList.clear()
        recordResultList.addAll(data.toMutableList())
        val groupList = reorganizationByGroupId(taskId = taskId, data = data)
        submitList(groupList)
    }

    fun getRecordResultItemCount(): Int {
        return recordResultList.size
    }

    fun setRecordGroupItemEventListener(listener: RecordGroupItemEventListener) {
        eventListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView = RecordGroupView(parent.context)
        return GroupViewHolder(itemView = itemView).apply {
            setEventListener(listener = { event, data, group ->
                Timber.d("Adapter Event: ${event.toString()}")

                when (event) {
                    ItemEvent.THUMBNAIL_CLICK -> {
                        data?.let {
                            eventListener?.onGroupItemThumbnailClicked(data = it)
                        }
                    }

                    ItemEvent.SELECT_CHANGE -> {
                        data?.let {
                            eventListener?.onGroupItemSelectUpdate(data = it)
                        }
                    }

                    ItemEvent.ADD_POINT -> {
                        data?.let {
                            eventListener?.onAddGroupMemberPointClicked(data = it)
                        }
                    }

                    ItemEvent.ADD_GROUP_POINT -> {
                        group?.let {
                            eventListener?.onAddGroupPointClicked(data = it)
                        }

                    }

                    ItemEvent.SUBTRACT_GROUP_POINT -> {
                        group?.let {
                            eventListener?.onSubtractGroupPointClicked(data = it)
                        }
                    }

                    ItemEvent.REFETCH_STUDENT_RECORD -> {
                        data?.let {
                            eventListener?.onRefetchRecordData(data = it)
                        }
                    }
                }
            })
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = currentList[position]
        if (holder is GroupViewHolder) {
            holder.bind(data = data)
        }
    }

    override fun getItemCount(): Int = currentList.size

    private fun reorganizationByGroupId(taskId: String, data: List<TaskResultInfo>): List<RecordGroupInfo> {

        return data
            .groupBy { it.groupId }
            .map { (groupId, result) ->
                RecordGroupInfo(
                    groupId = groupId.toIntOrNull() ?: -1,
                    data = result,
                    taskId = taskId
                )
            }
            .sortedBy { it.groupId }
    }

    private class GroupViewHolder(itemView: RecordGroupView) : RecyclerView.ViewHolder(itemView) {
        private var eventListener: ((ItemEvent, TaskResultInfo?, RecordGroupInfo?) -> Unit)? = null

        private var view: RecordGroupView = itemView

        fun bind(data: RecordGroupInfo) {
            view.bindData(data = data)
        }

        fun setEventListener(listener: (ItemEvent, TaskResultInfo?, RecordGroupInfo?) -> Unit) {
            eventListener = listener

            view.setGroupItemEventListener(object : GroupItemEventListener {
                override fun onItemThumbnailClicked(
                    group: RecordGroupInfo,
                    data: TaskResultInfo
                ) {
                    eventListener?.invoke(ItemEvent.THUMBNAIL_CLICK, data, group)
                }

                override fun onRecordDataSelectUpdate(
                    group: RecordGroupInfo,
                    data: TaskResultInfo
                ) {
                    eventListener?.invoke(ItemEvent.SELECT_CHANGE, data, group)
                }

                override fun onAddPointClicked(
                    group: RecordGroupInfo,
                    data: TaskResultInfo
                ) {
                    eventListener?.invoke(ItemEvent.ADD_POINT, data, group)
                }

                override fun onAddGroupPointClicked(data: RecordGroupInfo) {
                    eventListener?.invoke(ItemEvent.ADD_GROUP_POINT, null, data)
                }

                override fun onSubtractGroupPointClicked(data: RecordGroupInfo) {
                    eventListener?.invoke(ItemEvent.SUBTRACT_GROUP_POINT, null, data)
                }

                override fun onRefetchStudentRecord(data: TaskResultInfo) {
                    eventListener?.invoke(ItemEvent.REFETCH_STUDENT_RECORD, data, null)
                }
            })
        }
    }

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<RecordGroupInfo>() {

            override fun areItemsTheSame(oldItem: RecordGroupInfo, newItem: RecordGroupInfo): Boolean {
                return oldItem.taskId == newItem.taskId && oldItem.groupId == newItem.groupId
            }

            override fun areContentsTheSame(oldItem: RecordGroupInfo, newItem: RecordGroupInfo): Boolean {
                if (oldItem.taskId != newItem.taskId || oldItem.groupId != newItem.groupId) return false

                val oldList = oldItem.data
                val newList = newItem.data

                if (oldList.size != newList.size) return false
                for (i in oldList.indices) {
                    if (!sameResultContent(oldList[i], newList[i])) return false
                }
                return true
            }

            override fun getChangePayload(oldItem: RecordGroupInfo, newItem: RecordGroupInfo): Any? {
                val oldList = oldItem.data
                val newList = newItem.data

                val changedIndices = mutableListOf<Int>()
                for (i in oldList.indices) {
                    if (!sameResultIdentity(oldList[i], newList[i]) ||
                        !sameResultContent(oldList[i], newList[i])
                    ) {
                        changedIndices += i
                    }
                }
                return if (changedIndices.isEmpty()) null else changedIndices
            }

            private fun sameResultIdentity(oldItem: TaskResultInfo, newItem: TaskResultInfo): Boolean {
                if (oldItem::class != newItem::class) return false
                return when (oldItem) {
                    is TaskResultInfo.Content -> oldItem.isSameItemAs(newItem as TaskResultInfo.Content)
                    is TaskResultInfo.Link -> oldItem.isSameItemAs(newItem as TaskResultInfo.Link)
                    is TaskResultInfo.Guest -> {
                        val newItem = newItem as TaskResultInfo.Guest
                        oldItem.taskId == newItem.taskId &&
                                oldItem.studentId == newItem.studentId &&
                                oldItem.serialNumber == newItem.serialNumber &&
                                oldItem.groupId == newItem.groupId
                    }
                    is TaskResultInfo.ApiFail -> oldItem.isSameItemAs(newItem as TaskResultInfo.ApiFail)
                }
            }

            private fun sameResultContent(oldItem: TaskResultInfo, newItem: TaskResultInfo): Boolean {
                if (oldItem::class != newItem::class) return false
                return when (oldItem) {
                    is TaskResultInfo.Content -> oldItem.isContentSameAs(newItem as TaskResultInfo.Content)
                    is TaskResultInfo.Link -> oldItem.isContentSameAs(newItem as TaskResultInfo.Link)
                    is TaskResultInfo.Guest -> {
                        val newItem = newItem as TaskResultInfo.Guest
                        oldItem.taskId == newItem.taskId &&
                                oldItem.displayName == newItem.displayName &&
                                oldItem.studentId == newItem.studentId &&
                                oldItem.seatNumber == newItem.seatNumber &&
                                oldItem.serialNumber == newItem.serialNumber &&
                                oldItem.groupId == newItem.groupId
                    }
                    is TaskResultInfo.ApiFail -> oldItem.isSameItemAs(newItem as TaskResultInfo.ApiFail)
                }
            }
        }
    }
}