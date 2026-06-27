package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.WindowManager.LayoutParams
import androidx.compose.runtime.Composable
import com.viewsonic.classswift.feature.servicescreens.ui.UnderMaintenanceScreen
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.window.compose.toAnnotatedString
import com.viewsonic.classswift.ui.windowmodel.tool.UnderMaintenanceWindowModel
import com.viewsonic.classswift.uimanager.maintenance.MaintenanceAnnouncementsUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import org.koin.java.KoinJavaComponent.inject

class UnderMaintenanceWindow(androidContext: Context) : ComposeHostWindow(androidContext) {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val underMaintenanceWindowModel: UnderMaintenanceWindowModel by inject(UnderMaintenanceWindowModel::class.java)
    private val maintenanceAnnouncementsUiManager: MaintenanceAnnouncementsUiManager by inject(MaintenanceAnnouncementsUiManager::class.java)

    override var tag: WindowTag = WindowTag.WINDOW_UNDER_MAINTENANCE
    override var size: SizeInPixels = SizeInPixels(413f.dpToPx().toInt(), LayoutParams.WRAP_CONTENT)

    // Card wraps its content (size.height = WRAP_CONTENT renders it exactly); this fixed estimate
    // is only used for centering, so we don't measure the detached ComposeView.
    override fun getCurrentSize(): SizeInPixels =
        SizeInPixels(413f.dpToPx().toInt(), 360f.dpToPx().toInt())

    override fun onCreate() {
        csWindowManager.hideAllWindows(listOf(tag))
        underMaintenanceWindowModel.checkIfNeedToEndLesson()
    }

    @Composable
    override fun Content() {
        val phase = MaintenanceAnnouncementsUiManager.MaintenancePhase.DURING_DOWNTIME
        UnderMaintenanceScreen(
            title = maintenanceAnnouncementsUiManager.getMaintenanceTitle(phase),
            description = maintenanceAnnouncementsUiManager.getMaintenanceDescription(phase).toAnnotatedString(),
            onClose = { closeAndQuit() },
            onGotIt = { closeAndQuit() },
        )
    }

    private fun closeAndQuit() {
        csWindowManager.removeWindow(tag)
        accountManager.quitApp()
    }
}
