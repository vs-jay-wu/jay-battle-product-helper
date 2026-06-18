package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import com.viewsonic.classswift.databinding.WindowUpcomingMaintenanceBinding
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.uimanager.maintenance.MaintenanceAnnouncementsUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject

class UpcomingMaintenanceWindow(private val androidContext: Context) :
    IWindow<WindowUpcomingMaintenanceBinding> {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val maintenanceAnnouncementsUiManager: MaintenanceAnnouncementsUiManager by inject(MaintenanceAnnouncementsUiManager::class.java)

    override var tag: WindowTag = WindowTag.WINDOW_UPCOMING_MAINTENANCE
    override var size: SizeInPixels =
        SizeInPixels(413f.dpToPx().toInt(), LayoutParams.WRAP_CONTENT)
    override val binding: WindowUpcomingMaintenanceBinding = WindowUpcomingMaintenanceBinding.inflate(
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

    override fun onViewCreated() {
        initView()
    }

    private fun initView() {
        with(binding) {
            tvTitle.text = maintenanceAnnouncementsUiManager.getMaintenanceTitle(MaintenanceAnnouncementsUiManager.MaintenancePhase.TWO_DAYS_BEFORE)
            tvDescription.text = maintenanceAnnouncementsUiManager.getMaintenanceDescription(MaintenanceAnnouncementsUiManager.MaintenancePhase.TWO_DAYS_BEFORE)
            ivClose.setOnClickListener {
                csWindowManager.removeWindow(tag)
                csWindowManager.createWindow(
                    get(SelectOrgWindow::class.java),
                    Gravity.CENTER
                )
            }
            buttonGotIt.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    csWindowManager.removeWindow(tag)
                    csWindowManager.createWindow(
                        get(SelectOrgWindow::class.java),
                        Gravity.CENTER
                    )
                }
            })
        }
    }
}