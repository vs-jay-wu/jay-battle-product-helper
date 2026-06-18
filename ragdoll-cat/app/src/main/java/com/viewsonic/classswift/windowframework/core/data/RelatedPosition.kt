package com.viewsonic.classswift.windowframework.core.data

import com.viewsonic.classswift.windowframework.core.enums.Horizontal
import com.viewsonic.classswift.windowframework.core.enums.Vertical

data class RelatedPosition(
    var horizontal: Horizontal = Horizontal.CENTER,
    val vertical: Vertical = Vertical.TOP,
    var marginVerticalInPixels: Int = 16
)
