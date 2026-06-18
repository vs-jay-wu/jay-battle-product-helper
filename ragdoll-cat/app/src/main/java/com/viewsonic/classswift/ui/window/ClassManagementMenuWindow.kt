package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.databinding.WindowClassManagementMenuBinding
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.helper.JoinClassWindowOpener
import com.viewsonic.classswift.ui.widget.toolbar.CSSubordinateMenuItem
import com.viewsonic.classswift.ui.window.leaderboard.LeaderboardWindow
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnCSWindowChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject

class ClassManagementMenuWindow(context: Context) : IWindow<WindowClassManagementMenuBinding>,
    OnCSWindowChangedListener {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)

    override var tag: WindowTag = WindowTag.CLASS_MANAGEMENT_MENU
    override var size: SizeInPixels = SizeInPixels(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var isPremiumUser = false

    init {
        isPremiumUser = accountManager.selectedOrg?.isPremiumUser == true
    }

    override val binding: WindowClassManagementMenuBinding = WindowClassManagementMenuBinding.inflate(
        LayoutInflater.from(context)
    ).apply {
        cssmiStudentManagement.setOnClickListener {
            coroutineScope.launch(Dispatchers.Main) {
                csWindowManager.getWindow(WindowTag.JOIN_CLASS)?.hoistWindowZOrder() ?: run {
                    JoinClassWindowOpener.open(get(JoinClassWindow::class.java))
                }
                csWindowManager.removeSubWindow(tag)
            }
        }

        cssmiLeaderboard.setOnClickListener {
            if (!isPremiumUser) return@setOnClickListener
            coroutineScope.launch(Dispatchers.Main) {
                csWindowManager.getWindow(WindowTag.LEADERBOARD_WINDOW)?.hoistWindowZOrder() ?: run {
                    val window: LeaderboardWindow = get(LeaderboardWindow::class.java)
                    csWindowManager.createWindow(
                        window, Gravity.CENTER
                    )
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.LEADERBOARD_CLICKED)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                        .send()
                }
                csWindowManager.removeSubWindow(tag)
            }
        }
    }

    override fun onViewCreated() {
        csWindowManager.addOnWindowChangedListener(this@ClassManagementMenuWindow)
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
        csWindowManager.removeOnWindowChangedListener(this@ClassManagementMenuWindow)
        csWindowManager.getWindow(WindowTag.TOOLBAR)?.let { iWindowContainer ->
            val window: ToolbarWindow = iWindowContainer.customWindow as ToolbarWindow
            window.checkIconButtonSelectedState()
        }
    }

    override fun onCSWindowCountChanged() {
        checkIfStateChanged()
    }

    override fun onCSWindowHiddenCountChange() = Unit

    fun setLeaderboardItemState(itemState: CSSubordinateMenuItem.ItemState) {
        binding.cssmiLeaderboard.setItemState(itemState)
    }

    private fun checkIfStateChanged() {
        if (csWindowManager.isWindowExisted(WindowTag.STUDENT_MANAGEMENT)) {
            binding.cssmiStudentManagement.setItemState(CSSubordinateMenuItem.ItemState.SELECTED)
        } else {
            binding.cssmiStudentManagement.setItemState(CSSubordinateMenuItem.ItemState.NORMAL)
        }

        if (csWindowManager.isWindowExisted(WindowTag.LEADERBOARD_WINDOW)) {
            binding.cssmiLeaderboard.setItemState(CSSubordinateMenuItem.ItemState.SELECTED)
        } else {
            if (isPremiumUser) {
                binding.cssmiLeaderboard.setItemState(CSSubordinateMenuItem.ItemState.NORMAL)
            } else {
                binding.cssmiLeaderboard.setItemState(CSSubordinateMenuItem.ItemState.NEED_TO_UPGRADE)
            }
        }
    }
}