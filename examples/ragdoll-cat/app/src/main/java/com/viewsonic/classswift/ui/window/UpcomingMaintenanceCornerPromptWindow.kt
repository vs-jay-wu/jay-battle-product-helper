package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import com.viewsonic.classswift.databinding.WindowUpcomingMaintenanceCornerPromptBinding
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.uimanager.maintenance.MaintenanceAnnouncementsUiManager
import com.viewsonic.classswift.utils.DisplayUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import org.koin.java.KoinJavaComponent.inject

class UpcomingMaintenanceCornerPromptWindow(private val androidContext: Context) :
    IWindow<WindowUpcomingMaintenanceCornerPromptBinding> {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val maintenanceAnnouncementsUiManager: MaintenanceAnnouncementsUiManager by inject(MaintenanceAnnouncementsUiManager::class.java)

    override var tag: WindowTag = WindowTag.WINDOW_UPCOMING_MAINTENANCE_CORNER_PROMPT
    override var size: SizeInPixels =
        SizeInPixels(360f.dpToPx().toInt(), LayoutParams.WRAP_CONTENT)
    override val binding: WindowUpcomingMaintenanceCornerPromptBinding = WindowUpcomingMaintenanceCornerPromptBinding.inflate(
        LayoutInflater.from(androidContext)
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(360f.dpToPx().toInt(), View.MeasureSpec.EXACTLY),
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
            tvTitle.text = maintenanceAnnouncementsUiManager.getMaintenanceTitle(MaintenanceAnnouncementsUiManager.MaintenancePhase.FIVE_MINUTES_BEFORE)
            tvDescription.text = maintenanceAnnouncementsUiManager.getMaintenanceDescription(MaintenanceAnnouncementsUiManager.MaintenancePhase.FIVE_MINUTES_BEFORE)
            cslbGotIt.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    maintenanceAnnouncementsUiManager.viewedFiveMinutesBeforeMaintenanceAnnouncement()
                    csWindowManager.removeWindow(tag)
                }
            })
        }
    }

    fun getInitLocation(): Location {
        initView()
        val size = getCurrentSize()
        val (deviceWidth, deviceHeight) = DisplayUtils.getScreenSize(isIgnoreNavigationBars = true, isIgnoreStatusBars = true)
        val locationX = deviceWidth - size.width
        val locationY = deviceHeight - size.height
        return Location(locationX, locationY)
    }
}