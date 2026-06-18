package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import coil.load
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.task.LinkMetaInfo
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.databinding.ViewLinkRecordBinding
import com.viewsonic.classswift.utils.extension.setDebouncedClickListener
import timber.log.Timber

class LinkRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var recordInfo: TaskResultInfo.Link? = null
    private var addPointClickListener: ((TaskResultInfo.Link) -> Unit)? = null

    private val binding: ViewLinkRecordBinding = ViewLinkRecordBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    init {
        initClickAction()
    }

    fun setAddPointListener(listener: (TaskResultInfo.Link) -> Unit) {
        if (addPointClickListener == null) {
            addPointClickListener = listener
        }
    }

    fun bindData(data: TaskResultInfo.Link) {
        Timber.d("bind Link data = $data")
        with(binding) {
            data.let {
                recordInfo = it

                val seatNumber = if (it.seatNumber.isEmpty()) {
                    "-"
                } else {
                    it.seatNumber
                }
                tvSeatNumber.text = seatNumber.toString()

                tvUrl.text = it.linkUrl
                if (it.displayName.isNotEmpty()) {
                    tvName.text = it.displayName
                }

                handleLinkOpenStatus(isLinkOpen = it.linkIsOpened)
                //url preview display
                bindMetaData(data.linkMeta)
            }
        }
    }

    private fun initClickAction() {
        binding.acbAddPoint.apply {
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
    }

    private fun handleLinkOpenStatus(isLinkOpen: Boolean) {

        with(binding.tvOpenLinkStatus) {
            if (isLinkOpen) {
                text = context.getString(R.string.push_and_respond_status_link_opened)
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.records_link_opened_text_color
                    )
                )
            } else {
                text = context.getString(R.string.push_and_respond_status_link_not_opened)
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.records_link_not_opened_text_color
                    )
                )
            }
        }
    }

    private fun bindMetaData(data: LinkMetaInfo) {
        bindUrlTitle(title = data.title)
        bindDescription(description = data.description)
        bindUrlLink(url = data.siteName)
        bindThumbnail(imageUrl = data.image)
    }

    private fun bindUrlTitle(title: String) {
        binding.tvLinkTitle.apply {
            isVisible = title.isNotEmpty()
            text = title
        }
    }

    private fun bindDescription(description: String) {
        binding.tvLinkDescription.apply {
            isVisible = description.isNotEmpty()
            text = description
        }
    }

    private fun bindUrlLink(url: String) {
        binding.tvLinkUrl.apply {
            isVisible = url.isNotEmpty()
            text = url
        }
    }

    private fun bindThumbnail(imageUrl: String) {
        binding.ivLinkThumbnail.apply {
            load(imageUrl) {
                placeholder(R.drawable.ic_url_preview_no_thumbnail)
                error(R.drawable.ic_url_preview_no_thumbnail)
                crossfade(true)
                allowHardware(false)
                listener(
                    onError = { _, throwable ->
                        setImageResource(R.drawable.ic_url_preview_no_thumbnail)
                        Timber.e("load url image error: ${throwable.toString()}")
                    }
                )
            }
        }
    }

    private fun showAddPointAnimation() {
        with(binding.tvAddPointAnimator) {
            alpha = 1f
            translationY = 0f
            visibility = View.VISIBLE

            val floatDistance = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics
            )

            animate()
                .translationY(-floatDistance)
                .alpha(0f) // Fade out
                .setDuration(RecordsConstants.ANIMATION_DURATION)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    visibility = View.INVISIBLE
                }
                .start()
        }
    }
}