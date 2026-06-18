package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.databinding.ViewApiFailedRecordBinding
import kotlin.text.ifEmpty

class ApiFailedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var refetchResultClickListener: ((TaskResultInfo.ApiFail) -> Unit)? = null

    private val binding: ViewApiFailedRecordBinding = ViewApiFailedRecordBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    fun bindData(data: TaskResultInfo.ApiFail) {
        applyApiFailedStyle(data)
        with(binding) {
            tvSeatNumber.text = data.seatNumber.ifEmpty { "-" }
            if (data.displayName.isNotEmpty()) {
                tvName.text = data.displayName
            }
            cvRetry.setOnClickListener {
                refetchResultClickListener?.invoke(data)
            }
            cbSelect.isVisible = data.isEditable
        }
    }

    fun setRefetchResultListener(listener: (TaskResultInfo.ApiFail) -> Unit) {
        if (refetchResultClickListener == null) {
            refetchResultClickListener = listener
        }
    }

    private fun applyApiFailedStyle(data: TaskResultInfo.ApiFail) {
        with(binding) {
            tvName.apply {
                text = data.displayName.ifEmpty { "-" }
                setTextColor(ContextCompat.getColor(context, R.color.records_name_disable_text_color))
            }
            clRoot.setBackgroundResource(R.drawable.bg_neutral0_radius800_line_neutral450_border400)
            tvSeatNumber.setBackgroundResource(R.drawable.bg_records_seat_number_disable)
        }
    }
}