package com.viewsonic.classswift.ui.window.viewholder

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.StudentGroupInfo
import com.viewsonic.classswift.databinding.ItemStudentGroupBinding
import com.viewsonic.classswift.ui.window.adapter.StudentGroupListAdapter
import com.viewsonic.classswift.ui.window.adapter.StudentListAdapter
import timber.log.Timber

class StudentGroupListItemViewHolder(
    private val binding: ItemStudentGroupBinding,
    private val onItemInteractionListener: StudentListAdapter.OnItemInteractionListener,
    private val onGroupItemInteractionListener: StudentGroupListAdapter.OnGroupItemInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    private var studentListAdapter: StudentListAdapter = StudentListAdapter(onItemInteractionListener)
    private val pointDisableBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.color_C3C7C7))
    private val increaseEnableBackgroundColor = ContextCompat.getColorStateList(binding.root.context, R.color.selector_student_list_increase_point)
    private val decreaseEnableBackgroundColor = ContextCompat.getColorStateList(binding.root.context, R.color.selector_student_list_decrease_point)
    private lateinit var groupInfo: StudentGroupInfo

    @SuppressLint("SetTextI18n")
    fun onBind(info: StudentGroupInfo) {
        groupInfo = info
        Timber.d("onBind: groupInfo list:${groupInfo.studentList}")
        with(binding) {
            tvGroupNo.text = groupInfo.groupId.toString()
            rvStudentList.apply {
                studentListAdapter = StudentListAdapter(onItemInteractionListener)
                layoutManager = GridLayoutManager(root.context, 4)
                adapter = studentListAdapter
                studentListAdapter.submitList(groupInfo.studentList)
            }
            setPointButtonRelativeUI(groupInfo.canUpdatePoints())
            tvGroupIncreasePoint.setOnClickListener {
                onGroupItemInteractionListener.onIncreaseGroupPoint(groupInfo)
            }
            tvGroupDecreasePoint.setOnClickListener {
                onGroupItemInteractionListener.onDecreaseGroupPoint(groupInfo)
            }
        }
    }

    fun updateStudentList(info: StudentGroupInfo) {
        Timber.d("updateStudentList: groupInfo list:${info.studentList}")
        // update group info for DecreaseGroupPoint and onIncreaseGroupPoint
        groupInfo = info
        studentListAdapter.submitList(groupInfo.studentList)
        setPointButtonRelativeUI(groupInfo.canUpdatePoints())
    }

    private fun setPointButtonRelativeUI(enable: Boolean) {
        with(binding) {
            tvGroupIncreasePoint.isClickable = enable
            tvGroupDecreasePoint.isClickable = enable
            if (enable) {
                tvGroupIncreasePoint.backgroundTintList = increaseEnableBackgroundColor
                tvGroupDecreasePoint.backgroundTintList = decreaseEnableBackgroundColor
            } else {
                tvGroupIncreasePoint.backgroundTintList = pointDisableBackgroundColor
                tvGroupDecreasePoint.backgroundTintList = pointDisableBackgroundColor
            }
        }
    }
}