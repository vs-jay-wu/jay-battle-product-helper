package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.createBitmap
import coil.load
import com.viewsonic.classswift.constant.AppConstants.HTTPS_PREFIX
import com.viewsonic.classswift.databinding.ViewImageDownloadBinding
import com.viewsonic.classswift.manager.CoroutineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ImageDownloadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val coroutineScope = CoroutineManager.getScope(this)
    private var progressJob: Job? = null

    private var binding: ViewImageDownloadBinding =
        ViewImageDownloadBinding.inflate(LayoutInflater.from(context), this)
    private var downloadImageListener: DownloadImageListener? = null

    interface DownloadImageListener {
        fun onReDownload()
        fun onStartDownload()
        fun onDownloadSuccess()
        fun onDownloadCancel()
        fun onDownloadError()
    }

    init {
        startProgressAnimation()
        binding.cvRetry.setOnClickListener {
            downloadImageListener?.onReDownload()
        }
    }

    /**
     * setup image
     */
    fun setImage(uri: String) {
        Timber.d("[setImage] uri - $uri")
        if (uri.startsWith(HTTPS_PREFIX)) {
            // Coil loads image from URL
            binding.ivImage.load(uri) {
                // need to add this below, otherwise it will throw an IllegalArgumentException:
                // "Software rendering doesn't support hardware bitmaps",
                // reference to https://coil-kt.github.io/coil/recipes/
                allowHardware(false)

                listener(
                    onStart = {
                        downloadImageListener?.onStartDownload()
                    },
                    onSuccess = { _, _ ->
                        downloadImageListener?.onDownloadSuccess()
                    },
                    onError = { _, _ ->
                        downloadImageListener?.onDownloadError()
                    },
                    onCancel = {
                        downloadImageListener?.onDownloadCancel()
                    }
                )
            }
        } else {
            binding.ivImage.setImageBitmap(BitmapFactory.decodeFile(uri))
        }
    }

    fun setOnDownloadImageListener(listener: DownloadImageListener) {
        downloadImageListener = listener
    }

    fun setLocalImage(uri: Uri) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        binding.ivImage.setImageBitmap(bitmap)
        inputStream?.close()
    }

    fun setMaskVisibility(isShown: Boolean) {
        binding.csMaskView.visibility = if (isShown) View.VISIBLE else View.GONE
    }

    fun setCircleProgressbarVisibility(isShown: Boolean) {
        binding.csCircleProgressBar.visibility = if (isShown) View.VISIBLE else View.GONE
    }

    fun setFailedContainerVisibility(isShown: Boolean) {
        binding.clFailedContainer.visibility = if (isShown) View.VISIBLE else View.GONE
    }

    /**
     * Update progressbar status
     */
    fun startProgressAnimation(fromPercentage: Int = 0, toPercentage: Int = 90) {
        Timber.d("[startProgressAnimation] fromPercentage-$fromPercentage, toPercentage-$toPercentage")
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.IO) {
            for (percentage in fromPercentage..toPercentage) {
                withContext(Dispatchers.Main) {
                    binding.csCircleProgressBar.setProgress(percentage)
                }
                delay(100L)
            }
        }
    }

    fun exportToBitmap(): Bitmap {
        with(binding.ivImage) {
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            draw(canvas)
            return bitmap
        }
    }
}