package com.viewsonic.classswift.windowframework.core.interfaces.listener

import android.net.Uri

interface OnCropViewListener {
    fun cropSuccess(uri: Uri)
    fun cropFailed(errorMessage: String)
}