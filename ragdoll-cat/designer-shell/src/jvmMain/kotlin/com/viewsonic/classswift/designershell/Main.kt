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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import com.viewsonic.classswift.core.designsystem.AppTheme
import com.viewsonic.classswift.core.designsystem.AppTokens
import com.viewsonic.classswift.core.ui.DesignCanvas
import com.viewsonic.classswift.core.ui.DesignNodeInfo
import com.viewsonic.classswift.core.ui.ScreenSpec
import com.viewsonic.classswift.core.ui.ScreenState
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionScreen
import com.viewsonic.classswift.fixtures.Samples
import java.io.File
import kotlin.math.roundToInt

/**
 * Designer Shell: a Figma-like canvas (left) + Inspector & Claude panel (right).
 *
 * Design mode: click a component to select it (node id + box model in the Inspector). Type a
 * request in the Claude panel and send — the selected node id is appended, the prompt is streamed
 * to an interactive `claude` session running in this repo, Claude edits :feature:quizcollection:ui,
 * and `hotRunJvm --auto` hot-reloads the canvas.
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

fun main() {
    captureCanvasStates(quizCollectionSpec)
    singleWindowApplication(title = "Designer Shell") {
        Shell(quizCollectionSpec)
    }
}

/**
 * Self-screenshot: render each screen state off-screen to a PNG (no screen-recording permission
 * needed). Used to verify the canvas headlessly. Writes /tmp/canvas-<state>.png.
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun captureCanvasStates(spec: ScreenSpec) {
    spec.states.forEach { state ->
        runCatching {
            val scene = ImageComposeScene(width = 1000, height = 760, density = Density(2f)) {
                state.content()
            }
            repeat(8) { scene.render() } // let async resource (compose.resources) loads settle
            scene.render().encodeToData()?.let { data ->
                File("/tmp/canvas-${state.label}.png").writeBytes(data.bytes)
            }
            scene.close()
        }
    }
}

@Composable
private fun Shell(spec: ScreenSpec) {
    val tokens = AppTheme.tokens
    var designMode by remember { mutableStateOf(true) }
    var stateIndex by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<DesignNodeInfo?>(null) }
    val session = remember { ClaudeSession(resolveRepoRoot()) }

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
            Column(Modifier.width(380.dp).fillMaxHeight().background(tokens.colors.neutral0)) {
                InspectorPanel(designMode, selected, Modifier.fillMaxWidth())
                HorizontalDivider(color = tokens.colors.neutral300)
                ClaudePanel(session, selected?.id, Modifier.weight(1f).fillMaxWidth())
            }
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
    Column(modifier.padding(12.dp)) {
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
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, tokens: AppTokens) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = tokens.colors.neutral650, fontSize = tokens.type.md)
        Text(value, color = tokens.colors.neutral900, fontSize = tokens.type.md)
    }
}

@Composable
private fun ClaudePanel(session: ClaudeSession, selectedNodeId: String?, modifier: Modifier) {
    val tokens = AppTheme.tokens
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(session.transcript.size) {
        if (session.transcript.isNotEmpty()) listState.animateScrollToItem(session.transcript.lastIndex)
    }

    Column(modifier.padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Claude", color = tokens.colors.neutral900, fontSize = tokens.type.lg, modifier = Modifier.weight(1f))
            Text(
                if (session.running) "session ${session.sessionId.take(8)}" else "—",
                color = tokens.colors.neutral650,
                fontSize = tokens.type.xs,
            )
            TextButton(onClick = { session.start() }) { Text("New session") }
        }
        Spacer(Modifier.height(4.dp))

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState) {
            items(session.transcript) { m -> MessageRow(m, tokens) }
        }

        selectedNodeId?.let {
            Text("目標元件：$it", color = tokens.colors.primary, fontSize = tokens.type.xs)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("描述想要的變更…") },
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val text = input.trim()
                    if (text.isEmpty()) return@Button
                    if (!session.running) session.start()
                    session.send(buildPrompt(text, selectedNodeId))
                    input = ""
                },
            ) { Text("送出") }
        }
    }
}

@Composable
private fun MessageRow(message: ClaudeMessage, tokens: AppTokens) {
    val (label, color) = when (message.role) {
        ClaudeRole.USER -> "你" to tokens.colors.primary
        ClaudeRole.ASSISTANT -> "Claude" to tokens.colors.neutral900
        ClaudeRole.TOOL -> "tool" to tokens.colors.neutral650
        ClaudeRole.RESULT -> "✓" to tokens.colors.neutral650
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = color, fontSize = tokens.type.xs, fontWeight = FontWeight.Bold)
        Text(message.text, color = tokens.colors.neutral900, fontSize = tokens.type.sm)
    }
}

private fun buildPrompt(text: String, nodeId: String?): String =
    if (nodeId == null) {
        text
    } else {
        "$text\n\n(目標元件已用 Modifier.designNode(\"$nodeId\") 標記在 :feature:quizcollection:ui 模組，" +
            "請用它定位要修改的 composable。)"
    }

/** Walk up from the run dir to the Gradle root (so `claude` runs in the ragdoll-cat repo). */
private fun resolveRepoRoot(): File {
    var dir: File? = File(System.getProperty("user.dir"))
    while (dir != null) {
        if (File(dir, "settings.gradle.kts").exists()) return dir
        dir = dir.parentFile
    }
    return File(System.getProperty("user.dir"))
}
