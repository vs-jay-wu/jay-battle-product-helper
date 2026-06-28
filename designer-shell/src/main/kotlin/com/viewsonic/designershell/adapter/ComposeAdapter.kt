package com.viewsonic.designershell.adapter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedWriter
import java.io.File
import java.net.Socket

/**
 * Hosts a Compose target out-of-process. Launches the target's Compose-Desktop
 * preview host via CHR `hotRunJvm`, connects to its line-JSON TCP bridge
 * (ComposeTargetHost), toggles design mode + receives selections, and hot-reloads
 * via CHR `reload`. Mirrors [FlutterAdapter] for the Compose tech stack.
 */
class ComposeAdapter(
    private val projectDir: String,
    private val runCmd: String = "./gradlew :designer-shell:hotRunJvm",
    private val reloadCmd: String? = "./gradlew :designer-shell:reload",
) : TargetAdapter {

    override val displayName: String = "Compose · ${File(projectDir).name}"
    override val workingDir: File = File(projectDir)
    override var onStatus: (String) -> Unit = {}
    override var onSelection: (SelectedNode) -> Unit = {}
    override var onTree: (List<TreeNode>) -> Unit = {}
    override var onPages: (List<PageInfo>) -> Unit = {}
    override var onError: (String) -> Unit = {}

    private val json = Json { ignoreUnknownKeys = true }
    private var process: Process? = null
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null

    override fun start() {
        Thread {
            val port = launchHost() ?: run { onStatus("host 啟動失敗"); return@Thread }
            onStatus("連線中…")
            runCatching { connect(port) }.onFailure { onStatus("bridge 連線失敗"); return@Thread }
            onStatus("已連線")
        }.apply { isDaemon = true }.start()
    }

    private fun launchHost(): Int? {
        val p = ProcessBuilder("/bin/zsh", "-lc", "$runCmd --console=plain")
            .directory(File(projectDir)).redirectErrorStream(true).start()
        process = p
        val reader = p.inputStream.bufferedReader()
        val regex = Regex("""HOST_READY port=(\d+)""")
        var port: Int? = null
        while (true) {
            val line = reader.readLine() ?: break
            if (line.contains("Compiling") || line.contains("Configure project") || line.contains("Compose Hot Reload")) {
                onStatus("build 中…")
            }
            regex.find(line)?.let { port = it.groupValues[1].toInt() }
            if (port != null) break
        }
        Thread { runCatching { reader.forEachLine { } } }.apply { isDaemon = true }.start()
        return port
    }

    private fun connect(port: Int) {
        val s = Socket("127.0.0.1", port)
        socket = s
        writer = s.getOutputStream().bufferedWriter()
        Thread {
            runCatching {
                s.getInputStream().bufferedReader().forEachLine { line ->
                    if (line.isBlank()) return@forEachLine
                    val obj = runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull() ?: return@forEachLine
                    when (obj["type"]?.jsonPrimitive?.content) {
                        "selection" -> if (obj["found"]?.jsonPrimitive?.booleanOrNull == true) {
                            onSelection(
                                SelectedNode(
                                    desc = obj["name"]?.jsonPrimitive?.content ?: "?",
                                    file = obj["file"]?.jsonPrimitive?.content ?: "?",
                                    line = obj["line"]?.jsonPrimitive?.intOrNull ?: 0,
                                    col = 0,
                                ),
                            )
                        }
                        "tree" -> onTree((obj["nodes"] as? JsonArray)?.map(::parseTree) ?: emptyList())
                        "pages" -> onPages(
                            (obj["pages"] as? JsonArray)?.map {
                                val p = it.jsonObject
                                PageInfo(p["id"]?.jsonPrimitive?.content ?: "", p["label"]?.jsonPrimitive?.content ?: "?")
                            } ?: emptyList(),
                        )
                    }
                }
            }
        }.apply { isDaemon = true }.start()
    }

    override fun setDesignMode(on: Boolean) {
        send(buildJsonObject { put("cmd", "setDesignMode"); put("on", on.toString()) })
    }

    override fun requestTree() {
        send(buildJsonObject { put("cmd", "getTree") })
    }

    override fun requestPages() {
        send(buildJsonObject { put("cmd", "getPages") })
    }

    override fun setPage(id: String) {
        send(buildJsonObject { put("cmd", "setPage"); put("id", id) })
    }

    override fun selectNode(node: TreeNode) {
        // Re-select via the node's center point (host hit-tests it like a tap).
        send(buildJsonObject {
            put("cmd", "selectAt")
            put("x", (node.x + node.w / 2).toInt().toString())
            put("y", (node.y + node.h / 2).toInt().toString())
        })
    }

    private fun parseTree(e: kotlinx.serialization.json.JsonElement): TreeNode {
        val o = e.jsonObject
        return TreeNode(
            label = o["label"]?.jsonPrimitive?.content ?: "?",
            file = o["file"]?.jsonPrimitive?.content,
            line = o["line"]?.jsonPrimitive?.intOrNull ?: 0,
            x = o["x"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f,
            y = o["y"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f,
            w = o["w"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f,
            h = o["h"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f,
            children = (o["children"] as? JsonArray)?.map(::parseTree) ?: emptyList(),
        )
    }

    override fun hotReload() {
        // CHR explicit reload: recompiles changed sources + hot-swaps into the host.
        val cmd = reloadCmd ?: return
        runCatching {
            ProcessBuilder("/bin/zsh", "-lc", "$cmd --console=plain")
                .directory(File(projectDir))
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        }
    }

    override fun clearSelection() {
        // Compose target has no explicit clear; the highlight clears on next select.
    }

    override fun requestDesignTree() {
        // Compose targets already expose only app-authored design nodes, so the
        // clean tree is the same as the full tree here.
        requestTree()
    }

    override fun hotRestart() {
        // Compose Hot Reload has no separate "restart" channel; its reload already
        // recompiles + hot-swaps changed sources, so we fall back to that. (A true
        // process restart would mean stop() + start() and re-opening the window.)
        hotReload()
    }

    override fun stop() {
        runCatching { socket?.close() }
        process?.destroy()
    }

    private fun send(obj: JsonObject) {
        runCatching { writer?.apply { write(obj.toString()); newLine(); flush() } }
    }
}
