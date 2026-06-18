package com.viewsonic.classswift.ui.window.viewholder

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.databinding.ItemStudentListBinding
import com.viewsonic.classswift.ui.window.adapter.StudentListAdapter

class StudentListItemViewHolder(
    private val binding: ItemStudentListBinding,
    private val onItemInteractionListener: StudentListAdapter.OnItemInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun onBind(studentInfo: StudentInfo) {
        with(binding) {
            when (studentInfo.getParticipationState()) {
                StudentInfo.ParticipationState.NOT_JOINED -> {
                    val strokeLineColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    val increaseBackgroundColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    val decreaseBackgroundColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    val removeImageColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    val numberBackgroundColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    val numberBackgroundDrawable = ContextCompat.getDrawable(root.context, R.drawable.shape_student_number)?.apply {
                        setTint(numberBackgroundColor)
                    }
                    val numberTextColor = Color.WHITE
                    val nameTextColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    val scoreTextColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(strokeLineColor))
                    viewTopDivider.setBackgroundColor(strokeLineColor)
                    viewBottomDivider.setBackgroundColor(strokeLineColor)
                    numberBackgroundDrawable?.let { backgroundDrawable ->
                        tvNumber.setBackgroundDrawable(backgroundDrawable)
                    } ?: run {
                        tvNumber.setBackgroundColor(numberBackgroundColor)
                    }
                    tvIncreasePoint.backgroundTintList = ColorStateList.valueOf(increaseBackgroundColor)
                    tvDecreasePoint.backgroundTintList = ColorStateList.valueOf(decreaseBackgroundColor)
                    ivRemove.imageTintList = ColorStateList.valueOf(removeImageColor)
                    tvNumber.setTextColor(numberTextColor)
                    tvName.setTextColor(nameTextColor)
                    tvScore.setTextColor(scoreTextColor)

                    tvIncreasePoint.isClickable = false
                    tvDecreasePoint.isClickable = false
                    ivRemove.isClickable = false
                    tvNumber.text = studentInfo.getActualDisplaySeatNumber()
                    tvName.text = studentInfo.getActualDisplayName(root.context)
                }

                StudentInfo.ParticipationState.JOINING -> {
                    val strokeLineColor = ContextCompat.getColor(root.context, R.color.color_0A8CF0)
                    val increaseBackgroundColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    val decreaseBackgroundColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    val removeImageColor = ContextCompat.getColor(root.context, R.color.color_F02B2B)
                    val numberBackgroundColor = Color.TRANSPARENT
                    val numberBackgroundDrawable = ContextCompat.getDrawable(root.context, R.drawable.shape_student_number)?.apply {
                        setTint(numberBackgroundColor)
                    }
                    val numberTextColor = ContextCompat.getColor(root.context, R.color.color_0A8CF0)
                    val nameTextColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    val scoreTextColor = ContextCompat.getColor(root.context, R.color.color_C3C7C7)
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(strokeLineColor))
                    viewTopDivider.setBackgroundColor(strokeLineColor)
                    viewBottomDivider.setBackgroundColor(strokeLineColor)
                    numberBackgroundDrawable?.let { backgroundDrawable ->
                        tvNumber.setBackgroundDrawable(backgroundDrawable)
                    } ?: run {
                        tvNumber.setBackgroundColor(numberBackgroundColor)
                    }
                    tvNumber.setBackgroundDrawable(numberBackgroundDrawable)
                    tvIncreasePoint.backgroundTintList = ColorStateList.valueOf(increaseBackgroundColor)
                    tvDecreasePoint.backgroundTintList = ColorStateList.valueOf(decreaseBackgroundColor)
                    ivRemove.imageTintList = ColorStateList.valueOf(removeImageColor)
                    tvNumber.setTextColor(numberTextColor)
                    tvName.setTextColor(nameTextColor)
                    tvScore.setTextColor(scoreTextColor)
                    tvIncreasePoint.isClickable = false
                    tvDecreasePoint.isClickable = false
                    ivRemove.setOnClickListener {
                        onItemInteractionListener.onRemoveStudent(studentInfo)
                    }
                    tvNumber.text = studentInfo.getActualDisplaySeatNumber()
                    tvName.text = root.context.getString(R.string.student_list_join_status_typing)
                }

                StudentInfo.ParticipationState.JOINED -> {
                    val strokeLineColor = ContextCompat.getColor(root.context, R.color.color_0A8CF0)
                    val increaseBackgroundColor = ContextCompat.getColorStateList(root.context, R.color.selector_student_list_increase_point)
                    val removeImageColor = ContextCompat.getColor(root.context, R.color.color_F02B2B)
                    val numberBackgroundColor = ContextCompat.getColor(root.context, R.color.color_0A8CF0)
                    val numberBackgroundDrawable = ContextCompat.getDrawable(root.context, R.drawable.shape_student_number)?.apply {
                        setTint(numberBackgroundColor)
                    }
                    val numberTextColor = Color.WHITE
                    val nameTextColor = ContextCompat.getColor(root.context, R.color.color_2E3133)
                    val scoreTextColor = ContextCompat.getColor(root.context, R.color.color_2E3133)
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(strokeLineColor))
                    viewTopDivider.setBackgroundColor(strokeLineColor)
                    viewBottomDivider.setBackgroundColor(strokeLineColor)
                    numberBackgroundDrawable?.let { backgroundDrawable ->
                        tvNumber.setBackgroundDrawable(backgroundDrawable)
                    } ?: run {
                        tvNumber.setBackgroundColor(numberBackgroundColor)
                    }
                    tvIncreasePoint.backgroundTintList = increaseBackgroundColor
                    setJoinedDecreasePointBtn(studentInfo)
                    ivRemove.imageTintList = ColorStateList.valueOf(removeImageColor)
                    tvNumber.setTextColor(numberTextColor)
                    tvName.setTextColor(nameTextColor)
                    tvScore.setTextColor(scoreTextColor)

                    tvIncreasePoint.setOnClickListener {
                        onItemInteractionListener.onIncreasePoint(studentInfo)
                    }
                    tvDecreasePoint.setOnClickListener {
                        onItemInteractionListener.onDecreasePoint(studentInfo)
                    }
                    ivRemove.setOnClickListener {
                        onItemInteractionListener.onRemoveStudent(studentInfo)
                    }
                    tvNumber.text = studentInfo.getActualDisplaySeatNumber()
                    tvName.text = studentInfo.getActualDisplayName(root.context)
                }
            }
            tvScore.text = studentInfo.points.toString()
            if (studentInfo.isEditing) {
                llScoreContainer.visibility = View.INVISIBLE
                ivRemove.visibility = View.VISIBLE
            } else {
                llScoreContainer.visibility = View.VISIBLE
                ivRemove.visibility = View.INVISIBLE
            }
        }
    }

    // for switch edit mode
    fun updateEditMode(isEditing: Boolean) {
        with(binding) {
            if (isEditing) {
                llScoreContainer.visibility = View.INVISIBLE
                ivRemove.visibility = View.VISIBLE
            } else {
                llScoreContainer.visibility = View.VISIBLE
                ivRemove.visibility = View.INVISIBLE
            }
        }
    }

    private fun setJoinedDecreasePointBtn(info: StudentInfo) {
        binding.apply {
            val decreaseBackgroundColor = ContextCompat.getColorStateList(root.context, R.color.selector_student_list_decrease_point)
            val decreaseDisableBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(root.context, R.color.color_C3C7C7))
            val enableDecreaseBtn = info.canDecreasePoints()
            tvDecreasePoint.isClickable = enableDecreaseBtn
            tvDecreasePoint.backgroundTintList = if (enableDecreaseBtn) decreaseBackgroundColor else decreaseDisableBackgroundColor
        }
    }

    // for update student points
    @SuppressLint("SetTextI18n")
    fun updatePoint(info: StudentInfo) {
        binding.tvScore.text = info.points.toString()
        setJoinedDecreasePointBtn(info)
    }
}