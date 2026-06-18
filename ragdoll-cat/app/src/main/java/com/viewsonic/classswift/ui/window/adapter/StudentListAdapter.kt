package com.viewsonic.classswift.ui.window.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.databinding.ItemStudentListBinding
import com.viewsonic.classswift.ui.window.viewholder.StudentListItemViewHolder
import timber.log.Timber

class StudentListAdapter(
    private val onItemInteractionListener: OnItemInteractionListener
) : ListAdapter<StudentInfo, RecyclerView.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return StudentListItemViewHolder(
            ItemStudentListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onItemInteractionListener
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as StudentListItemViewHolder).onBind(getItem(position))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            (holder as StudentListItemViewHolder).onBind(getItem(position))
        } else {
            val bundle = payloads[0] as Bundle
            if (bundle.containsKey("EDIT_MODE_CHANGED")) {
                (holder as StudentListItemViewHolder).updateEditMode(getItem(position).isEditing)
            } else if (bundle.containsKey("UPDATE_POINTS")) {
                (holder as StudentListItemViewHolder).updatePoint(getItem(position))
            }
        }
    }

    interface OnItemInteractionListener {
        fun onIncreasePoint(studentInfo: StudentInfo)
        fun onDecreasePoint(studentInfo: StudentInfo)
        fun onRemoveStudent(studentInfo: StudentInfo)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<StudentInfo>() {
            override fun areItemsTheSame(
                oldItem: StudentInfo,
                newItem: StudentInfo
            ): Boolean {
                Timber.d("areItemsTheSame: oldItem id:${oldItem.serialNumber}, newItem id:${oldItem.serialNumber}")
                Timber.d("areItemsTheSame: result:${oldItem.serialNumber == newItem.serialNumber}")
                return oldItem.serialNumber == newItem.serialNumber
            }

            override fun areContentsTheSame(
                oldItem: StudentInfo,
                newItem: StudentInfo
            ): Boolean {
                Timber.d("areContentsTheSame: item id:${oldItem.serialNumber}, result: ${oldItem == newItem}")
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: StudentInfo, newItem: StudentInfo): Any? {
                Timber.d("getChangePayload: item id:${oldItem.serialNumber}")
                val diff = Bundle()
                if (oldItem.getParticipationState() != newItem.getParticipationState()) {
                    return null
                }
                if (oldItem.isEditing != newItem.isEditing) {
                    diff.putBoolean("EDIT_MODE_CHANGED", true)
                    return diff
                }
                if (oldItem.points != newItem.points) {
                    diff.putBoolean("UPDATE_POINTS", true)
                    return diff
                }
                return null
            }
        }
    }
}