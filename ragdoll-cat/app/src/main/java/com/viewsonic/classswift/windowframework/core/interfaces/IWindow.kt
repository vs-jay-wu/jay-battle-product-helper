package com.viewsonic.classswift.windowframework.core.interfaces

import android.view.View
import androidx.viewbinding.ViewBinding
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag

interface IWindow<out VB : ViewBinding> {

    var tag: WindowTag
    var size: SizeInPixels
    val binding: VB

    fun getRootView(): View {
        return binding.root
    }

    fun getCurrentSize(): SizeInPixels {
        return size
    }

    fun onCreate() {}
    fun onViewCreated() {}
    fun onViewSizeChanged(oldSizeInPixels: SizeInPixels, newSizeInPixels: SizeInPixels) {}
    fun onDestroy() {}
}