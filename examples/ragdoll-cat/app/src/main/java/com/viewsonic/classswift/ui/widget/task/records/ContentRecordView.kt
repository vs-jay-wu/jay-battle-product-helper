package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.databinding.ViewContentRecordBinding
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus
import com.viewsonic.classswift.utils.extension.setDebouncedClickListener
import timber.log.Timber

class ContentRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var recordInfo: TaskResultInfo.Content? = null
    private var thumbClickListener: ((TaskResultInfo.Content) -> Unit)? = null
    private var selectedUpdateListener: ((TaskResultInfo.Content) -> Unit)? = null
    private var addPointClickListener: ((TaskResultInfo.Content) -> Unit)? = null

    private val binding: ViewContentRecordBinding = ViewContentRecordBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    init {
        initClickAction()
    }

    fun setThumbnailClickListener(listener: ((TaskResultInfo.Content) -> Unit)) {
        if (thumbClickListener == null) {
            thumbClickListener = listener
        }
    }


    fun setSelectedUpdateListener(listener: (TaskResultInfo.Content) -> Unit) {
        if (selectedUpdateListener == null) {
            selectedUpdateListener = listener
        }
    }

    fun setAddPointListener(listener: (TaskResultInfo.Content) -> Unit) {
        if (addPointClickListener == null) {
            addPointClickListener = listener
        }
    }

    fun bindData(data: TaskResultInfo.Content) {
        Timber.d("bind data = $data")

        recordInfo = data
        with(binding) {
            data.let {
                applyStyleByStudentId(data.studentId)

                val seatNumber = it.seatNumber.ifEmpty { "-" }
                tvSeatNumber.text = seatNumber

                if (it.displayName.isNotEmpty()) {
                    tvName.text = it.displayName
                }
                if (it.isPushable) {
                    updatePushRecordStyle(data = it)
                } else {
                    updateLabelMarkModeStyle(data = it)
                }
                handleSubmitStatus(type = it.triggerType)
                handleImageLoad(url = it.imgUrl)
                handleSelectedStatus(isSelected = it.isSelected)
            }
        }
    }

    private fun handleSelectedStatus(isSelected: Boolean) {
        binding.cbSelect.let {
            if (it.isChecked != isSelected) {
                it.isChecked = isSelected
            }
        }
    }

    private fun updateLabelMarkModeStyle(data: TaskResultInfo.Content) {
        with(binding) {
            val isEditMode = data.isEditable
            val isMaskVisible = isEditMode && data.triggerType != SubmitStatus.RESPONSE

            cbSelect.isVisible = isEditMode
            acbAddPoint.isVisible = !isEditMode
            viewMask.isVisible = isMaskVisible

            iuvThumbnail.apply {
                isEnabled = !isEditMode
                isClickable = !isEditMode
            }

            clRoot.isEnabled = isEditMode && !isMaskVisible
            cbSelect.isEnabled = isEditMode && !isMaskVisible
        }
    }

    private fun updatePushRecordStyle(data: TaskResultInfo.Content) {
        with(binding) {
            val isEditMode = data.isEditable
            Timber.d("updatePushRecordStyle: $data")
            val isMaskVisible = isEditMode && data.triggerType !in listOf(
                SubmitStatus.RESPONSE,
                SubmitStatus.GRADED
            )
            Timber.d("updatePushRecordStyle isMaskVisible: $isMaskVisible")

            cbSelect.isVisible = isEditMode
            acbAddPoint.isVisible = !isEditMode
            viewMask.isVisible = isMaskVisible

            iuvThumbnail.apply {
                isEnabled = !isEditMode
                isClickable = !isEditMode
            }

            clRoot.isEnabled = isEditMode && !isMaskVisible
            cbSelect.isEnabled = isEditMode && !isMaskVisible
        }
    }

    private fun initClickAction() {
        with(binding) {
            acbAddPoint.apply {
                setDebouncedClickListener(RecordsConstants.UPDATE_POINT_DEBOUNCE) {
                    if (isEnabled) {
                        recordInfo?.let {
                            if (it.studentId.isNotEmpty()) {
                                addPointClickListener?.invoke(it)
                            }
                        }
                        showAddPointAnimation()
                    }
                }
            }

            iuvThumbnail.setDebouncedClickListener(RecordsConstants.THUMBNAIL_DEBOUNCE) {
                recordInfo?.let {
                    thumbClickListener?.invoke(it)
                }
            }

            clRoot.setOnClickListener {
                val isSelected = cbSelect.isChecked
                cbSelect.isChecked = !isSelected
            }

            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                setUIStyleBySelectStatus(isSelected = isChecked)

                recordInfo?.let {
                    val updatedTask = it.copy(isSelected = isChecked)
                    selectedUpdateListener?.invoke(updatedTask)
                }
            }
        }
    }

    private fun applyGuestStyle() {
        with(binding) {
            iuvThumbnail.isVisible = false

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

    private fun applyActiveStyle() {
        with(binding) {
            tvSeatNumber.setBackgroundResource(R.drawable.bg_records_seat_number_enable)
            tvName.setTextColor(ContextCompat.getColor(context, R.color.records_name_enable_text_color))
        }
    }

    private fun showAddPointAnimation() {
        with(binding.tvAddPointAnimator) {
            alpha = 1f
            translationY = 0f
            visibility = VISIBLE

            val floatDistance = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics
            )

            animate()
                .translationY(-floatDistance)
                .alpha(0f) // Fade out
                .setDuration(RecordsConstants.ANIMATION_DURATION)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    visibility = INVISIBLE
                }
                .start()
        }
    }

    private fun applyStyleByStudentId(studentId: String) {
        Timber.d("applyStyleByStudentId = ${studentId.isNotEmpty()}")

        if (studentId.isNotEmpty()) {
            applyActiveStyle()
        } else {
            applyGuestStyle()
        }
    }

    private fun handleImageLoad(url: String) {
        Timber.d("start load image: $url")
        binding.iuvThumbnail.let {
            it.setImage(uri = url)
            it.setCircleProgressbarVisibility(isShown = false)
            it.setMaskVisibility(isShown = false)
        }
    }

    private fun handleSubmitStatus(type: SubmitStatus) {

        with(binding.tvSubmitStatus) {
            when (type) {
                SubmitStatus.RESPONSE -> {
                    text = context.getString(R.string.push_and_respond_status_submission_not_marked)
                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.records_no_marked_text_color
                        )
                    )
                }

                SubmitStatus.UNSUBMITTED -> {
                    text = context.getString(R.string.push_and_respond_status_submission_not_submitted)
                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.records_not_submitted_text_color
                        )
                    )
                }

                SubmitStatus.GRADED -> {
                    text = context.getString(R.string.push_and_respond_status_submission_marked)
                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.records_marked_text_color
                        )
                    )
                }

                SubmitStatus.UNKNOWN -> {
                    text = context.getString(R.string.common_status_absent)
                }
            }
        }
    }

    private fun setUIStyleBySelectStatus(isSelected: Boolean) {
        with(binding) {
            if (isSelected) {
                cbSelect.visibility = VISIBLE
                clRoot.setBackgroundResource(R.drawable.bg_neutral0_radius800_brandblue_border400)
            } else {
                clRoot.setBackgroundResource(R.drawable.bg_neutral0_radius800_line_neutral450_border400)
            }
        }
    }
}

