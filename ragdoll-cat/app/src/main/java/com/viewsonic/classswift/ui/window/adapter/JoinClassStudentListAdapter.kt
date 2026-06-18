package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.databinding.ItemJoinClassStudentBinding
import com.viewsonic.classswift.ui.window.viewholder.JoinClassStudentItemViewHolder

class JoinClassStudentListAdapter(
    private val onRemoveClick: (StudentInfo) -> Unit,
) : ListAdapter<StudentInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return JoinClassStudentItemViewHolder(
            ItemJoinClassStudentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            onRemoveClick
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as JoinClassStudentItemViewHolder).onBind(getItem(position))
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<StudentInfo>() {
            override fun areItemsTheSame(oldItem: StudentInfo, newItem: StudentInfo): Boolean =
                oldItem.studentId == newItem.studentId &&
                oldItem.serialNumber == newItem.serialNumber

            override fun areContentsTheSame(oldItem: StudentInfo, newItem: StudentInfo): Boolean =
                oldItem == newItem
        }
    }
}
