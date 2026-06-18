package com.viewsonic.classswift.utils.extension

import android.view.View
import com.viewsonic.classswift.constant.AppConstants.THREE_SEC_DELAY
import com.viewsonic.classswift.ui.widget.CSToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun CSToast.show(
    durationInMillis: Long = THREE_SEC_DELAY
) = withContext(Dispatchers.Main) {
    if (visibility != View.VISIBLE) {
        visibility = View.VISIBLE
        withContext(Dispatchers.IO) {
            delay(durationInMillis)
        }
        visibility = View.GONE
    }
}

fun CSToast.show(
    coroutineScope: CoroutineScope,
    durationInMillis: Long = THREE_SEC_DELAY
) {
    coroutineScope.launch(Dispatchers.Main) {
        if (visibility != View.VISIBLE) {
            visibility = View.VISIBLE
            withContext(Dispatchers.IO) {
                delay(durationInMillis)
            }
            visibility = View.GONE
        }
    }
}


