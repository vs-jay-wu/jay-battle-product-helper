package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WidgetMvbQcTagChipBinding

class MvbQuizTagChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Variant(val attrValue: Int) {
        QUIZ_TYPE(0),
        SUBJECT(1),
        STANDARDS(2),
        SUBJECT_GENERAL(3),
        ;

        companion object {
            fun from(attrValue: Int): Variant =
                values().firstOrNull { it.attrValue == attrValue } ?: SUBJECT
        }
    }

    private val binding: WidgetMvbQcTagChipBinding =
        WidgetMvbQcTagChipBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        background = null
        attrs?.let { applyAttrs(it) }
    }

    private fun applyAttrs(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MvbQuizTagChipView)
        try {
            val variantValue = typedArray.getInt(
                R.styleable.MvbQuizTagChipView_mqctc_variant,
                Variant.SUBJECT.attrValue,
            )
            setVariant(Variant.from(variantValue))
            val text = typedArray.getString(R.styleable.MvbQuizTagChipView_mqctc_text)
            if (!text.isNullOrEmpty()) {
                setText(text)
            }
        } finally {
            typedArray.recycle()
        }
    }

    fun setVariant(variant: Variant) {
        @DrawableRes val iconRes: Int = when (variant) {
            Variant.QUIZ_TYPE -> R.drawable.ic_mvb_qc_quiz_type
            Variant.SUBJECT, Variant.SUBJECT_GENERAL -> R.drawable.ic_mvb_qc_subject
            Variant.STANDARDS -> R.drawable.ic_mvb_qc_standards
        }
        @ColorRes val tintColor: Int = when (variant) {
            Variant.SUBJECT_GENERAL -> R.color.neutral_650
            else -> R.color.neutral_900
        }
        val resolvedColor = ContextCompat.getColor(context, tintColor)
        binding.ivMqctcIcon.setImageResource(iconRes)
        binding.ivMqctcIcon.setColorFilter(resolvedColor)
        binding.ivMqctcIcon.visibility = View.VISIBLE
        binding.tvMqctcText.setTextColor(resolvedColor)
    }

    fun setText(text: CharSequence?) {
        binding.tvMqctcText.text = text
    }
}
