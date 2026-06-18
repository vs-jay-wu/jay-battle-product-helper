package com.viewsonic.classswift.utils.extension

import android.graphics.Bitmap

fun Bitmap.doRecycle() {
    if (!isRecycled) recycle()
}