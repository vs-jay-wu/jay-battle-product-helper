package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import timber.log.Timber

class StarredTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    override fun setText(text: CharSequence?, type: BufferType?) {
        if (this.text.endsWith("*")) {
            super.setText(text, type)
            return
        }
        if (text.isNullOrEmpty()) {
            // 如果文字為空，則傳遞空字串
            super.setText("", type)
        } else {
            // 將傳入的文字處理為 SpannableString
            val spannableString = SpannableString("$text*")
            Timber.tag("StarredTextView").d("spannableString: $spannableString")
            // 將星號設置為上標
            spannableString.setSpan(
                SuperscriptSpan(),
                text.length , text.length + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                RelativeSizeSpan(0.75f),
                text.length , text.length + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.starred_text_view_red)),
                text.length , text.length + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // 將處理後的 SpannableString 設置到 TextView
            super.setText(spannableString, type)
        }
    }
}