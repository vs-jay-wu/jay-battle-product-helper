package com.viewsonic.classswift.ui.window.viewholder

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.databinding.ItemInAppTutorialCompleteBinding

class InAppTutorialCompleteViewHolder(
    private val binding: ItemInAppTutorialCompleteBinding,
    private val onItemInteractionListener: OnItemInteractionListener,
    private val onItemCallback: OnItemCallback
) : RecyclerView.ViewHolder(binding.root) {

    init {
        with(binding) {
            clFeedbackContainer.isVisible = onItemCallback.isNeedToShowFeedbackUI()
            buttonThumbUp.setOnClickListener {
                buttonThumbUp.isSelected = true
                buttonThumbUp.isClickable = false
                buttonThumbDown.visibility = View.GONE
                startRippleAnimation()
                onItemInteractionListener.onThumbUpClicked()
            }
            buttonThumbDown.setOnClickListener {
                buttonThumbDown.isSelected = true
                buttonThumbDown.isClickable = false
                buttonThumbUp.visibility = View.GONE
                startRippleAnimation()
                onItemInteractionListener.onThumbDownClicked()
            }
        }
    }

    private fun startRippleAnimation() {
        val animationDurationInMillis = 1500L
        val animationRepeatCount = 2
        with(binding) {
            viewRipple.isVisible = true
            val scaleX = ObjectAnimator.ofFloat(viewRipple, View.SCALE_X, 0f, 1f).apply {
                repeatCount = animationRepeatCount
            }
            val scaleY = ObjectAnimator.ofFloat(viewRipple, View.SCALE_Y, 0f, 1f).apply {
                repeatCount = animationRepeatCount
            }
            val alpha = ObjectAnimator.ofFloat(viewRipple, View.ALPHA, 1f, 0f).apply {
                repeatCount = animationRepeatCount
            }
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = animationDurationInMillis
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    interface OnItemInteractionListener {
        fun onThumbUpClicked()
        fun onThumbDownClicked()
    }

    interface OnItemCallback {
        fun isNeedToShowFeedbackUI(): Boolean
    }
}