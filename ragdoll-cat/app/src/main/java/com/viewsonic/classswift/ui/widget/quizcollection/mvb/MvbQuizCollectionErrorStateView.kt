package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.viewsonic.classswift.databinding.WidgetMvbQcErrorStateBinding

class MvbQuizCollectionErrorStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: WidgetMvbQcErrorStateBinding =
        WidgetMvbQcErrorStateBinding.inflate(LayoutInflater.from(context), this)

    var onRefreshClick: (() -> Unit)? = null
        set(value) {
            field = value
            binding.tvMqcerBtnRefresh.setOnClickListener { value?.invoke() }
        }

    init {
        orientation = VERTICAL
        gravity = android.view.Gravity.CENTER
    }
}
