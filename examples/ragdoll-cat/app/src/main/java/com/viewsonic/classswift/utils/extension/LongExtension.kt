package com.viewsonic.classswift.utils.extension

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 原本顯示 12:34，但因:設計上需要有動畫，所以:留空白
 * return "12 34"
 */
fun Long.secondToTimeNoColon(): String {
    val minute = this / 60
    val second = this % 60
    return String.format(Locale.US, "%02d %02d", minute, second)
}

/**
 * return "12:34.56"
 */
fun Long.millionSecondToStopwatch(): String {
    val minutes = this / 60000
    val seconds = (this % 60000) / 1000
    val hundredths = (this % 1000) / 10
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
}

fun Long.milliSecondToTimerUnit(): String {
    val minutes = this / 60000
    val seconds = (this % 60000) / 1000
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

fun Long.formatAsTime(
    format: String = "HH:mm:ss",
    fallback: String = "00:00:00"
): String {
    if (this < 0) return fallback

    val totalSeconds = this
    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60

    return try {
        var result = format
        if ("HH" in result) result = result.replace("HH", hours.toString().padStart(2, '0'))
        if ("mm" in result) result = result.replace("mm", minutes.toString().padStart(2, '0'))
        if ("ss" in result) result = result.replace("ss", seconds.toString().padStart(2, '0'))
        if (result == format) fallback else result
    } catch (e: Exception) {
        fallback
    }
}