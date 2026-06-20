package com.viewsonic.classswift.designershell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.singleWindowApplication
import com.viewsonic.classswift.core.ui.DesignNodeRegistry
import com.viewsonic.classswift.core.ui.LocalDesignNodeRegistry
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionScreen
import com.viewsonic.classswift.fixtures.Samples
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedWriter
import java.net.ServerSocket
import kotlin.math.roundToInt

/**
 * Out-of-process Compose target for the standalone Designer Shell. Renders the
 * real ragdoll CMP screen; in design mode a tap is hit-tested two ways:
 *  - DesignNodeRegistry (Modifier.designNode tags) — works for LazyGrid items
 *    (subcompositions), giving a per-instance id + precise bounds.
 *  - ui-tooling-data asTree snapshot — gives the composable's source file:line.
 * Talks to the shell over a line-JSON TCP socket; CHR provides hot reload.
 */

// StateFlows so writes from the IPC thread reliably trigger recomposition.
private val designModeFlow = MutableStateFlow(false)
private val highlightFlow = MutableStateFlow<Rect?>(null)

/** Switchable pages this target exposes to the shell. Any ragdoll screen can be
 *  registered here; the shell lists them and asks the host to switch. */
private class Page(val id: String, val label: String, val content: @Composable () -> Unit)

private val pages: List<Page> = listOf(
    Page("qc_populated", "Quiz Collection · Populated") { QuizCollectionScreen(Samples.populated, onEvent = {}) },
    Page("qc_empty", "Quiz Collection · Empty") { QuizCollectionScreen(Samples.empty, onEvent = {}) },
    Page("qc_loading", "Quiz Collection · Loading") { QuizCollectionScreen(Samples.loading, onEvent = {}) },
    Page("qc_error", "Quiz Collection · Error") { QuizCollectionScreen(Samples.error, onEvent = {}) },
)
private val currentPageFlow = MutableStateFlow(0)

/** Immutable snapshot of located target composables (source file:line), rebuilt
 *  on the UI thread after layout (reading the live slot table off-thread crashes). */
@Volatile private var cachedGroups: List<Located> = emptyList()

/** Latest structure tree (target composables, hierarchy) for the shell. */
@Volatile private var cachedTree: JsonArray = JsonArray(emptyList())

private data class Located(val name: String, val file: String, val line: Int, val x: Int, val y: Int, val w: Int, val h: Int)

// Figma-style click-to-drill: repeated taps on the same node stack step inward.
private var lastStackIds: List<String> = emptyList()
private var drillIndex = 0

fun main() {
    val port = Ipc.start()
    println("HOST_READY port=$port")
    Ipc.onCommand = ::handleCommand
    singleWindowApplication(title = "Compose Target · ragdoll-cat") {
        val designMode by designModeFlow.collectAsState()
        val pageIndex by currentPageFlow.collectAsState()
        val registry = remember { DesignNodeRegistry() }
        var host by remember { mutableStateOf<LayoutCoordinates?>(null) }
        Box(Modifier.fillMaxSize().background(Color.White).onGloballyPositioned { host = it }) {
            CompositionLocalProvider(LocalDesignNodeRegistry provides registry) {
                Capture {
                    pages[pageIndex].content()
                }
            }
            if (designMode) DesignOverlay { pos -> handleTap(pos, registry, host) }
        }
    }
}

@OptIn(InternalComposeApi::class)
@Composable
private fun Capture(content: @Composable () -> Unit) {
    currentComposer.collectParameterInformation()
    val data = currentComposer.compositionData
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            val snap = snapshotLocated(data)
            if (snap.isNotEmpty()) cachedGroups = snap
            val tree = buildTree(data)
            if (tree.isNotEmpty()) cachedTree = JsonArray(tree)
        }
    }
    content()
}

/** Hierarchy of target composables (parent/child preserved, framework groups
 *  collapsed) for the shell's structure tree. */
@OptIn(UiToolingDataApi::class)
private fun buildTree(data: CompositionData): List<JsonObject> = runCatching {
    fun walk(g: Group): List<JsonObject> {
        val childTrees = g.children.flatMap { walk(it) }
        val loc = g.location
        val file = loc?.sourceFile
        val name = g.name
        return if (file != null && name != null && file in targetFiles && g.box.width > 0 && g.box.height > 0) {
            listOf(
                buildJsonObject {
                    put("label", name)
                    put("file", file)
                    put("line", loc.lineNumber)
                    put("x", g.box.left); put("y", g.box.top); put("w", g.box.width); put("h", g.box.height)
                    put("children", JsonArray(childTrees))
                },
            )
        } else {
            childTrees // collapse non-target group, lift its target descendants
        }
    }
    walk(data.asTree())
}.getOrElse { emptyList() }

@OptIn(UiToolingDataApi::class)
private fun snapshotLocated(data: CompositionData): List<Located> = runCatching {
    val out = mutableListOf<Located>()
    fun walk(g: Group) {
        val loc = g.location
        val file = loc?.sourceFile
        val name = g.name
        if (file != null && name != null && file in targetFiles && g.box.width > 0 && g.box.height > 0) {
            out.add(Located(name, file, loc.lineNumber, g.box.left, g.box.top, g.box.width, g.box.height))
        }
        g.children.forEach(::walk)
    }
    walk(data.asTree())
    out
}.getOrElse { cachedGroups }

@Composable
private fun DesignOverlay(onTap: (Offset) -> Unit) {
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    // Detect the press BEFORE consuming — changedToDown() is false once consumed.
                    val down = event.changes.firstOrNull { it.changedToDown() }
                    event.changes.forEach { it.consume() }
                    if (down != null) onTap(down.position)
                }
            }
        },
    ) {
        val density = LocalDensity.current
        val highlight by highlightFlow.collectAsState()
        highlight?.let { r ->
            Box(
                Modifier.offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
                    .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
                    .background(Color(0x334848F0)),
            )
        }
    }
}

/** Interactive tap: prefer the precise design-node (per-card), fall back to the
 *  source-located composable; report both id and source file:line to the shell. */
private data class HitNode(val label: String, val file: String?, val line: Int, val rect: Rect)

/** All selectable nodes under [pos] — designNode tags (per-instance, incl. lazy)
 *  merged with source-located composables (asTree) — deduped by bounds, outermost
 *  (largest) first. Drives Figma-style click-to-drill for every context. */
private fun unifiedStack(pos: Offset, registry: DesignNodeRegistry, host: LayoutCoordinates?): List<HitNode> {
    fun key(r: Rect) = "${r.left.roundToInt()},${r.top.roundToInt()},${r.width.roundToInt()},${r.height.roundToInt()}"
    val byRect = LinkedHashMap<String, HitNode>()
    val x = pos.x.toInt()
    val y = pos.y.toInt()
    cachedGroups.filter { x >= it.x && x < it.x + it.w && y >= it.y && y < it.y + it.h }.forEach {
        val r = Rect(it.x.toFloat(), it.y.toFloat(), (it.x + it.w).toFloat(), (it.y + it.h).toFloat())
        byRect[key(r)] = HitNode(it.name, it.file, it.line, r)
    }
    host?.let { h ->
        registry.hitStack(pos, h).forEach { n ->
            val existing = byRect[key(n.rect)]
            byRect[key(n.rect)] = HitNode(n.id, existing?.file, existing?.line ?: 0, n.rect)
        }
    }
    return byRect.values.sortedByDescending { it.rect.width.toDouble() * it.rect.height }
}

private fun handleTap(pos: Offset, registry: DesignNodeRegistry, host: LayoutCoordinates?) {
    val stack = unifiedStack(pos, registry, host)
    if (stack.isEmpty()) {
        lastStackIds = emptyList()
        drillIndex = 0
        emitSelection(null, null, 0, null)
        return
    }
    // Same node stack (same spot) → step inward (wrap); new spot → outermost.
    val ids = stack.map { it.label }
    drillIndex = if (ids == lastStackIds) (drillIndex + 1) % stack.size else 0
    lastStackIds = ids
    val sel = stack[drillIndex]
    emitSelection(sel.label, sel.file ?: "QuizCollectionScreen.kt", sel.line, sel.rect)
}

private fun emitSelection(name: String?, file: String?, line: Int, rect: Rect?) {
    highlightFlow.value = rect
    Ipc.send(buildJsonObject {
        put("type", "selection")
        put("found", name != null)
        if (name != null) {
            put("name", name)
            put("file", file ?: "?")
            put("line", line)
        }
    })
}

/** Deepest source-located target composable under [pos], from the cached snapshot. */
private fun sourceAt(pos: Offset): Located? {
    val x = pos.x.toInt()
    val y = pos.y.toInt()
    return cachedGroups
        .filter { x >= it.x && x < it.x + it.w && y >= it.y && y < it.y + it.h }
        .minByOrNull { it.w.toLong() * it.h.toLong() }
}

/** Source-file basenames that belong to the target app (vs Compose framework). */
private val targetFiles: Set<String> by lazy {
    var dir: java.io.File? = java.io.File(System.getProperty("user.dir"))
    while (dir != null && !java.io.File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
    val root = dir ?: java.io.File("/Users/jay.wj.wu/ProjectsWork_GitHub/Battle/jay-battle-product-helper/ragdoll-cat")
    val skip = setOf("build", ".gradle", ".git", ".idea", "generated")
    listOf("feature", "core").flatMap { sub ->
        java.io.File(root, sub).walkTopDown()
            .onEnter { it.name !in skip }
            .filter { it.isFile && it.extension == "kt" }
            .map { it.name }
            .toList()
    }.toSet()
}

private fun handleCommand(cmd: JsonObject) {
    when (cmd["cmd"]?.jsonPrimitive?.content) {
        "setDesignMode" -> designModeFlow.value = cmd["on"]?.jsonPrimitive?.content == "true"
        "getTree" -> Ipc.send(buildJsonObject { put("type", "tree"); put("nodes", cachedTree) })
        "getPages" -> Ipc.send(
            buildJsonObject {
                put("type", "pages")
                put("current", currentPageFlow.value)
                put("pages", JsonArray(pages.map { buildJsonObject { put("id", it.id); put("label", it.label) } }))
            },
        )
        "setPage" -> {
            val idx = pages.indexOfFirst { it.id == cmd["id"]?.jsonPrimitive?.content }
            if (idx >= 0) currentPageFlow.value = idx
        }
        "selectAt" -> {
            // Headless test path (no registry): source-located hit only.
            val x = cmd["x"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val y = cmd["y"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val src = sourceAt(Offset(x.toFloat(), y.toFloat()))
            if (src != null) {
                emitSelection(src.name, src.file, src.line, Rect(src.x.toFloat(), src.y.toFloat(), (src.x + src.w).toFloat(), (src.y + src.h).toFloat()))
            } else {
                emitSelection(null, null, 0, null)
            }
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
