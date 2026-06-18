package com.viewsonic.classswift.ui.window.viewholder

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.databinding.ItemJoinClassStudentBinding
import com.viewsonic.classswift.utils.AvatarPicker

class JoinClassStudentItemViewHolder(
    private val binding: ItemJoinClassStudentBinding,
    private val onRemoveClick: (StudentInfo) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind(studentInfo: StudentInfo) {
        when (studentInfo.getParticipationState()) {
            StudentInfo.ParticipationState.JOINED -> bindJoined(studentInfo)
            StudentInfo.ParticipationState.NOT_JOINED -> bindNotJoined(studentInfo)
            StudentInfo.ParticipationState.JOINING -> bindJoining(studentInfo)
        }
    }

    private fun bindJoined(studentInfo: StudentInfo) {
        with(binding) {
            ivAvatar.setImageResource(AvatarPicker.pick(studentInfo))
            ivAvatar.alpha = 1f
            tvName.text = studentInfo.getActualDisplayName(root.context)
            tvName.setTextColor(ContextCompat.getColor(root.context, R.color.neutral_900))
            ivRemove.isVisible = true
            ivRemove.setOnClickListener { onRemoveClick(studentInfo) }
        }
    }

    private fun bindNotJoined(studentInfo: StudentInfo) {
        with(binding) {
            ivAvatar.setImageResource(R.drawable.ic_avatar_student_not_joined)
            ivAvatar.alpha = 1f
            tvName.text = studentInfo.getActualDisplayName(root.context)
            tvName.setTextColor(ContextCompat.getColor(root.context, R.color.neutral_400))
            ivRemove.isVisible = false
        }
    }

    private fun bindJoining(studentInfo: StudentInfo) {
        with(binding) {
            ivAvatar.setImageResource(R.drawable.ic_avatar_student_joining)
            ivAvatar.alpha = 1f
            tvName.text = root.context.getString(R.string.join_class_student_joining)
            tvName.setTextColor(ContextCompat.getColor(root.context, R.color.neutral_600))
            ivRemove.isVisible = false
        }
    }
}
