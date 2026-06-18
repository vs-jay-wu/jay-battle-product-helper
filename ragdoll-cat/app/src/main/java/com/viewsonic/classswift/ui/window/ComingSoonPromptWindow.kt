package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.viewsonic.classswift.databinding.WindowComingSoonPromptBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class ComingSoonPromptWindow(context: Context) : IWindow<WindowComingSoonPromptBinding> {
    private val dismissCheckInterval: Long = 1000 // ms
    private val dismissTimeInterval: Long = 3000 // ms
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private var createdTime: Long = 0
    private var dismissTimerJob: Job? = null

    override var tag: WindowTag = WindowTag.COMING_SOON_PROMPT
    override var size: SizeInPixels = SizeInPixels(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)

    override val binding: WindowComingSoonPromptBinding = WindowComingSoonPromptBinding.inflate(
        LayoutInflater.from(context)
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onCreate() {
        createdTime = System.currentTimeMillis()
        dismissTimerJob?.cancel()
        dismissTimerJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                while (System.currentTimeMillis() - createdTime < dismissTimeInterval) {
                    delay(dismissCheckInterval)
                }
                withContext(Dispatchers.Main) {
                    csWindowManager.removeSubWindow(tag)
                }
            } catch (exception: CancellationException) {
                Timber.e("[onCreate]: CancellationException")
            }
        }
    }

    override fun onDestroy() {
        dismissTimerJob?.cancel()
    }

    fun setTitle(title: String) {
        binding.tvTitle.text = title
    }
    
    fun refreshCreatedTime() {
        createdTime = System.currentTimeMillis()
    }
}