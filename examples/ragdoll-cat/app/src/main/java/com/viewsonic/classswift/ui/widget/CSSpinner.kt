package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewCsSpinnerBinding
import com.viewsonic.classswift.databinding.ViewSpinnerDropDownWindowBinding
import timber.log.Timber

class CSSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {


    private val popupBinding = ViewSpinnerDropDownWindowBinding.inflate(LayoutInflater.from(context))
    private var popupWindow: PopupWindow? = null
    private var listener: OnSpinnerItemSelectedListener? = null
    private var isKeyboardVisible = false
    private var needShowPopupWindow = false
    private var popupWindowHeight = 448
    var displayString: CharSequence
        get() = binding.tvCountry.text
        set(value) {
            binding.tvCountry.text = value
        }


    private val binding: ViewCsSpinnerBinding =
        ViewCsSpinnerBinding.inflate(LayoutInflater.from(context), this, true).apply {
            root.viewTreeObserver.addOnGlobalLayoutListener {
                Timber.tag("CSspinner").d("setFocusUI onGlobalLayout ")
                if (popupWindow == null) {
                    popupWindow = PopupWindow(
                        popupBinding.root,
                        root.width,
                        popupWindowHeight,
                        true
                    )
                    popupWindow?.setOnDismissListener {
                        needShowPopupWindow = false
                        setFocusUI(false)
                    }
                }
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keyboardHeight = screenHeight - rect.bottom
                val isNowVisible = keyboardHeight > screenHeight * 0.15 // 假設鍵盤高度超過 15% 時認為彈出
                if (isNowVisible != isKeyboardVisible) {
                    isKeyboardVisible = isNowVisible
                    if (!isKeyboardVisible && needShowPopupWindow) {
                        popupWindow?.showAsDropDown(tvCountry, 0, 16)
                        needShowPopupWindow = false
                    }
                }

            }
            acivIcon.setImageResource(R.drawable.ic_arrow_down_black)
            tvCountry.setOnClickListener {
                setFocusUI(true)
            }
        }


    fun setAdapter(adapter: BaseAdapter, listener: OnSpinnerItemSelectedListener?) {
        popupBinding.lvPopupList.adapter = adapter
        popupBinding.lvPopupList.divider = null
        this.listener = listener
        popupBinding.lvPopupList.setOnItemClickListener { _, _, position, _ ->
            setFocusUI(false)
            listener?.onItemSelected(position)

        }
    }

    private fun setFocusUI(isFocus: Boolean) {
        if (isFocus) {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(binding.tvCountry.windowToken, 0)
            binding.acivIcon.setImageResource(R.drawable.ic_arrow_up_blue)
            binding.root.setBackgroundResource(R.drawable.bg_cs_spinner_focus)
            if (isKeyboardVisible) {
                needShowPopupWindow = true
            } else {
                popupWindow?.showAsDropDown(binding.tvCountry, 0, 16)
            }
        } else {
            binding.acivIcon.setImageResource(R.drawable.ic_arrow_down_black)
            binding.root.setBackgroundResource(R.drawable.bg_cs_spinner)
            if (popupWindow?.isShowing == true) {
                popupWindow?.dismiss()
            }
        }
    }

    // for change drop down popup window height
    fun setPopUpWindowHeight(height: Int) {
        popupWindowHeight = height
    }

    interface OnSpinnerItemSelectedListener {
        fun onItemSelected(position: Int)
    }
}

