package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import coil.load
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewMvbImageUploadBinding
import com.viewsonic.classswift.manager.CoroutineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MvbImageUploadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val coroutineScope = CoroutineManager.getScope(this)
    private var progressJob: Job? = null

    private var binding: ViewMvbImageUploadBinding =
        ViewMvbImageUploadBinding.inflate(LayoutInflater.from(context), this)
    private var listener: Listener? = null

    init {
        clipToOutline = true
        background = ResourcesCompat.getDrawable(resources, R.drawable.bg_mvb_neutral0_radius400_line_neutral300_border100, null)
        startProgressAnimation()
        binding.cslbTryAgain.setOnClickListener {
            listener?.onTryAgainButtonClicked()
        }
        binding.buttonCaptureAgain.setOnClickListener {
            listener?.onCaptureAgainButtonClicked()
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setImage(uri: String) {
        Timber.d("[setImage] uri - $uri")
        if (uri.startsWith(HTTPS_PREFIX)) {
            // Coil loads image from URL
            binding.ivScreenshotImage.load(uri) {
                // need to add this below, otherwise it will throw an IllegalArgumentException:
                // "Software rendering doesn't support hardware bitmaps",
                // reference to https://coil-kt.github.io/coil/recipes/
                allowHardware(false)
            }
        } else {
            binding.ivScreenshotImage.setImageBitmap(BitmapFactory.decodeFile(uri))
        }
    }

    fun setMaskVisibility(isShown: Boolean) {
        binding.ivMask.visibility = if (isShown) VISIBLE else GONE
    }

    fun setProgressbarVisibility(isShown: Boolean) {
        binding.llProgressBarContainer.visibility = if (isShown) VISIBLE else GONE
    }

    fun setUploadFailedContainerVisibility(isShown: Boolean) {
        binding.clUploadFailedContainer.visibility = if (isShown) VISIBLE else GONE
    }

    fun setCaptureAgainButtonEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            binding.buttonCaptureAgain.setEnable()
        } else {
            binding.buttonCaptureAgain.setDisable()
        }
    }

    fun setCaptureAgainButtonVisibility(isVisible: Boolean) {
        binding.buttonCaptureAgain.visibility = if (isVisible) VISIBLE else GONE
    }

    fun resetProgress() {
        binding.lpiProgressBar.setProgress(0)
    }

    fun startProgressAnimation(fromPercentage: Int = 0, toPercentage: Int = 90) {
        Timber.d("[startProgressAnimation] fromPercentage-$fromPercentage, toPercentage-$toPercentage")
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.IO) {
            for (percentage in fromPercentage..toPercentage) {
                withContext(Dispatchers.Main) {
                    binding.lpiProgressBar.setProgress(percentage)
                    binding.tvProgress.text = binding.root.context.getString(R.string.mvb_question_edit_preparing_question_progress, percentage)
                }
                delay(100L)
            }
        }
    }

    interface Listener {
        fun onTryAgainButtonClicked()
        fun onCaptureAgainButtonClicked()
    }

    companion object {
        //TODO HTTPS_PREFIX  move to app constants object
        private const val HTTPS_PREFIX = "https://"
    }
}
