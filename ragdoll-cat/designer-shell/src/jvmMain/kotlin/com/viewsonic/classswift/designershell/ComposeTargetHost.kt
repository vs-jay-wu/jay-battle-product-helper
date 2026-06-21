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
import com.viewsonic.classswift.feature.servicescreens.ui.BatchQuizResultList
import com.viewsonic.classswift.feature.servicescreens.ui.BuzzerScreen
import com.viewsonic.classswift.feature.servicescreens.ui.CSSystemDialogScreen
import com.viewsonic.classswift.feature.servicescreens.ui.CropImageScreen
import com.viewsonic.classswift.feature.servicescreens.ui.LeaderboardScreen
import com.viewsonic.classswift.feature.servicescreens.ui.PushRespondScreen
import com.viewsonic.classswift.feature.servicescreens.ui.ToastScreen
import com.viewsonic.classswift.feature.servicescreens.ui.ComingSoonPrompt
import com.viewsonic.classswift.feature.servicescreens.ui.JoinClassScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizEditScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizStartScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.feature.servicescreens.ui.RandomDrawScreen
import com.viewsonic.classswift.feature.servicescreens.ui.SelectOrgAndClassScreen
import com.viewsonic.classswift.feature.servicescreens.ui.ToolbarScreen
import com.viewsonic.classswift.feature.servicescreens.ui.SpinnerHeader
import com.viewsonic.classswift.feature.servicescreens.ui.StudentManagementScreen
import com.viewsonic.classswift.feature.servicescreens.ui.TimerToolScreen
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
        DesignerPage("svc_select_org_class", "Service · Select Org & Class") { Dialog { SelectOrgAndClassScreen() } },
        DesignerPage("svc_student_mgmt", "Service · Student Management") { Dialog { StudentManagementScreen() } },
        DesignerPage("svc_join_class", "Service · Join Class") { Dialog { JoinClassScreen() } },
        DesignerPage("svc_buzzer", "Service · Buzzer") { Dialog { BuzzerScreen() } },
        DesignerPage("svc_random_draw", "Service · Random Draw") { Dialog { RandomDrawScreen() } },
        DesignerPage("svc_timer", "Service · Timer") { Dialog { TimerToolScreen() } },
        DesignerPage("svc_spinner", "Service · Spinner") { Dialog { SpinnerHeader() } },
        DesignerPage("svc_toolbar", "Service · Toolbar") { Dialog { ToolbarScreen() } },
        DesignerPage("svc_quiz_mc", "Quiz Start · Multiple Choice") { Dialog { MvbQuizStartScreen(MvbQuizType.MULTIPLE_CHOICE) } },
        DesignerPage("svc_quiz_tf", "Quiz Start · True/False") { Dialog { MvbQuizStartScreen(MvbQuizType.TRUE_FALSE) } },
        DesignerPage("svc_quiz_sa", "Quiz Start · Short Answer") { Dialog { MvbQuizStartScreen(MvbQuizType.SHORT_ANSWER) } },
        DesignerPage("svc_quiz_poll", "Quiz Start · Poll") { Dialog { MvbQuizStartScreen(MvbQuizType.POLL) } },
        DesignerPage("svc_quiz_audio", "Quiz Start · Audio") { Dialog { MvbQuizStartScreen(MvbQuizType.AUDIO) } },
        DesignerPage("svc_quiz_sketch", "Quiz Start · Sketch Response") { Dialog { MvbQuizStartScreen(MvbQuizType.SKETCH) } },
        DesignerPage("svc_quiz_text_sa", "Quiz Start · Short Answer (Text)") { Dialog { MvbQuizStartScreen(MvbQuizType.TEXT_SHORT_ANSWER) } },
        DesignerPage("svc_quiz_text_tf", "Quiz Start · True/False (Text)") { Dialog { MvbQuizStartScreen(MvbQuizType.TEXT_TRUE_FALSE) } },
        DesignerPage("svc_edit_mc", "Quiz Edit · Multiple Choice") { Dialog { MvbQuizEditScreen(MvbQuizType.MULTIPLE_CHOICE) } },
        DesignerPage("svc_edit_tf", "Quiz Edit · True/False") { Dialog { MvbQuizEditScreen(MvbQuizType.TRUE_FALSE) } },
        DesignerPage("svc_edit_sa", "Quiz Edit · Short Answer") { Dialog { MvbQuizEditScreen(MvbQuizType.SHORT_ANSWER) } },
        DesignerPage("svc_edit_poll", "Quiz Edit · Poll") { Dialog { MvbQuizEditScreen(MvbQuizType.POLL) } },
        DesignerPage("svc_edit_audio", "Quiz Edit · Audio") { Dialog { MvbQuizEditScreen(MvbQuizType.AUDIO) } },
        DesignerPage("svc_edit_sketch", "Quiz Edit · Sketch Response") { Dialog { MvbQuizEditScreen(MvbQuizType.SKETCH) } },
        DesignerPage("svc_batch_result", "Result · Batch Quiz Result") { Dialog { BatchQuizResultList() } },
        DesignerPage("svc_leaderboard", "Result · Leaderboard") { Dialog { LeaderboardScreen() } },
        DesignerPage("svc_push_respond", "Result · Push Respond") { Dialog { PushRespondScreen() } },
        DesignerPage("svc_system_dialog", "Misc · System Dialog") { Dialog { CSSystemDialogScreen() } },
        DesignerPage("svc_toast", "Misc · Toast") { Dialog { ToastScreen() } },
        DesignerPage("svc_crop_image", "Misc · Crop Image") { Dialog { CropImageScreen() } },
    ),
)
