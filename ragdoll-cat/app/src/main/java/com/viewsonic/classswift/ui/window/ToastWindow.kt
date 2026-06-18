package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import com.viewsonic.classswift.databinding.WindowToastBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

class ToastWindow(val context: Context) :
    IWindow<WindowToastBinding> {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private var duration: Long = 3000

    override var tag: WindowTag = WindowTag.WINDOW_TOAST
    override var size: SizeInPixels =
        SizeInPixels(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    override val binding: WindowToastBinding = WindowToastBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
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

    fun setMessage(message: String) {
        binding.cstErrorToast.setText(message)
    }


    fun show() {
        coroutineScope.launch(Dispatchers.Main) {
            csWindowManager.createWindow(this@ToastWindow, Gravity.CENTER_TOP)
            withContext(Dispatchers.IO) {
                delay(duration)
            }
            csWindowManager.removeWindow(WindowTag.WINDOW_TOAST)
            coroutineScope.cancel()
        }
    }

    fun show(location: Location) {
        coroutineScope.launch(Dispatchers.Main) {
            csWindowManager.createWindow(
                window = this@ToastWindow,
                location = location,
                isOutOfScreen = true,
                isDraggable = false
            )
            withContext(Dispatchers.IO) {
                delay(duration)
            }
            csWindowManager.removeWindow(WindowTag.WINDOW_TOAST)
            coroutineScope.cancel()
        }
    }

    class MakeText(val context: Context, val message: String, val duration: Long) {
        fun build(): ToastWindow {
            val window = ToastWindow(context)
            window.duration = duration
            window.setMessage(message)
            return window
        }
    }

}