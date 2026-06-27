package com.viewsonic.classswift.utils.extension

import android.content.res.Resources
import android.util.TypedValue
import java.math.BigDecimal
import java.math.RoundingMode

fun Float.dpToPx(): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
    )
}

fun Float.pxToDp(): Float {
    return this / Resources.getSystem().displayMetrics.density
}

fun Float.toPercent(): String {
    val percent = this * 100f
    val rounded = BigDecimal(percent.toDouble())
        .setScale(1, RoundingMode.HALF_UP)   //Round to 1 decimal place, keeping the trailing .0.
    return "${rounded.toPlainString()}%"
}