package com.viewsonic.classswift.ui.window.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.StudentGroupInfo
import com.viewsonic.classswift.databinding.ItemStudentGroupBinding
import com.viewsonic.classswift.ui.window.adapter.StudentListAdapter.OnItemInteractionListener
import com.viewsonic.classswift.ui.window.viewholder.StudentGroupListItemViewHolder

class StudentGroupListAdapter(private val onItemInteractionListener: OnItemInteractionListener,
    private val onGroupItemInteractionListener: OnGroupItemInteractionListener) :
    ListAdapter<StudentGroupInfo, RecyclerView.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return StudentGroupListItemViewHolder(
            ItemStudentGroupBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), onItemInteractionListener, onGroupItemInteractionListener
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as StudentGroupListItemViewHolder).onBind(getItem(position))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            (holder as StudentGroupListItemViewHolder).onBind(getItem(position))
        } else {
            val bundle = payloads[0] as Bundle
            if (bundle.containsKey("STUDENT_LIST_CHANGE")) {
                (holder as StudentGroupListItemViewHolder).updateStudentList(getItem(position))
            }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<StudentGroupInfo>() {
            override fun areItemsTheSame(oldItem: StudentGroupInfo, newItem: StudentGroupInfo): Boolean {
                return oldItem.groupId == newItem.groupId
            }

            override fun areContentsTheSame(oldItem: StudentGroupInfo, newItem: StudentGroupInfo): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: StudentGroupInfo, newItem: StudentGroupInfo): Any? {
                val diff = Bundle()
                if (oldItem.studentList != newItem.studentList) {
                    diff.putBoolean("STUDENT_LIST_CHANGE", true)
                }
                return if (diff.isEmpty) null else diff
            }
        }
    }

    interface OnGroupItemInteractionListener {
        fun onIncreaseGroupPoint(groupInfo: StudentGroupInfo)
        fun onDecreaseGroupPoint(groupInfo: StudentGroupInfo)
    }
}