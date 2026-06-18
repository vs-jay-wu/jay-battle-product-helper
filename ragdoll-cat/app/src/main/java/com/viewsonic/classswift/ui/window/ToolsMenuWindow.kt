package com.viewsonic.classswift.ui.window

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WindowToolsMenuBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.toolbar.CSSubordinateMenuItem
import com.viewsonic.classswift.ui.window.tool.BuzzerWindow
import com.viewsonic.classswift.ui.window.tool.RandomDrawWindow
import com.viewsonic.classswift.ui.window.tool.SpinnerWindow
import com.viewsonic.classswift.ui.window.tool.TimerToolWindow
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnCSWindowChangedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class ToolsMenuWindow(context: Context) : IWindow<WindowToolsMenuBinding>, OnCSWindowChangedListener {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)

    override var tag: WindowTag = WindowTag.TOOLS_MENU
    override var size: SizeInPixels = SizeInPixels(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)

    @SuppressLint("ClickableViewAccessibility")
    override val binding: WindowToolsMenuBinding = WindowToolsMenuBinding.inflate(
        LayoutInflater.from(context)
    ).apply {
        cssmiBuzzer.setOnClickListener {
            coroutineScope.launch(Dispatchers.Main) {
                csWindowManager.getWindow(WindowTag.BUZZER_TOOL)?.hoistWindowZOrder() ?: run {
                    val window = BuzzerWindow(context)
                    csWindowManager.createWindow(
                        window, Gravity.CENTER
                    )
                }
                csWindowManager.removeSubWindow(tag)
            }
        }

        cssmiRandomDrawer.setOnClickListener {
            coroutineScope.launch(Dispatchers.Main) {
                csWindowManager.getWindow(WindowTag.RANDOM_DRAW_TOOL)?.hoistWindowZOrder() ?: run {
                    val window = RandomDrawWindow(context)
                    csWindowManager.createWindow(
                        window, Gravity.CENTER
                    )
                }
                csWindowManager.removeSubWindow(tag)
            }
        }

        cssmiTimer.setOnClickListener {
            coroutineScope.launch(Dispatchers.Main) {
                csWindowManager.getWindow(WindowTag.TIMER_TOOL)?.hoistWindowZOrder() ?: run {
                    val window = TimerToolWindow(context)
                    csWindowManager.createWindow(
                        window, Gravity.CENTER
                    )
                }
                csWindowManager.removeSubWindow(tag)
            }
        }

        cssmiSpinner.setOnClickListener {
            coroutineScope.launch(Dispatchers.Main) {
                csWindowManager.getWindow(WindowTag.SPINNER_TOOL)?.hoistWindowZOrder() ?: run {
                    val window = SpinnerWindow(context)
                    csWindowManager.createWindow(
                        window, Gravity.CENTER
                    )
                }
                csWindowManager.removeSubWindow(tag)
            }
        }
    }

    override fun onViewCreated() {
        csWindowManager.addOnWindowChangedListener(this@ToolsMenuWindow)
        checkIfStateChanged()
    }

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

    override fun onDestroy() {
        csWindowManager.getWindow(WindowTag.TOOLBAR)?.let { iWindowContainer ->
            val window: ToolbarWindow = iWindowContainer.customWindow as ToolbarWindow
            window.checkIconButtonSelectedState()
        }
    }

    fun setBuzzerItemState(itemState: CSSubordinateMenuItem.ItemState) {
        binding.cssmiBuzzer.setItemState(itemState)
    }

    fun setRandomDrawerItemState(itemState: CSSubordinateMenuItem.ItemState) {
        binding.cssmiRandomDrawer.setItemState(itemState)
    }

    fun setTimerItemState(itemState: CSSubordinateMenuItem.ItemState) {
        binding.cssmiTimer.setItemState(itemState)
    }

    fun enableSpinnerEntryPoint(enable: Boolean) {
        binding.cssmiSpinner.isVisible = enable
    }

    private fun checkIfStateChanged() {
        if (csWindowManager.isWindowExisted(WindowTag.BUZZER_TOOL)) {
            binding.cssmiBuzzer.setItemState(CSSubordinateMenuItem.ItemState.SELECTED)
        } else {
            binding.cssmiBuzzer.setItemState(CSSubordinateMenuItem.ItemState.NORMAL)
        }

        if (csWindowManager.isWindowExisted(WindowTag.RANDOM_DRAW_TOOL)) {
            binding.cssmiRandomDrawer.setItemState(CSSubordinateMenuItem.ItemState.SELECTED)
        } else {
            binding.cssmiRandomDrawer.setItemState(CSSubordinateMenuItem.ItemState.NORMAL)
        }

        if (csWindowManager.isWindowExisted(WindowTag.TIMER_TOOL)) {
            binding.cssmiTimer.setItemState(CSSubordinateMenuItem.ItemState.SELECTED)
        } else {
            binding.cssmiTimer.setItemState(CSSubordinateMenuItem.ItemState.NORMAL)
        }

        if (csWindowManager.isWindowExisted(WindowTag.SPINNER_TOOL)) {
            binding.cssmiSpinner.setItemState(CSSubordinateMenuItem.ItemState.SELECTED)
        } else {
            binding.cssmiSpinner.setItemState(CSSubordinateMenuItem.ItemState.NORMAL)
        }
    }

    override fun onCSWindowCountChanged() {
        checkIfStateChanged()
    }

    override fun onCSWindowHiddenCountChange() = Unit
}