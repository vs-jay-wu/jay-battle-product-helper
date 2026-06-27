package com.viewsonic.classswift.utils

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.core.content.ContextCompat
import org.koin.java.KoinJavaComponent.inject

object SpannableStringUtils {
    private val applicationContext: Context by inject(Context::class.java)

    /**
     *  The string from @stringResId should has at least one  argument for formatting.
     *  Example:
     *      <string name="test_string">Hello, $s! You have new messages.</string>
     */
    fun replaceStringFirstArgAsBoldStyle(stringResId: Int, argString: String): SpannableString {
        val message = applicationContext.getString(stringResId, argString)
        val startIndex = message.indexOf(argString)
        val spannedString = SpannableString(message)
        spannedString.setSpan(
            StyleSpan(Typeface.BOLD),
            startIndex,
            startIndex + argString.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spannedString
    }

    fun replaceStringFirstArgAsBoldStyle(fullString: String, argString: String): SpannableString {
        val message = String.format(fullString, argString)
        val startIndex = message.indexOf(argString)
        val spannedString = SpannableString(message)
        spannedString.setSpan(
            StyleSpan(Typeface.BOLD),
            startIndex,
            startIndex + argString.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spannedString
    }

    fun replaceStringFirstArgAsColorStyle(stringResId: Int, argString: String, colorResId: Int): SpannableString {
        val message = applicationContext.getString(stringResId, argString)
        val startIndex = message.indexOf(argString)
        val spannedString = SpannableString(message)
        spannedString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(applicationContext, colorResId)),
            startIndex,
            startIndex + argString.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spannedString
    }

    fun replaceStringWithUnderline(stringResId: Int): SpannableString {
        val message = applicationContext.getString(stringResId)
        val spannedString = SpannableString(message)
        spannedString.setSpan(
            UnderlineSpan(),
            0,
            message.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spannedString
    }
}