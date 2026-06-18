package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewRecordStatusBinding

class RecordStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val binding: ViewRecordStatusBinding = ViewRecordStatusBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    private val initStatusString = "-"

    init {
        setStatus(RecordStatus.INIT)
    }

    fun setStatus(status: RecordStatus) {
        binding.apply {
            when (status) {
                RecordStatus.INIT -> setGrayStyleUi(initStatusString)
                RecordStatus.LOADING -> setGrayStyleUi(initStatusString)
                RecordStatus.NOT_MARKED -> setNotMarkedUi()
                RecordStatus.MARKED -> setMarkedUi()
                RecordStatus.ABSENT -> setGrayStyleUi(context.getString(R.string.common_status_absent))
                RecordStatus.NOT_SUBMITTED -> setGrayStyleUi(context.getString(R.string.push_and_respond_status_submission_not_submitted))
            }
        }
    }

    private fun setGrayStyleUi(content: String) {
        binding.apply {
            tvStatus.text = content
            tvStatus.setTextColor(context.getColor(R.color.neutral_650))
            root.background = ContextCompat.getDrawable(context, R.color.neutral_200)
        }
    }

    private fun setNotMarkedUi() {
        binding.apply {
            tvStatus.text = context.getString(R.string.push_and_respond_status_submission_not_marked)
            tvStatus.setTextColor(context.getColor(R.color.red_600))
            root.background = ContextCompat.getDrawable(context, R.color.red_50)
        }
    }

    private fun setMarkedUi() {
        binding.apply {
            tvStatus.text = context.getString(R.string.push_and_respond_status_submission_marked)
            tvStatus.setTextColor(context.getColor(R.color.green_500))
            root.background = ContextCompat.getDrawable(context, R.color.green_50)
        }
    }

}

enum class RecordStatus {
    INIT,
    LOADING,
    NOT_MARKED,
    MARKED,
    ABSENT,
    NOT_SUBMITTED
}