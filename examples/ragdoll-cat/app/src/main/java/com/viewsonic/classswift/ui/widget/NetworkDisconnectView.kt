package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewNetworkDisconnectBinding
import timber.log.Timber
import androidx.core.view.isVisible

class NetworkDisconnectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    private val binding = ViewNetworkDisconnectBinding.inflate(LayoutInflater.from(context), this, true)
    private var closeClickListener: (() -> Unit)? = null

    init {
        // set Attr variable
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NetworkDisconnectView,
            0,
            0
        ).apply {
            try {
                val cardRadius = this.getDimension(R.styleable.NetworkDisconnectView_cardRadius, 0f)
                Timber.d("network disconnect card radius: $cardRadius")
                this@NetworkDisconnectView.radius = cardRadius
                this@NetworkDisconnectView.setCardBackgroundColor(context.getColor(R.color.black_a40))
                this@NetworkDisconnectView.elevation = 0f
            } finally {
                recycle()
            }
        }
        binding.tvClose.paintFlags = binding.tvClose.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvClose.setOnClickListener { closeClickListener?.invoke() }
    }

    fun setCloseClickListener(listener: (() -> Unit)?) { closeClickListener = listener }

    fun bindCloseAction(closeView: View) {
        setCloseClickListener {
            closeView
                .takeIf { it.isEnabled && it.isVisible }
                ?.performClick()
        }
    }
}
