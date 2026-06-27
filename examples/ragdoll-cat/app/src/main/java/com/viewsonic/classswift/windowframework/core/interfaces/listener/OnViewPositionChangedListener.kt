package com.viewsonic.classswift.windowframework.core.interfaces.listener

interface OnViewPositionChangedListener {
    // sub-window 接收 main-window 的位置資訊
    fun onViewPositionChanged(coordinateX: Int, coordinateY: Int, anchorXOffset: Int)
}