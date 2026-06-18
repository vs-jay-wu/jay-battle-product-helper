package com.viewsonic.classswift.ui.window

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager.LayoutParams
import com.viewsonic.classswift.databinding.WindowCropImageBinding
import com.viewsonic.classswift.ui.customview.CropImageView
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import timber.log.Timber

class CropImageWindow(context: Context) : IWindow<WindowCropImageBinding> {
    private var cropImagePath: String = ""

    var cancelButtonListener: (() -> Unit)? = null
    private var onCropImageRangeListener: CropImageView.OnCropRangeListener? = null

    fun setCropImagePath(pathStr: String) {
        cropImagePath = pathStr
    }

    fun setOnCropRangeListener(listener: CropImageView.OnCropRangeListener) {
        this.onCropImageRangeListener = listener
    }


    override var tag: WindowTag = WindowTag.CROP_IMAGE
    override var size: SizeInPixels =
        SizeInPixels(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    override val binding: WindowCropImageBinding = WindowCropImageBinding.inflate(
        LayoutInflater.from(context)
    ).apply {
        //cancel button
        buttonCancel.setOnClickListener {
            Timber.d("[buttonCancel]: clicked")
            CSWindowManager.removeWindow(WindowTag.CROP_IMAGE)
            cancelButtonListener?.invoke()
            //因為外部可能會引用到 singleton, 設定用完一次先清除
            cancelButtonListener = null
        }

        //screenshot tool
        csivCrop.apply {
            setSelectionStyle(
                color = Color.WHITE,
                strokeWidth = 4f,
                isDashed = false
            )

            setOnCropRangeListener(object : CropImageView.OnCropRangeListener {
                override fun cropRange(x: Int, y: Int, width: Int, height: Int) {
                    onCropImageRangeListener?.cropRange(x, y, width, height)
                }
            })

            onCropEvent = { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        buttonCancel.visibility = View.INVISIBLE
                        llDragInfo.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

}

