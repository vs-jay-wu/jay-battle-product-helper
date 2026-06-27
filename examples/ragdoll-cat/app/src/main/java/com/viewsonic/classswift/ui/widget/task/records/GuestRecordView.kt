package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.databinding.ViewGuestRecordBinding

class GuestRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewGuestRecordBinding = ViewGuestRecordBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    fun bindData(data: TaskResultInfo.Guest) {
        applyGuestStyle()
        data.let {
            with(binding) {

                val seatNumber = it.seatNumber.ifEmpty { "-" }
                tvSeatNumber.text = seatNumber

                if (it.displayName.isNotEmpty()) {
                    tvName.text = it.displayName
                }
            }
            updateLabelMarkModeStyle(data = data)
        }
    }

    private fun updateLabelMarkModeStyle(data: TaskResultInfo.Guest) {
        with(binding) {
            val isEditMode = data.isEditable
            cbSelect.isVisible = isEditMode
            acbAddPoint.isVisible = !isEditMode
            viewMask.isVisible = isEditMode
        }
    }

    private fun applyGuestStyle() {
        with(binding) {

            tvName.apply {
                text = context.getString(R.string.common_guest)
                setTextColor(ContextCompat.getColor(context, R.color.records_name_disable_text_color))
            }

            clRoot.setBackgroundResource(R.drawable.bg_neutral0_radius800_line_neutral450_border400)
            tvSeatNumber.setBackgroundResource(R.drawable.bg_records_seat_number_disable)
            tvSubmitStatus.text = context.getString(R.string.common_status_absent)
            acbAddPoint.isEnabled = false
        }
    }
}