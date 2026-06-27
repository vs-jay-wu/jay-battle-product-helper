package com.viewsonic.classswift.ui.window.compose

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Converts an Android [Spanned] (e.g. a `SpannableString` from a WindowModel) to a Compose
 * [AnnotatedString], preserving the spans that matter for our screens — foreground color
 * (e.g. the brand-blue maintenance date) and bold/italic — so the CMP render stays faithful.
 */
fun CharSequence.toAnnotatedString(): AnnotatedString {
    val spanned = this as? Spanned ?: return AnnotatedString(toString())
    return buildAnnotatedString {
        append(spanned.toString())
        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start < 0 || end <= start) return@forEach
            when (span) {
                is ForegroundColorSpan ->
                    addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    Typeface.BOLD_ITALIC ->
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                }
            }
        }
    }
}
