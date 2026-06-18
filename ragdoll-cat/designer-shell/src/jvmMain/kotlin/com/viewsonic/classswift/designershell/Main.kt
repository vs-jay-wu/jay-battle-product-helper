package com.viewsonic.classswift.designershell

import androidx.compose.ui.window.singleWindowApplication
import com.viewsonic.classswift.feature.playground.ui.PlaygroundScreen
import com.viewsonic.classswift.fixtures.Samples

/**
 * Phase 0 entry point for the Designer Shell (Compose Desktop).
 *
 * For now it just renders the playground screen from mock data to prove the
 * scaffold + hot reload loop. Phase 1+ grows this into the Figma-like shell:
 * left session list, bottom prompt input, right inspector, and a canvas that
 * hosts target screens via a ScreenRegistry.
 *
 * Run with hot reload:  ./gradlew :designer-shell:hotRunJvm --auto
 */
fun main() = singleWindowApplication(title = "Designer Shell — Phase 0") {
    PlaygroundScreen(state = Samples.playgroundPopulated)
}
