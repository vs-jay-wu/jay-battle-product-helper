package com.viewsonic.classswift.windowframework.core.data

import com.viewsonic.classswift.windowframework.core.enums.ViewState
import com.viewsonic.classswift.windowframework.core.enums.WindowTag

data class Config(
    var tag: WindowTag = WindowTag.DEFAULT,
    var isInitialized: Boolean = false,
    var isRemoved: Boolean = false,
    var isSubWindow: Boolean = false,
    var hasSubWindow: Boolean = false,
    var mainWindowTag: WindowTag = WindowTag.NONE,
    var relatedPosition: RelatedPosition = RelatedPosition(),
    var viewState: ViewState = ViewState.VISIBLE,
    var sizeInPixels: SizeInPixels = SizeInPixels(0, 0),
    var location: Location = Location(0, 0),
    // toolbar 內按鈕的水平中心位置，與 toolbar location.coordinateX 的水平 offset
    var anchorXOffset: Int = 0,
    val isDraggable: Boolean = false
)