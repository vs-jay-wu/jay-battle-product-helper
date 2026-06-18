package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.viewsonic.classswift.databinding.WidgetMvbQcEmptyStateBinding

class MvbQuizCollectionEmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: WidgetMvbQcEmptyStateBinding =
        WidgetMvbQcEmptyStateBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
        gravity = android.view.Gravity.CENTER
    }
}
