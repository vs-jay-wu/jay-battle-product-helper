package com.viewsonic.classswift.designershell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionScreen
import com.viewsonic.classswift.feature.servicescreens.ui.BuzzerScreen
import com.viewsonic.classswift.feature.servicescreens.ui.ClassManagementMenu
import com.viewsonic.classswift.feature.servicescreens.ui.ComingSoonPrompt
import com.viewsonic.classswift.feature.servicescreens.ui.JoinClassScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MyClassScreen
import com.viewsonic.classswift.feature.servicescreens.ui.QuizMenu
import com.viewsonic.classswift.feature.servicescreens.ui.RandomDrawScreen
import com.viewsonic.classswift.feature.servicescreens.ui.SelectOrgAndClassScreen
import com.viewsonic.classswift.feature.servicescreens.ui.SelectOrgScreen
import com.viewsonic.classswift.feature.servicescreens.ui.SettingMenu
import com.viewsonic.classswift.feature.servicescreens.ui.SettingsScreen
import com.viewsonic.classswift.feature.servicescreens.ui.ToolbarScreen
import com.viewsonic.classswift.feature.servicescreens.ui.SpinnerScreen
import com.viewsonic.classswift.feature.servicescreens.ui.StudentManagementScreen
import com.viewsonic.classswift.feature.servicescreens.ui.TimerToolScreen
import com.viewsonic.classswift.feature.servicescreens.ui.ToolsMenu
import com.viewsonic.classswift.feature.servicescreens.ui.UnderMaintenanceScreen
import com.viewsonic.classswift.feature.servicescreens.ui.UpcomingMaintenanceCornerPrompt
import com.viewsonic.classswift.feature.servicescreens.ui.UpcomingMaintenanceScreen
import com.viewsonic.classswift.feature.servicescreens.ui.UpgradePrompt
import com.viewsonic.classswift.fixtures.Samples
import com.viewsonic.designer.bridge.DesignerPage
import com.viewsonic.designer.bridge.runDesignerTarget

/** Service screens are dialog-sized cards; center them on a dim backdrop like the live overlay. */
@Composable
private fun Dialog(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xFFEDEFF1)).padding(40.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * ragdoll-cat's Compose target for the Designer Shell — now just a list of pages
 * handed to the reusable :designer-bridge runtime. Any repo can do the same:
 * depend on :designer-bridge and call runDesignerTarget with its own screens.
 */
fun main() = runDesignerTarget(
    title = "Compose Target · ragdoll-cat",
    sourceDirs = listOf("feature", "core"),
    pages = listOf(
        DesignerPage("qc_populated", "Quiz Collection · Populated") { QuizCollectionScreen(Samples.populated, onEvent = {}) },
        DesignerPage("qc_empty", "Quiz Collection · Empty") { QuizCollectionScreen(Samples.empty, onEvent = {}) },
        DesignerPage("qc_loading", "Quiz Collection · Loading") { QuizCollectionScreen(Samples.loading, onEvent = {}) },
        DesignerPage("qc_error", "Quiz Collection · Error") { QuizCollectionScreen(Samples.error, onEvent = {}) },
        DesignerPage("svc_under_maintenance", "Service · Under Maintenance") { Dialog { UnderMaintenanceScreen() } },
        DesignerPage("svc_upcoming_maintenance", "Service · Upcoming Maintenance") { Dialog { UpcomingMaintenanceScreen() } },
        DesignerPage("svc_upcoming_corner", "Service · Upcoming Maintenance (corner)") { Dialog { UpcomingMaintenanceCornerPrompt() } },
        DesignerPage("svc_coming_soon", "Service · Coming Soon Prompt") { Dialog { ComingSoonPrompt() } },
        DesignerPage("svc_upgrade", "Service · Upgrade Prompt") { Dialog { UpgradePrompt() } },
        DesignerPage("svc_setting_menu", "Service · Setting Menu") { Dialog { SettingMenu() } },
        DesignerPage("svc_tools_menu", "Service · Tools Menu") { Dialog { ToolsMenu() } },
        DesignerPage("svc_quiz_menu", "Service · Quiz Menu") { Dialog { QuizMenu() } },
        DesignerPage("svc_class_menu", "Service · Class Management Menu") { Dialog { ClassManagementMenu() } },
        DesignerPage("svc_select_org", "Service · Select Organization") { Dialog { SelectOrgScreen() } },
        DesignerPage("svc_select_org_class", "Service · Select Org & Class") { Dialog { SelectOrgAndClassScreen() } },
        DesignerPage("svc_my_class", "Service · My Class") { Dialog { MyClassScreen() } },
        DesignerPage("svc_student_mgmt", "Service · Student Management") { Dialog { StudentManagementScreen() } },
        DesignerPage("svc_join_class", "Service · Join Class") { Dialog { JoinClassScreen() } },
        DesignerPage("svc_buzzer", "Service · Buzzer") { Dialog { BuzzerScreen() } },
        DesignerPage("svc_random_draw", "Service · Random Draw") { Dialog { RandomDrawScreen() } },
        DesignerPage("svc_timer", "Service · Timer") { Dialog { TimerToolScreen() } },
        DesignerPage("svc_spinner", "Service · Spinner") { Dialog { SpinnerScreen() } },
        DesignerPage("svc_settings", "Service · Settings") { Dialog { SettingsScreen() } },
        DesignerPage("svc_toolbar", "Service · Toolbar") { Dialog { ToolbarScreen() } },
    ),
)
