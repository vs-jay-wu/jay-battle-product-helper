package com.viewsonic.designershell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedWriter
import java.io.File
import java.util.UUID

enum class ClaudeRole { USER, ASSISTANT, TOOL, RESULT }

data class ClaudeMessage(val role: ClaudeRole, val text: String)

/**
 * One long-lived `claude` CLI process = one interactive session, driven over stream-json
 * (stdin: user messages, stdout: assistant/tool/result events). No API token needed — uses the
 * logged-in CLI. Runs with --dangerously-skip-permissions (demo) so edits apply unattended; the
 * file changes are picked up by `hotRunJvm --auto` and hot-reload the canvas.
 */
class ClaudeSession(private val workingDir: File) {

    val transcript = mutableStateListOf<ClaudeMessage>()
    var sessionId: String by mutableStateOf(""); private set
    var running: Boolean by mutableStateOf(false); private set

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var scope: CoroutineScope? = null
    private val json = Json { ignoreUnknownKeys = true }

    /** Invoked (on an IO thread) when a turn finishes — the shell uses this to trigger a reload. */
    var onComplete: (() -> Unit)? = null

    fun start() {
        stop()
        transcript.clear()
        val id = UUID.randomUUID().toString()
        sessionId = id
        val cmd = "claude -p --input-format stream-json --output-format stream-json --verbose " +
            "--dangerously-skip-permissions --session-id $id"
        // login shell so ~/.local/bin (claude) is on PATH
        val p = ProcessBuilder("/bin/zsh", "-lc", cmd).directory(workingDir).start()
        process = p
        writer = p.outputStream.bufferedWriter()
        running = true
        val s = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope = s
        s.launch {
            runCatching { p.inputStream.bufferedReader().forEachLine(::handleLine) }
            launch(Dispatchers.Main) { running = false }
        }
        s.launch { runCatching { p.errorStream.bufferedReader().forEachLine { } } }
    }

    fun send(text: String) {
        val w = writer ?: return
        add(ClaudeRole.USER, text)
        val msg = buildJsonObject {
            put("type", "user")
            putJsonObject("message") {
                put("role", "user")
                put("content", text)
            }
        }
        runCatching { w.write(msg.toString()); w.newLine(); w.flush() }
    }

    fun stop() {
        runCatching { writer?.close() }
        process?.destroy()
        scope?.cancel()
        process = null
        writer = null
        scope = null
        running = false
    }

    private fun handleLine(line: String) {
        if (line.isBlank()) return
        val obj = runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull() ?: return
        when (obj["type"]?.jsonPrimitive?.content) {
            "assistant" -> {
                val content = obj["message"]?.jsonObject?.get("content") as? JsonArray ?: return
                content.forEach { block ->
                    val b = block.jsonObject
                    when (b["type"]?.jsonPrimitive?.content) {
                        "text" -> b["text"]?.jsonPrimitive?.content
                            ?.takeIf { it.isNotBlank() }
                            ?.let { add(ClaudeRole.ASSISTANT, it) }
                        "tool_use" -> add(ClaudeRole.TOOL, "↳ ${b["name"]?.jsonPrimitive?.content ?: "tool"}")
                    }
                }
            }
            "result" -> {
                obj["result"]?.jsonPrimitive?.content
                    ?.takeIf { it.isNotBlank() }
                    ?.let { add(ClaudeRole.RESULT, it) }
                onComplete?.invoke()
            }
        }
    }

    private fun add(role: ClaudeRole, text: String) {
        (scope ?: return).launch(Dispatchers.Main) { transcript.add(ClaudeMessage(role, text)) }
    }
}
