package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.WindowManager.LayoutParams
import androidx.compose.runtime.Composable
import com.viewsonic.classswift.feature.servicescreens.ui.UpcomingMaintenanceScreen
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.window.compose.toAnnotatedString
import com.viewsonic.classswift.uimanager.maintenance.MaintenanceAnnouncementsUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject

class UpcomingMaintenanceWindow(androidContext: Context) : ComposeHostWindow(androidContext) {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val maintenanceAnnouncementsUiManager: MaintenanceAnnouncementsUiManager by inject(MaintenanceAnnouncementsUiManager::class.java)

    override var tag: WindowTag = WindowTag.WINDOW_UPCOMING_MAINTENANCE
    override var size: SizeInPixels = SizeInPixels(413f.dpToPx().toInt(), LayoutParams.WRAP_CONTENT)

    override fun getCurrentSize(): SizeInPixels =
        SizeInPixels(413f.dpToPx().toInt(), 360f.dpToPx().toInt())

    @Composable
    override fun Content() {
        val phase = MaintenanceAnnouncementsUiManager.MaintenancePhase.TWO_DAYS_BEFORE
        UpcomingMaintenanceScreen(
            title = maintenanceAnnouncementsUiManager.getMaintenanceTitle(phase),
            description = maintenanceAnnouncementsUiManager.getMaintenanceDescription(phase).toAnnotatedString(),
            onClose = { dismissToSelectOrg() },
            onGotIt = { dismissToSelectOrg() },
        )
    }

    private fun dismissToSelectOrg() {
        csWindowManager.removeWindow(tag)
        csWindowManager.createWindow(get(SelectOrgWindow::class.java), Gravity.CENTER)
    }
}
