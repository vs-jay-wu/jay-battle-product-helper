package com.viewsonic.classswift.windowframework.core.interfaces.listener

import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag

interface OnCSWindowChangedListener {
    fun onCSWindowCountChanged()
    fun onCSWindowHiddenCountChange()
    fun onCSWindowStateChanged(windowTag: WindowTag, state: CSWindowManager.WindowState) = Unit
}