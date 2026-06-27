package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import coil.load
import com.viewsonic.classswift.databinding.ViewImageUploadBinding
import com.viewsonic.classswift.manager.CoroutineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ImageUploadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val coroutineScope = CoroutineManager.getScope(this)
    private var progressJob: Job? = null

    private var binding: ViewImageUploadBinding =
        ViewImageUploadBinding.inflate(LayoutInflater.from(context), this)
    private var reUploadImageListener: ReUploadImageListener? = null

    fun setOnUploadImageListener(listener: ReUploadImageListener) {
        reUploadImageListener = listener
    }

    init {
        startProgressAnimation()
        binding.buttonUploadAgain.setOnClickListener {
            uploadImageToS3()
        }
    }

    /**
     * 設定 image
     */
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
        binding.csMaskView.visibility = if (isShown) View.VISIBLE else View.GONE
    }

    fun setCircleProgressbarVisibility(isShown: Boolean) {
        binding.csCircleProgressBar.visibility = if (isShown) View.VISIBLE else View.GONE
    }

    fun setUploadFailedContainerVisibility(isShown: Boolean) {
        binding.clUploadFailedContainer.visibility = if (isShown) View.VISIBLE else View.GONE
    }

    fun resetProgress() {
        binding.csCircleProgressBar.setProgress(0)
    }

    private fun uploadImageToS3() {
        reUploadImageListener?.onUploadImage()
    }

    /**
     * 更新 progressbar status
     */
    fun startProgressAnimation(fromPercentage: Int = 0, toPercentage: Int = 90) {
        Timber.d("[startProgressAnimation] fromPercentage-$fromPercentage, toPercentage-$toPercentage")
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.IO) {
            for (percentage in fromPercentage..toPercentage) {
                withContext(Dispatchers.Main) {
                    binding.csCircleProgressBar.setProgress(percentage)
                }
                delay(1L)
            }
        }
    }

    interface ReUploadImageListener {
        fun onUploadImage()
    }

    companion object {
        //TODO HTTPS_PREFIX  move to app constants object
        private const val HTTPS_PREFIX = "https://"
    }
}