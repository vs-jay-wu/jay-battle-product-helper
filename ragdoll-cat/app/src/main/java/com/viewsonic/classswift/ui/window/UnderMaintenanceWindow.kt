package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import com.viewsonic.classswift.databinding.WindowUnderMaintenanceBinding
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.windowmodel.tool.UnderMaintenanceWindowModel
import com.viewsonic.classswift.uimanager.maintenance.MaintenanceAnnouncementsUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import org.koin.java.KoinJavaComponent.inject

class UnderMaintenanceWindow(private val androidContext: Context) :
    IWindow<WindowUnderMaintenanceBinding> {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val underMaintenanceWindowModel: UnderMaintenanceWindowModel by inject(UnderMaintenanceWindowModel::class.java)
    private val maintenanceAnnouncementsUiManager: MaintenanceAnnouncementsUiManager by inject(MaintenanceAnnouncementsUiManager::class.java)

    override var tag: WindowTag = WindowTag.WINDOW_UNDER_MAINTENANCE
    override var size: SizeInPixels =
        SizeInPixels(413f.dpToPx().toInt(), LayoutParams.WRAP_CONTENT)
    override val binding: WindowUnderMaintenanceBinding = WindowUnderMaintenanceBinding.inflate(
        LayoutInflater.from(androidContext)
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(413f.dpToPx().toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onCreate() {
        csWindowManager.hideAllWindows(listOf(tag))
        underMaintenanceWindowModel.checkIfNeedToEndLesson()
    }

    override fun onViewCreated() {
        initView()
    }

    private fun initView() {
        with(binding) {
            tvTitle.text = maintenanceAnnouncementsUiManager.getMaintenanceTitle(MaintenanceAnnouncementsUiManager.MaintenancePhase.DURING_DOWNTIME)
            tvDescription.text = maintenanceAnnouncementsUiManager.getMaintenanceDescription(MaintenanceAnnouncementsUiManager.MaintenancePhase.DURING_DOWNTIME)
            ivClose.setOnClickListener {
                csWindowManager.removeWindow(tag)
                accountManager.quitApp()
            }
            cslbGotIt.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    csWindowManager.removeWindow(tag)
                    accountManager.quitApp()
                }
            })
        }
    }
}