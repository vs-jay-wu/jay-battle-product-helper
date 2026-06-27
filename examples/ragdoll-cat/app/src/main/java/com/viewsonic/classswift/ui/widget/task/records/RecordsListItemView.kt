package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.records.RecordListInfo
import com.viewsonic.classswift.databinding.ItemRecordsListBinding

class RecordsListItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var onClickedListener: ((RecordListInfo.TaskItem) -> Unit)? = null
    private var taskInfo: RecordListInfo.TaskItem? = null

    private val binding: ItemRecordsListBinding = ItemRecordsListBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    init {
        initClickAction()
    }

    fun setData(data: RecordListInfo.TaskItem) {
        taskInfo = data
        setText(text = "${context.getString(R.string.push_and_respond_task)}${data.sequenceNumber}")
        setSelectedStyle(isSelected = data.isSelected)
    }

    fun setOnClickedListener(listener: (RecordListInfo.TaskItem) -> Unit) {
        if (onClickedListener == null) {
            onClickedListener = listener
        }
    }

    private fun setText(text: String) {
        binding.tvTitle.text = text
    }

    private fun initClickAction() {
        binding.clRootView.setOnClickListener {
            taskInfo?.let {
                onClickedListener?.invoke(it)
            }
        }
    }

    private fun setSelectedStyle(isSelected: Boolean) {
        val backgroundResId = if (isSelected) {
            R.drawable.bg_neutral200_radius400
        } else {
            android.R.color.transparent
        }

        val textColorResId = if (isSelected) {
            R.color.records_task_list_item_text_selected_color
        } else {
            R.color.records_task_list_item_text_normal_color
        }

        val textStyle = if (isSelected) {
            Typeface.BOLD
        } else {
            Typeface.NORMAL
        }

        with(binding) {
            clRootView.setBackgroundResource(backgroundResId)
            tvTitle.apply {
                setTextColor(ContextCompat.getColor(context, textColorResId))
                setTypeface(null, textStyle)
            }
            acivArrow.isVisible = isSelected
        }
    }
}