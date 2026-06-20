package com.viewsonic.classswift.designershell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.singleWindowApplication
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionScreen
import com.viewsonic.classswift.fixtures.Samples
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedWriter
import java.net.ServerSocket

/**
 * Out-of-process Compose target for the standalone Designer Shell. Renders the
 * real ragdoll CMP screen, and in design mode hit-tests a tap to the composable
 * + its source file:line (ui-tooling-data) — the Compose analogue of Flutter's
 * creationLocation. Talks to the shell over a line-delimited JSON TCP socket;
 * CHR (`hotRunJvm` / `reload`) provides hot reload.
 */

// StateFlows so writes from the IPC thread reliably trigger recomposition
// (a bare mutableStateOf written off the UI thread may not notify observers).
private val designModeFlow = MutableStateFlow(false)
private val highlightFlow = MutableStateFlow<HitResult?>(null)

/** Immutable snapshot of located target composables, rebuilt on the UI thread
 *  after each composition (reading the live slot table off-thread crashes). */
@Volatile private var cachedGroups: List<HitResult> = emptyList()

private data class HitResult(val name: String, val file: String, val line: Int, val x: Int, val y: Int, val w: Int, val h: Int)

fun main() {
    val port = Ipc.start()
    println("HOST_READY port=$port")
    Ipc.onCommand = ::handleCommand
    singleWindowApplication(title = "Compose Target · ragdoll-cat") {
        val designMode by designModeFlow.collectAsState()
        Box(Modifier.fillMaxSize().background(Color.White)) {
            Capture {
                QuizCollectionScreen(Samples.populated, onEvent = {})
            }
            if (designMode) DesignOverlay()
        }
    }
}

@OptIn(InternalComposeApi::class)
@Composable
private fun Capture(content: @Composable () -> Unit) {
    currentComposer.collectParameterInformation()
    val data = currentComposer.compositionData
    // Poll the composition on the UI thread (after layout, when boxes are real and the
    // slot table is idle between frames). Reading off-thread or pre-layout fails, so we
    // cache an immutable snapshot here; hit-testing reads the cache from any thread.
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            val snap = snapshotLocated(data)
            if (snap.isNotEmpty()) cachedGroups = snap
        }
    }
    content()
}

@OptIn(UiToolingDataApi::class)
private fun snapshotLocated(data: CompositionData): List<HitResult> = runCatching {
    val out = mutableListOf<HitResult>()
    var totalLocated = 0
    fun walk(g: Group) {
        val loc = g.location
        val file = loc?.sourceFile
        val name = g.name
        if (file != null && name != null && g.box.width > 0 && g.box.height > 0) {
            totalLocated++
            if (file in targetFiles) {
                out.add(HitResult(name, file, loc.lineNumber, g.box.left, g.box.top, g.box.width, g.box.height))
            }
        }
        g.children.forEach(::walk)
    }
    walk(data.asTree())
    if (out.isEmpty()) println("[host] snapshot: located=$totalLocated target=${out.size} files=$targetFiles")
    out
}.getOrElse { println("[host] snapshot asTree error: $it"); cachedGroups }

@Composable
private fun DesignOverlay() {
    // Absorb every pointer event; on press, hit-test the captured screen tree.
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    // Detect the press BEFORE consuming — changedToDown() returns false once consumed.
                    val down = event.changes.firstOrNull { it.changedToDown() }
                    event.changes.forEach { it.consume() }
                    if (down != null) onTap(down.position)
                }
            }
        },
    ) {
        val density = LocalDensity.current
        val highlight by highlightFlow.collectAsState()
        highlight?.let { h ->
            Box(
                Modifier.offset { IntOffset(h.x, h.y) }
                    .size(with(density) { h.w.toDp() }, with(density) { h.h.toDp() })
                    .background(Color(0x334848F0)),
            )
        }
    }
}

private fun onTap(pos: Offset) {
    val hit = runCatching { hitTest(IntOffset(pos.x.toInt(), pos.y.toInt())) }
        .onFailure { println("[host] hitTest error: $it") }
        .getOrNull()
    highlightFlow.value = hit
    Ipc.send(buildJsonObject {
        put("type", "selection")
        put("found", hit != null)
        if (hit != null) {
            put("name", hit.name)
            put("file", hit.file)
            put("line", hit.line)
        }
    })
}

/** Source-file basenames that belong to the target app (vs Compose framework). */
private val targetFiles: Set<String> by lazy {
    var dir: java.io.File? = java.io.File(System.getProperty("user.dir"))
    while (dir != null && !java.io.File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
    val root = dir ?: java.io.File("/Users/jay.wj.wu/ProjectsWork_GitHub/Battle/jay-battle-product-helper/ragdoll-cat")
    val skip = setOf("build", ".gradle", ".git", ".idea", "generated")
    val files = listOf("feature", "core").flatMap { sub ->
        java.io.File(root, sub).walkTopDown()
            .onEnter { it.name !in skip }
            .filter { it.isFile && it.extension == "kt" }
            .map { it.name }
            .toList()
    }.toSet()
    println("[host] targetFiles=${files.size} root=$root userDir=${System.getProperty("user.dir")}")
    files
}

/** Deepest located target composable under [pos], from the cached snapshot. */
private fun hitTest(pos: IntOffset): HitResult? =
    cachedGroups
        .filter { pos.x >= it.x && pos.x < it.x + it.w && pos.y >= it.y && pos.y < it.y + it.h }
        .minByOrNull { it.w.toLong() * it.h.toLong() }

private fun handleCommand(cmd: JsonObject) {
    when (cmd["cmd"]?.jsonPrimitive?.content) {
        "setDesignMode" -> designModeFlow.value = cmd["on"]?.jsonPrimitive?.content == "true"
        "selectAt" -> {
            val x = cmd["x"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val y = cmd["y"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            onTap(Offset(x.toFloat(), y.toFloat()))
        }
    }
}

/** Line-delimited JSON over a TCP socket (one shell client). */
private object Ipc {
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var writer: BufferedWriter? = null
    var onCommand: (JsonObject) -> Unit = {}

    fun start(): Int {
        val server = ServerSocket(0)
        Thread {
            while (true) {
                val sock = runCatching { server.accept() }.getOrNull() ?: break
                writer = sock.getOutputStream().bufferedWriter()
                runCatching {
                    sock.getInputStream().bufferedReader().forEachLine { line ->
                        if (line.isNotBlank()) runCatching { onCommand(json.parseToJsonElement(line).jsonObject) }
                    }
                }
            }
        }.apply { isDaemon = true }.start()
        return server.localPort
    }

    fun send(obj: JsonObject) {
        runCatching { writer?.apply { write(obj.toString()); newLine(); flush() } }
    }
}
