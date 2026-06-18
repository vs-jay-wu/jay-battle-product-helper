package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WindowSettingMenuBinding
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.toolbar.CSSubordinateMenuItem
import com.viewsonic.classswift.ui.window.quiz.start.BatchQuizStartWindow
import com.viewsonic.classswift.ui.window.tool.SettingsWindow
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager.ParticipationState
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnCSWindowChangedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

class SettingMenuWindow(context: Context) :
    IWindow<WindowSettingMenuBinding>, OnCSWindowChangedListener {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val toolbarManager: ToolbarManager by inject(ToolbarManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private var participationState = ParticipationState.NOT_JOINED

    override var tag: WindowTag = WindowTag.SETTING_MENU
    override var size: SizeInPixels =
        SizeInPixels(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)

    override val binding: WindowSettingMenuBinding = WindowSettingMenuBinding.inflate(
        LayoutInflater.from(context)
    ).apply {
        cssmiSettings.setOnClickListener {
            coroutineScope.launch(Dispatchers.Main) {
                csWindowManager.getWindow(WindowTag.SETTING_WINDOW)?.hoistWindowZOrder() ?: run {
                    val window = SettingsWindow(context)
                    csWindowManager.createWindow(
                        window, Gravity.CENTER
                    )
                }
                csWindowManager.removeSubWindow(tag)
            }
        }
        cssmiSignOut.setOnClickListener {
            csWindowManager.removeSubWindow(tag)
            when (participationState) {
                ParticipationState.LESSON_STARTED -> {
                    CSSystemDialogWindow.Builder(context)
                        .setTitle(context.getString(R.string.dialog_buttons_end_in_session))
                        .setMessage(context.getString(R.string.dialog_end_lesson_with_logout))
                        .setNegativeButton(
                            text = context.getString(R.string.common_cancel),
                            color = ContextCompat.getColor(context, R.color.color_2E3133),
                            listener = {
                                csWindowManager.removeWindow(WindowTag.CS_NORMAL_DIALOG)
                            }
                        )
                        .setPositiveButton(
                            text = context.getString(R.string.dialog_buttons_end_lesson_logout),
                            color = ContextCompat.getColor(context, R.color.color_F02B2B),
                            listener = {
                                coroutineScope.launch {
                                    toolbarManager.endLesson()
                                    withContext(Dispatchers.Main) {
                                        accountManager.logout()
                                    }
                                }
                            }
                        )
                        .build()
                        .show()
                }
                else -> {
                    accountManager.logout()
                }
            }
        }
        cssmiQuitClassSwift.setOnClickListener {
            csWindowManager.removeSubWindow(tag)
            when (participationState) {
                ParticipationState.LESSON_STARTED -> {
                    CSSystemDialogWindow.Builder(context)
                        .setTitle(context.getString(R.string.dialog_buttons_end_in_session))
                        .setMessage(context.getString(R.string.dialog_end_lesson_with_quit))
                        .setNegativeButton(
                            text = context.getString(R.string.common_cancel),
                            color = ContextCompat.getColor(context, R.color.color_2E3133),
                            listener = {
                                csWindowManager.removeWindow(WindowTag.CS_NORMAL_DIALOG)
                            }
                        )
                        .setPositiveButton(
                            text = context.getString(R.string.dialog_buttons_end_lesson_quit),
                            color = ContextCompat.getColor(context, R.color.color_F02B2B),
                            listener = {
                                coroutineScope.launch {
                                    toolbarManager.endLesson()
                                    withContext(Dispatchers.Main) {
                                        accountManager.quitApp()
                                    }
                                }
                            }
                        )
                        .build()
                        .show()
                }
                else -> {
                    accountManager.quitApp()
                }
            }
        }
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

    override fun onViewCreated() {
        csWindowManager.addOnWindowChangedListener(this@SettingMenuWindow)
        checkIfStateChanged()
    }

    override fun onDestroy() {
        csWindowManager.getWindow(WindowTag.TOOLBAR)?.let { iWindowContainer ->
            val window: ToolbarWindow = iWindowContainer.customWindow as ToolbarWindow
            window.checkIconButtonSelectedState()
        }
    }

    override fun onCSWindowCountChanged() {
        checkIfStateChanged()
    }

    override fun onCSWindowHiddenCountChange() = Unit

    fun setParticipationState(state: ParticipationState) {
        participationState = state
    }

    private fun checkIfStateChanged() {
        if (csWindowManager.isWindowExisted(WindowTag.SETTING_WINDOW)) {
            binding.cssmiSettings.setItemState(CSSubordinateMenuItem.ItemState.SELECTED)
        } else {
            binding.cssmiSettings.setItemState(CSSubordinateMenuItem.ItemState.NORMAL)
        }
    }
}