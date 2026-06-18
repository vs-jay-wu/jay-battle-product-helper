package com.viewsonic.classswift.designershell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import com.viewsonic.classswift.core.ui.ScreenSpec
import com.viewsonic.classswift.core.ui.ScreenState
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionScreen
import com.viewsonic.classswift.fixtures.Samples

/**
 * Phase 1 Designer Shell entry point.
 *
 * The canvas hosts the real [QuizCollectionScreen] in-process; a StatePicker flips between
 * the loading / empty / error / populated fixtures. Edit QuizCollectionScreen.kt (or the
 * tokens in AppTheme.kt) and save to see it hot-reload here.
 *
 * Run:  ./gradlew :designer-shell:hotRunJvm --auto
 *
 * Phase 2 grows this shell out (session list, prompt input, inspector). For now it is the
 * canvas + StatePicker, driven by an app-agnostic ScreenRegistry.
 */
private val quizCollectionSpec = ScreenSpec(
    name = "Quiz Collection",
    states = listOf(
        ScreenState("Populated") { QuizCollectionScreen(Samples.populated, onEvent = {}) },
        ScreenState("Empty") { QuizCollectionScreen(Samples.empty, onEvent = {}) },
        ScreenState("Loading") { QuizCollectionScreen(Samples.loading, onEvent = {}) },
        ScreenState("Error") { QuizCollectionScreen(Samples.error, onEvent = {}) },
    ),
)

fun main() = singleWindowApplication(title = "Designer Shell — Phase 1") {
    ShellCanvas(quizCollectionSpec)
}

@Composable
private fun ShellCanvas(spec: ScreenSpec) {
    var selected by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${spec.name}:")
            spec.states.forEachIndexed { index, state ->
                FilterChip(
                    selected = index == selected,
                    onClick = { selected = index },
                    label = { Text(state.label) },
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            spec.states[selected].content()
        }
    }
}
