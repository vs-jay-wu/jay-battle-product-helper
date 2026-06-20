package com.viewsonic.designershell.adapter

import kotlinx.serialization.json.Json
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
    private val module: String = ":designer-shell",
) : TargetAdapter {

    override val displayName: String = "Compose · ${File(projectDir).name}"
    override val workingDir: File = File(projectDir)
    override var onStatus: (String) -> Unit = {}
    override var onSelection: (SelectedNode) -> Unit = {}

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
        val p = ProcessBuilder("/bin/zsh", "-lc", "./gradlew $module:hotRunJvm --console=plain")
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
                    if (obj["type"]?.jsonPrimitive?.content == "selection" &&
                        obj["found"]?.jsonPrimitive?.booleanOrNull == true
                    ) {
                        onSelection(
                            SelectedNode(
                                desc = obj["name"]?.jsonPrimitive?.content ?: "?",
                                file = obj["file"]?.jsonPrimitive?.content ?: "?",
                                line = obj["line"]?.jsonPrimitive?.intOrNull ?: 0,
                                col = 0,
                            ),
                        )
                    }
                }
            }
        }.apply { isDaemon = true }.start()
    }

    override fun setDesignMode(on: Boolean) {
        send(buildJsonObject { put("cmd", "setDesignMode"); put("on", on.toString()) })
    }

    override fun hotReload() {
        // CHR explicit reload: recompiles changed sources + hot-swaps into the host.
        runCatching {
            ProcessBuilder("/bin/zsh", "-lc", "./gradlew $module:reload --console=plain")
                .directory(File(projectDir))
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        }
    }

    override fun stop() {
        runCatching { socket?.close() }
        process?.destroy()
    }

    private fun send(obj: JsonObject) {
        runCatching { writer?.apply { write(obj.toString()); newLine(); flush() } }
    }
}
