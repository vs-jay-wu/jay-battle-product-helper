package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager.LayoutParams
import com.viewsonic.classswift.databinding.WindowTurotialBinding
import com.viewsonic.classswift.ui.widget.CSTooltipView
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow

class TutorialWindow(
    private val applicationContext: Context,
) : IWindow<WindowTurotialBinding> {

    override var tag: WindowTag = WindowTag.WINDOW_TUTORIAL
    override var size: SizeInPixels =
        SizeInPixels(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    override val binding: WindowTurotialBinding = WindowTurotialBinding.inflate(
        LayoutInflater.from(applicationContext)
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onViewCreated() {}

    fun setTitle(title: String) {
        binding.cstvTooltip.setTitle(title)
    }

    fun setDescription(description: String) {
        binding.cstvTooltip.setDescription(description)
    }

    fun setLottieAnimation(rawResId: Int) {
        binding.cstvTooltip.setLottieAnimation(rawResId)
    }

    fun setLottieImage(rawResId: Int) {
        binding.cstvTooltip.setLottieImage(rawResId)
    }

    fun setArrowPosition(arrowPosition: CSTooltipView.ArrowPosition) {
        binding.cstvTooltip.setArrowPosition(arrowPosition)
    }

    fun showNegativeButton() {
        binding.cstvTooltip.showNegativeButton()
    }

    fun hideNegativeButton() {
        binding.cstvTooltip.hideNegativeButton()
    }

    fun setNegativeOnClickListener(listener: OnClickListener) {
        binding.cstvTooltip.setNegativeOnClickListener(listener)
    }

    fun setPositiveButtonTitle(titleResId: Int) {
        binding.cstvTooltip.setPositiveButtonTitle(titleResId)
    }

    fun setPositiveOnClickListener(listener: OnClickListener) {
        // Clicking outside the tooltip area should behave the same as clicking the positive button.
        binding.viewBackground.setOnClickListener(listener)
        binding.cstvTooltip.setPositiveOnClickListener(listener)
    }

    fun setAnchorPosition(anchorX: Int, anchorY: Int, anchorGravity: AnchorGravity) {
        val (tooltipMeasureWidth, tooltipMeasureHeight) =
            binding.cstvTooltip.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            ).let {
                binding.cstvTooltip.measuredWidth to binding.cstvTooltip.measuredHeight
            }

        when (anchorGravity) {
            AnchorGravity.TOP_LEFT -> {
                binding.cstvTooltip.x = anchorX.toFloat()
                binding.cstvTooltip.y = anchorY.toFloat()
            }
            AnchorGravity.TOP_CENTER -> {
                binding.cstvTooltip.x = anchorX.toFloat() - (tooltipMeasureWidth / 2)
                binding.cstvTooltip.y = anchorY.toFloat()
            }
            AnchorGravity.TOP_RIGHT -> {
                binding.cstvTooltip.x = anchorX.toFloat() - tooltipMeasureWidth
                binding.cstvTooltip.y = anchorY.toFloat()
            }
            AnchorGravity.MIDDLE_LEFT -> {
                binding.cstvTooltip.x = anchorX.toFloat()
                binding.cstvTooltip.y = anchorY.toFloat() - (tooltipMeasureHeight / 2)
            }
            AnchorGravity.MIDDLE_RIGHT -> {
                binding.cstvTooltip.x = anchorX.toFloat() - tooltipMeasureWidth
                binding.cstvTooltip.y = anchorY.toFloat() - (tooltipMeasureHeight / 2)
            }
            AnchorGravity.BOTTOM_LEFT -> {
                binding.cstvTooltip.x = anchorX.toFloat()
                binding.cstvTooltip.y = anchorY.toFloat() - tooltipMeasureHeight
            }
            AnchorGravity.BOTTOM_CENTER -> {
                binding.cstvTooltip.x = anchorX.toFloat() - (tooltipMeasureWidth / 2)
                binding.cstvTooltip.y = anchorY.toFloat() - tooltipMeasureHeight
            }
            AnchorGravity.BOTTOM_RIGHT -> {
                binding.cstvTooltip.x = anchorX.toFloat() - tooltipMeasureWidth
                binding.cstvTooltip.y = anchorY.toFloat() - tooltipMeasureHeight
            }
        }
    }

    enum class AnchorGravity {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        MIDDLE_LEFT,
        MIDDLE_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }
}