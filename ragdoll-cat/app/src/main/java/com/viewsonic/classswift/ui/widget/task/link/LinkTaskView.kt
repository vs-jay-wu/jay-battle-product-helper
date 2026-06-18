package com.viewsonic.classswift.ui.widget.task.link

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import coil.load
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.task.TaskInfo
import com.viewsonic.classswift.databinding.ViewLinkTaskBinding
import com.viewsonic.classswift.ui.widget.task.enums.DisplayMode
import timber.log.Timber

class LinkTaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var taskInfo: TaskInfo.Link? = null
    private var deletedListener: ((TaskInfo.Link) -> Unit)? = null
    private var selectedUpdateListener: ((TaskInfo.Link) -> Unit)? = null

    private val binding: ViewLinkTaskBinding = ViewLinkTaskBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    init {
        initClickAction()
    }

    fun setDeleteClickListener(listener: (TaskInfo.Link) -> Unit) {
        if (deletedListener == null) {
            deletedListener = listener
        }
    }

    fun setSelectedUpdateListener(listener: (TaskInfo.Link) -> Unit) {
        if (selectedUpdateListener == null) {
            selectedUpdateListener = listener
        }
    }

    fun setData(data: TaskInfo.Link) {
        Timber.d("[DATA] = ${data.toString()}")
        taskInfo = data

        //The image returned by the API is an HTTP URL.
        bindUrlTitle(title = data.title)
        bindDescription(description = data.description)
        bindUrlLink(url = data.url)
        bindThumbnail(imageUrl = data.imageUrl)
        bindUrl(data = data)
        updateCheckboxState(data = data)
        applyDisplayMode(if (data.isEditable) DisplayMode.EDIT else DisplayMode.NORMAL)
    }

    private fun initClickAction() {
        with(binding) {

            clRoot.setOnClickListener {
                val isSelected = cbSelect.isChecked
                cbSelect.isChecked = !isSelected
            }

            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                setUIStyleBySelectStatus(isSelected = isChecked)

                taskInfo?.let {
                    val updatedTask = it.copy(isSelected = isChecked)
                    selectedUpdateListener?.invoke(updatedTask)
                }
            }

            ivRemove.setOnClickListener {
                taskInfo?.let {
                    deletedListener?.invoke(it)
                }
            }
        }
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
        binding.tvUrl.apply {
            isVisible = url.isNotEmpty()
            text = url
        }
    }

    private fun bindUrl(data: TaskInfo.Link) {
        binding.tvLinkUrl.apply {
            text = addUnderLineForUrlString(url = data.url)
            isVisible = data.title.isEmpty() && data.description.isEmpty()
        }
    }

    private fun bindThumbnail(imageUrl: String) {
        binding.ivLinkThumbnail.apply {
            load(imageUrl) {
                placeholder(R.drawable.ic_url_preview_no_thumbnail)
                error(R.drawable.ic_url_preview_no_thumbnail)
                crossfade(true)
                allowHardware(false)
            }
        }
    }

    private fun updateCheckboxState(data: TaskInfo.Link) {
        with(binding.cbSelect) {
            if (isChecked != data.isSelected) {
                isChecked = data.isSelected
            }
        }
    }

    private fun applyDisplayMode(mode: DisplayMode) {
        with(binding) {
            when (mode) {
                DisplayMode.NORMAL -> {
                    ivRemove.visibility = View.VISIBLE
                    cbSelect.visibility = View.INVISIBLE
                    clRoot.setBackgroundResource(R.drawable.bg_neutral0_radius800_line_neutral450_border400)
                }

                DisplayMode.EDIT -> {
                    ivRemove.visibility = View.INVISIBLE
                    cbSelect.visibility = View.VISIBLE
                }

                DisplayMode.NONE -> {
                    ivRemove.visibility = View.INVISIBLE
                    cbSelect.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun setUIStyleBySelectStatus(isSelected: Boolean) {
        with(binding) {
            if (isSelected) {
                cbSelect.visibility = View.VISIBLE
                ivRemove.visibility = View.GONE
                clRoot.setBackgroundResource(R.drawable.bg_neutral0_radius800_brandblue_border400)
            } else {
                clRoot.setBackgroundResource(R.drawable.bg_neutral0_radius800_line_neutral450_border400)
            }
        }
    }

    private fun addUnderLineForUrlString(url: String): String {
        val spannable = SpannableString(url)
        spannable.setSpan(UnderlineSpan(), 0, url.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable.toString()
    }
}