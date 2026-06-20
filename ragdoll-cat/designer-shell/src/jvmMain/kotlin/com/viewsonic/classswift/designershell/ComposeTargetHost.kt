package com.viewsonic.classswift.designershell

import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionScreen
import com.viewsonic.classswift.fixtures.Samples
import com.viewsonic.designer.bridge.DesignerPage
import com.viewsonic.designer.bridge.runDesignerTarget

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
    ),
)
