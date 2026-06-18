package com.viewsonic.classswift.utils.extension

import android.content.res.Resources
import android.util.TypedValue
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

fun Int.dpToPx(): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics
    )
}


/**
 *  yyyy/MM/dd
 */
fun Int.toDate(): String {
    val instant = Instant.ofEpochSecond(this.toLong())
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}