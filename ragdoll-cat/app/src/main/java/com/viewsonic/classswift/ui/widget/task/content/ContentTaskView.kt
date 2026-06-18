package com.viewsonic.classswift.ui.widget.task.content

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.task.TaskInfo
import com.viewsonic.classswift.databinding.ViewContentTaskBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.coordinator.UploadFileHandler
import com.viewsonic.classswift.ui.widget.quiz.ImageUploadView.ReUploadImageListener
import com.viewsonic.classswift.ui.widget.task.enums.DisplayMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class ContentTaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var deletedListener: ((TaskInfo.Content) -> Unit)? = null
    private var imageUploadSuccessListener: ((TaskInfo.Content) -> Unit)? = null
    private var selectedUpdateListener: ((TaskInfo.Content) -> Unit)? = null

    private var taskInfo: TaskInfo.Content? = null
    private val binding: ViewContentTaskBinding = ViewContentTaskBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    //For image upload
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val uploadManager: UploadFileHandler by inject(UploadFileHandler::class.java)

    init {
        initImageUploadListener()
        observeUploadFileStatus()
        initClickAction()
    }

    private fun initClickAction() {
        with(binding) {
            clRoot.setOnClickListener {
                taskInfo?.let {
                    if(it.isUploadImageSuccess) {
                        val isSelected = cbSelect.isChecked
                        cbSelect.isChecked = !isSelected
                    }
                }
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

    private fun initImageUploadListener() {
        val listener: ReUploadImageListener = object : ReUploadImageListener {
            override fun onUploadImage() {
                Timber.d("ContentTaskView onRetryUploadImage...")
                binding.csScreenshotImage.let {
                    it.setUploadFailedContainerVisibility(isShown = false)
                    it.setCircleProgressbarVisibility(isShown = true)
                    it.setMaskVisibility(isShown = true)
                    it.startProgressAnimation()
                }
                taskInfo?.let {
                    startUploadImageToAwsS3(data = it)
                }
            }
        }
        binding.csScreenshotImage.setOnUploadImageListener(listener)
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

    fun setDeleteClickListener(listener: (TaskInfo.Content) -> Unit) {
        if (deletedListener == null) {
            deletedListener = listener
        }
    }

    fun setImageUploadSuccessListener(listener: (TaskInfo.Content) -> Unit) {
        if (imageUploadSuccessListener == null) {
            imageUploadSuccessListener = listener
        }
    }

    fun setSelectedUpdateListener(listener: (TaskInfo.Content) -> Unit) {
        if (selectedUpdateListener == null) {
            selectedUpdateListener = listener
        }
    }

    fun setData(data: TaskInfo.Content) {
        Timber.d("[DATA] = ${data.toString()}")
        taskInfo = data
        updateScreenshotImage(data)
        updateUploadState(data)
        updateCheckboxState(data)
        applyDisplayMode(if (data.isEditable) DisplayMode.EDIT else DisplayMode.NORMAL)
    }

    private fun updateScreenshotImage(data: TaskInfo.Content) {
        binding.csScreenshotImage.setImage(uri = data.screenshotImgUrl)
    }

    private fun updateUploadState(data: TaskInfo.Content) {
        binding.csScreenshotImage.let {
            if (!data.isUploadImageSuccess) {
                it.setCircleProgressbarVisibility(isShown = true)
                it.setUploadFailedContainerVisibility(isShown = false)
                it.setMaskVisibility(isShown = true)
                startUploadImageToAwsS3(data)
            } else {
                it.setCircleProgressbarVisibility(isShown = false)
                it.setUploadFailedContainerVisibility(isShown = false)
                it.setMaskVisibility(isShown = false)
            }
        }
    }

    private fun updateCheckboxState(data: TaskInfo.Content) {
        with(binding.cbSelect) {
            if (isChecked != data.isSelected) {
                isChecked = data.isSelected
            }
        }
    }

    private fun setUpdateFailUIStyle() {
        with(binding) {
            ivRemove.isVisible = true
            csScreenshotImage.apply {
                setCircleProgressbarVisibility(isShown = false)
                resetProgress()
                setUploadFailedContainerVisibility(isShown = true)
            }
            cbSelect.isChecked = false
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

    private fun startUploadImageToAwsS3(data: TaskInfo.Content) {
        uploadManager.fetchPreSignedUrl(
            lessonId = data.lessonId,
            imageUrl = data.screenshotImgUrl
        )
    }

    private fun observeUploadFileStatus() {
        coroutineScope.launch(Dispatchers.Main) {
            uploadManager.uploadImageSharedFlow.collect { isSuccess ->

                when (isSuccess) {
                    true -> {
                        with(binding.csScreenshotImage) {
                            startProgressAnimation(fromPercentage = 90, toPercentage = 100)
                            setMaskVisibility(false)
                            setCircleProgressbarVisibility(false)
                            resetProgress()
                            //Notify adapter data update
                            taskInfo?.let {
                                val updatedTask = it.copy(
                                    isSelected = true,
                                    isEditable = true,
                                    isUploadImageSuccess = true,
                                    imagePreSignUrl = uploadManager.awsPreSignedUrl.s3GetUrl
                                )
                                imageUploadSuccessListener?.invoke(updatedTask)
                            }
                        }
                    }

                    false -> {
                        setUpdateFailUIStyle()
                    }
                }
            }
        }
    }
}