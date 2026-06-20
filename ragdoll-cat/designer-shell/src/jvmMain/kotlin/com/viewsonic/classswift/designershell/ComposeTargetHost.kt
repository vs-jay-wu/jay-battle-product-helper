package com.viewsonic.classswift.designershell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

private var designMode by mutableStateOf(false)
private var highlight by mutableStateOf<HitResult?>(null)

@Volatile private var capturedData: CompositionData? = null

private data class HitResult(val name: String, val file: String, val line: Int, val pkg: Int, val x: Int, val y: Int, val w: Int, val h: Int)

fun main() {
    val port = Ipc.start()
    println("HOST_READY port=$port")
    Ipc.onCommand = ::handleCommand
    singleWindowApplication(title = "Compose Target · ragdoll-cat") {
        Box(Modifier.fillMaxSize().background(Color.White)) {
            Capture(onData = { capturedData = it }) {
                QuizCollectionScreen(Samples.populated, onEvent = {})
            }
            if (designMode) DesignOverlay()
        }
    }
}

@OptIn(InternalComposeApi::class)
@Composable
private fun Capture(onData: (CompositionData) -> Unit, content: @Composable () -> Unit) {
    currentComposer.collectParameterInformation()
    onData(currentComposer.compositionData)
    content()
}

@Composable
private fun DesignOverlay() {
    // Absorb every pointer event; on press, hit-test the captured screen tree.
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                    event.changes.firstOrNull { it.changedToDown() }?.let { onTap(it.position) }
                }
            }
        },
    ) {
        val density = LocalDensity.current
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
    val hit = hitTest(IntOffset(pos.x.toInt(), pos.y.toInt()))
    highlight = hit
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
    val root = dir ?: java.io.File(System.getProperty("user.dir"))
    val files = listOf("feature", "core", "app").flatMap { sub ->
        java.io.File(root, sub).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { it.name }
            .toList()
    }.toSet()
    println("targetFiles: ${files.size} kt files under $root")
    files
}

/** Deepest located composable under [pos] that belongs to the target app. */
@OptIn(UiToolingDataApi::class)
private fun hitTest(pos: IntOffset): HitResult? {
    val data = capturedData ?: return null
    val located = mutableListOf<Group>()
    fun walk(g: Group) {
        if (g.location != null && g.name != null) located.add(g)
        g.children.forEach(::walk)
    }
    walk(data.asTree())
    val candidates = located.filter { it.box.contains(pos) && it.box.width > 0 && it.box.height > 0 }
    val hit = candidates
        .filter { it.location?.sourceFile in targetFiles }
        .minByOrNull { it.box.width.toLong() * it.box.height.toLong() }
        ?: return null
    val loc = hit.location!!
    return HitResult(
        hit.name ?: "?", loc.sourceFile ?: "?", loc.lineNumber, loc.packageHash,
        hit.box.left, hit.box.top, hit.box.width, hit.box.height,
    )
}

private fun handleCommand(cmd: JsonObject) {
    when (cmd["cmd"]?.jsonPrimitive?.content) {
        "setDesignMode" -> designMode = cmd["on"]?.jsonPrimitive?.content == "true"
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
