package com.viewsonic.designershell.adapter

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedWriter
import java.io.File

/**
 * Hosts a native Flutter target out-of-process: launches `flutter run`, talks to
 * the Dart VM Service, drives the ext.shopdemo.* design-mode bridge, and triggers
 * hot reload by writing `r` to the flutter tool's stdin.
 */
class FlutterAdapter(private val projectDir: String) : TargetAdapter {

    override val displayName: String = "Flutter · ${File(projectDir).name}"
    override val workingDir: File = File(projectDir)
    override var onStatus: (String) -> Unit = {}
    override var onSelection: (SelectedNode) -> Unit = {}
    override var onTree: (List<TreeNode>) -> Unit = {}
    override var onPages: (List<PageInfo>) -> Unit = {}

    @Volatile private var vm: VmService? = null
    private var process: Process? = null
    private var stdin: BufferedWriter? = null

    override fun start() {
        Thread {
            val uri = startFlutter() ?: run { onStatus("flutter 啟動失敗"); return@Thread }
            onStatus("連線中…")
            val service = runCatching { VmService.connect(uri) }.getOrNull()
                ?: run { onStatus("VM 連線失敗"); return@Thread }
            vm = service
            service.call("streamListen", buildJsonObject { put("streamId", "Extension") })
            runCatching {
                service.ext(
                    "ext.flutter.inspector.setPubRootDirectories",
                    mapOf("arg0" to projectDir, "arg1" to "$projectDir/lib"),
                )
            }
            service.onEvent = { event ->
                if (event["extensionKind"]?.jsonPrimitive?.content == "shopdemo:selection") {
                    readSelection(service)?.let(onSelection)
                }
            }
            onStatus("已連線")
        }.apply { isDaemon = true }.start()
    }

    override fun setDesignMode(on: Boolean) {
        val service = vm ?: return
        Thread { runCatching { service.ext("ext.shopdemo.setDesignMode", mapOf("on" to on.toString())) } }
            .apply { isDaemon = true }.start()
    }

    override fun requestPages() {
        // flutter_shop is a single screen for now.
        onPages(listOf(PageInfo("storefront", "Storefront")))
    }

    override fun setPage(id: String) {
        // No-op: single page. (A multi-screen flutter app would navigate here.)
    }

    override fun requestTree() {
        val service = vm ?: return
        Thread {
            val r = runCatching {
                service.ext("ext.flutter.inspector.getRootWidgetSummaryTree", mapOf("objectGroup" to "tree"))
            }.getOrNull()
            val root = r?.get("result")?.jsonObject?.get("result")?.jsonObject
            onTree(if (root != null) listOf(parseFlutterTree(root)) else emptyList())
        }.apply { isDaemon = true }.start()
    }

    override fun selectNode(node: TreeNode) {
        val service = vm ?: return
        val id = node.id ?: return
        Thread {
            runCatching {
                service.ext("ext.flutter.inspector.setSelectionById", mapOf("arg" to id, "objectGroup" to "tree"))
                readSelection(service)?.let(onSelection)
            }
        }.apply { isDaemon = true }.start()
    }

    private fun parseFlutterTree(o: JsonObject): TreeNode {
        val loc = o["creationLocation"]?.jsonObject
        return TreeNode(
            label = o["description"]?.jsonPrimitive?.content ?: "?",
            file = loc?.get("file")?.jsonPrimitive?.content?.substringAfterLast('/'),
            line = loc?.get("line")?.jsonPrimitive?.intOrNull ?: 0,
            id = o["valueId"]?.jsonPrimitive?.content,
            children = (o["children"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::parseFlutterTree) } ?: emptyList(),
        )
    }

    override fun hotReload() {
        runCatching { stdin?.apply { write("r"); newLine(); flush() } }
    }

    override fun stop() {
        runCatching { vm?.close() }
        process?.destroy()
    }

    private fun startFlutter(): String? {
        val p = ProcessBuilder("/bin/zsh", "-lc", "fvm flutter run -d macos --no-version-check")
            .directory(File(projectDir)).redirectErrorStream(true).start()
        process = p
        stdin = p.outputStream.bufferedWriter()
        val regex = Regex("""Dart VM Service[^h]*at:\s*(http://\S+)""")
        val reader = p.inputStream.bufferedReader()
        var uri: String? = null
        while (true) {
            val line = reader.readLine() ?: break
            if (line.contains("Building") || line.contains("Compiling") || line.contains("Xcode")) onStatus("build 中…")
            regex.find(line)?.let { uri = it.groupValues[1].trimEnd() }
            if (uri != null) break
        }
        // Keep draining so flutter never blocks on a full stdout pipe.
        Thread { runCatching { reader.forEachLine { } } }.apply { isDaemon = true }.start()
        return uri
    }

    private fun readSelection(service: VmService): SelectedNode? {
        val r = runCatching {
            service.ext("ext.flutter.inspector.getSelectedSummaryWidget", mapOf("objectGroup" to "shell"))
        }.getOrNull() ?: return null
        val node = r["result"]?.jsonObject?.get("result")?.jsonObject ?: return null
        val desc = node["description"]?.jsonPrimitive?.content ?: "?"
        val loc = node["creationLocation"]?.jsonObject
        val file = loc?.get("file")?.jsonPrimitive?.content?.substringAfterLast('/') ?: "?"
        val line = loc?.get("line")?.jsonPrimitive?.intOrNull ?: 0
        val col = loc?.get("column")?.jsonPrimitive?.intOrNull ?: 0
        return SelectedNode(desc, file, line, col)
    }
}
