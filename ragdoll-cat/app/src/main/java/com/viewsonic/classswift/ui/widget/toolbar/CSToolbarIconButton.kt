package com.viewsonic.classswift.ui.widget.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewToolbarIconButtonBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.utils.DisplayUtils
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.RelatedPosition
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.Horizontal
import com.viewsonic.classswift.windowframework.core.enums.Vertical
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnCSWindowChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class CSToolbarIconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), OnCSWindowChangedListener, View.OnAttachStateChangeListener {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private var binding: ViewToolbarIconButtonBinding =
        ViewToolbarIconButtonBinding.inflate(LayoutInflater.from(context), this)
    private var buttonState: ButtonState = ButtonState.ACTIVE
    private var callback: Callback? = null
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CSToolbarIconButton,
            0,
            0
        ).apply {
            try {
                val iconSrcResId = getResourceId(R.styleable.CSToolbarIconButton_iconSrc, -1)
                buttonState = ButtonState.entries[getInt(R.styleable.CSToolbarIconButton_buttonState, 0)]
                if (iconSrcResId != -1) {
                    binding.ivIcon.setImageResource(iconSrcResId)
                    binding.ivIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.selector_toolbar_icon_button_tint)
                }
            } finally {
                recycle()
            }
        }
        updateUiByButtonState(buttonState, true)
        setOnClickListener {
            if (callback == null) {
                Timber.e("[init]: callback is null")
                return@setOnClickListener
            }
            callback?.let { callback ->
                val boundWindowTag = callback.getBoundWindowTag()
                if (WindowTag.isSubWindowTag(boundWindowTag)) {
                    WindowTag.getSubWindowTagList()
                        .filter { it != boundWindowTag }
                        .forEach {
                            csWindowManager.removeSubWindow(it)
                        }
                }

                when (buttonState) {
                    ButtonState.ACTIVE,
                    ButtonState.SELECTED -> {
                        coroutineScope.launch(Dispatchers.Main) {
                            if (csWindowManager.getWindow(boundWindowTag) == null) {
                                if (callback.isSubWindow()) {
                                    val anchorX =
                                        DisplayUtils.getViewTopCenterLocationOnDisplay(this@CSToolbarIconButton).first
                                    csWindowManager.createSubWindow(
                                        callback.getNewWindowInstance(),
                                        WindowTag.TOOLBAR,
                                        anchorX,
                                        RelatedPosition(
                                            horizontal = Horizontal.CENTER,
                                            vertical = Vertical.TOP
                                        )
                                    )
                                } else {
                                    csWindowManager.createWindow(
                                        callback.getNewWindowInstance(),
                                        Gravity.CENTER
                                    )
                                }
                            } else {
                                val isWindowNotVisible = csWindowManager.isWindowHidden(boundWindowTag)
                                    || csWindowManager.isWindowMinimized(boundWindowTag)

                                if (isWindowNotVisible) {
                                    csWindowManager.bringWindowToTop(boundWindowTag)
                                    csWindowManager.showWindow(boundWindowTag)
                                    callback.updateWindowAsNeeded()
                                } else {
                                    if (callback.isSubWindow()) {
                                        csWindowManager.removeSubWindow(boundWindowTag)
                                    } else {
                                        callback.updateWindowAsNeeded()
                                    }
                                }
                            }
                            checkIfInSelectedState()
                        }
                    }

                    ButtonState.DISABLED -> {
                        if (callback.isSubWindow()) {
                            csWindowManager.removeSubWindow(boundWindowTag)
                        } else {
                            csWindowManager.removeWindow(boundWindowTag)
                        }
                        if (csWindowManager.getWindow(boundWindowTag) == null) {
                            if (callback.isSubWindow()) {
                                val anchorX = DisplayUtils.getViewTopCenterLocationOnDisplay(this@CSToolbarIconButton).first
                                csWindowManager.createSubWindow(
                                    callback.getNewWindowInstance(),
                                    WindowTag.TOOLBAR,
                                    anchorX,
                                    RelatedPosition(
                                        horizontal = Horizontal.CENTER,
                                        vertical = Vertical.TOP
                                    )
                                )
                            } else {
                                csWindowManager.createWindow(
                                    callback.getNewWindowInstance(),
                                    Gravity.CENTER
                                )
                            }
                        }
                    }
                }
            }
        }
        addOnAttachStateChangeListener(this@CSToolbarIconButton)
    }

    override fun onViewAttachedToWindow(v: View) {
        if (!isInEditMode) {
            csWindowManager.addOnWindowChangedListener(this)
        }
    }

    override fun onViewDetachedFromWindow(v: View) {
        if (!isInEditMode) {
            csWindowManager.removeOnWindowChangedListener(this)
        }
    }

    override fun onCSWindowCountChanged() {
        checkIfInSelectedState()
    }

    override fun onCSWindowHiddenCountChange() {
        checkIfInSelectedState()
    }

    fun setButtonData(buttonData: CSToolbarIconButtonData) {
        isVisible = buttonData.isVisible
        if (buttonData.isVisible) {
            this.callback = buttonData.callback
            updateUiByButtonState(buttonData.buttonState)
            checkIfInSelectedState()
        }
    }

    private fun updateUiByButtonState(newState: ButtonState, isForced: Boolean = false) {
        if (buttonState == newState && !isForced) {
            return
        }
        buttonState = newState
        when (buttonState) {
            ButtonState.ACTIVE -> {
                binding.ivIcon.isEnabled = true
                binding.ivIcon.isSelected = false
            }

            ButtonState.DISABLED -> {
                binding.ivIcon.isEnabled = false
                binding.ivIcon.isSelected = false
            }

            ButtonState.SELECTED -> {
                binding.ivIcon.isEnabled = true
                binding.ivIcon.isSelected = true
            }
        }
    }

    fun checkIfInSelectedState() {
        if (buttonState == ButtonState.DISABLED) {
            return
        }
        callback?.let { callback ->
            val boundWindowTag = callback.getBoundWindowTag()
            if (csWindowManager.getWindow(boundWindowTag) != null) {
                if (csWindowManager.isWindowHidden(boundWindowTag)
                    || csWindowManager.isWindowMinimized(boundWindowTag)
                ) {
                    updateUiByButtonState(ButtonState.ACTIVE)
                    return@let
                }
                updateUiByButtonState(ButtonState.SELECTED)
                return@let
            }
            callback.getObservedWindowTagList().forEach { tag ->
                if (csWindowManager.getWindow(tag) != null) {
                    updateUiByButtonState(ButtonState.SELECTED)
                    return@let
                }
            }
            updateUiByButtonState(ButtonState.ACTIVE)
        }
    }

    interface Callback {
        fun getBoundWindowTag(): WindowTag
        fun isSubWindow(): Boolean
        fun getObservedWindowTagList(): List<WindowTag>
        fun getNewWindowInstance(): IWindow<ViewBinding>
        fun updateWindowAsNeeded() {}
    }

    data class CSToolbarIconButtonData(
        val isVisible: Boolean = true,
        val buttonState: ButtonState = ButtonState.DISABLED,
        val callback: Callback? = null
    )

    enum class ButtonState {
        ACTIVE,
        DISABLED,
        SELECTED
    }
}