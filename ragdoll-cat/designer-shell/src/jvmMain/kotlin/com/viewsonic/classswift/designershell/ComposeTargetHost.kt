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
import com.viewsonic.classswift.feature.servicescreens.ui.UnderMaintenanceScreen
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
    ),
)
