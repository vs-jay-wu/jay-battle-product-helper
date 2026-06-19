package com.viewsonic.classswift.designershell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import com.viewsonic.classswift.core.designsystem.AppTheme
import com.viewsonic.classswift.core.ui.DesignCanvas
import com.viewsonic.classswift.core.ui.DesignNodeInfo
import com.viewsonic.classswift.core.ui.ScreenSpec
import com.viewsonic.classswift.core.ui.ScreenState
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionScreen
import com.viewsonic.classswift.fixtures.Samples
import kotlin.math.roundToInt

/**
 * Phase 2 Designer Shell: Figma-like canvas with design / interactive modes.
 *
 * Design mode: click an element on the canvas to select it — a selection outline is drawn and
 * the right Inspector panel shows its node id + box (width/height/x/y). This selection is what
 * gets fed to Claude Code alongside the prompt (Phase 3+). Interactive mode lets the screen's
 * own clicks/navigation run (mock data).
 *
 * Run:  ./gradlew :designer-shell:hotRunJvm --auto
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

fun main() = singleWindowApplication(title = "Designer Shell — Phase 2") {
    Shell(quizCollectionSpec)
}

@Composable
private fun Shell(spec: ScreenSpec) {
    val tokens = AppTheme.tokens
    var designMode by remember { mutableStateOf(true) }
    var stateIndex by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<DesignNodeInfo?>(null) }

    Column(Modifier.fillMaxSize().background(tokens.colors.neutral100)) {
        TopBar(
            designMode = designMode,
            onModeChange = { designMode = it },
            spec = spec,
            stateIndex = stateIndex,
            onStateChange = { stateIndex = it; selected = null },
        )
        Row(Modifier.weight(1f).fillMaxWidth()) {
            DesignCanvas(
                designMode = designMode,
                selectedId = selected?.id,
                onSelect = { selected = it },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                spec.states[stateIndex].content()
            }
            InspectorPanel(designMode, selected, Modifier.width(248.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun TopBar(
    designMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    spec: ScreenSpec,
    stateIndex: Int,
    onStateChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(selected = designMode, onClick = { onModeChange(true) }, label = { Text("設計") })
        FilterChip(selected = !designMode, onClick = { onModeChange(false) }, label = { Text("互動") })
        Spacer(Modifier.width(8.dp))
        Text("${spec.name}:")
        spec.states.forEachIndexed { index, state ->
            FilterChip(
                selected = index == stateIndex,
                onClick = { onStateChange(index) },
                label = { Text(state.label) },
            )
        }
    }
}

@Composable
private fun InspectorPanel(designMode: Boolean, selected: DesignNodeInfo?, modifier: Modifier) {
    val tokens = AppTheme.tokens
    val density = LocalDensity.current.density
    Column(modifier.background(tokens.colors.neutral0).padding(12.dp)) {
        Text("Inspector", color = tokens.colors.neutral900, fontSize = tokens.type.lg)
        Spacer(Modifier.height(8.dp))
        when {
            !designMode ->
                Text("互動模式：點擊會觸發元件行為", color = tokens.colors.neutral650, fontSize = tokens.type.md)
            selected == null ->
                Text("點選畫布上的元件以檢視", color = tokens.colors.neutral650, fontSize = tokens.type.md)
            else -> {
                InfoRow("node id", selected.id, tokens)
                InfoRow("width", "${(selected.rect.width / density).roundToInt()} dp", tokens)
                InfoRow("height", "${(selected.rect.height / density).roundToInt()} dp", tokens)
                InfoRow("x", "${(selected.rect.left / density).roundToInt()} dp", tokens)
                InfoRow("y", "${(selected.rect.top / density).roundToInt()} dp", tokens)
                Spacer(Modifier.height(8.dp))
                Text(
                    "padding / margin：之後以 designNode 標註補上",
                    color = tokens.colors.neutral500,
                    fontSize = tokens.type.xs,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    tokens: com.viewsonic.classswift.core.designsystem.AppTokens,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = tokens.colors.neutral650, fontSize = tokens.type.md)
        Text(value, color = tokens.colors.neutral900, fontSize = tokens.type.md)
    }
}
