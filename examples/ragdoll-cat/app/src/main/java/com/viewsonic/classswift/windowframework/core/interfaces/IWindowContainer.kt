package com.viewsonic.classswift.windowframework.core.interfaces

import android.view.ViewGroup
import android.view.WindowManager
import androidx.viewbinding.ViewBinding
import com.viewsonic.classswift.windowframework.core.data.Config
import com.viewsonic.classswift.windowframework.core.enums.ViewState
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnMotionEventChangedListener
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnViewPositionChangedListener

interface IWindowContainer {

    var floatWindowLayoutParam: WindowManager.LayoutParams
    var floatingView: ViewGroup
    var customWindow: IWindow<ViewBinding>

    fun getLayoutParam(): WindowManager.LayoutParams

    // Caution: Do not assign a new instance, as it may cause the window’s touch and move functionality to fail.
    fun updateLayoutParam(windowLayoutParams: WindowManager.LayoutParams)

    fun getWindowConfig(): Config

    fun setVisibility(viewState: ViewState)

    suspend fun hoistWindowZOrder()

    fun setOnMotionEventChangedListener(onMotionEventChangedListener: OnMotionEventChangedListener)

    fun addOnViewPositionChangedListener(listener: OnViewPositionChangedListener) {}

    fun removeOnViewPositionChangedListener(listener: OnViewPositionChangedListener) {}

    fun onCreate() {}

    fun onViewCreated() {}

    // 由 windowContainer.removeWindow() 時呼叫，非真正系統 GC
    fun onDestroy() {}
}