package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ItemRecordsListHeaderBinding

class RecordsHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding: ItemRecordsListHeaderBinding = ItemRecordsListHeaderBinding.inflate(
        LayoutInflater.from(context),
        this,
    )

    fun setReceivingStyle() {
        binding.tvTitle.apply {
            text = context.getString(R.string.push_and_respond_task_status_receiving)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.records_receiving_header_text_color
                )
            )
        }
    }

    fun setTaskEndedStyle() {
        binding.tvTitle.apply {
            text = context.getString(R.string.push_and_respond_task_status_ended)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.records_task_ended_header_text_color
                )
            )
        }
    }
}