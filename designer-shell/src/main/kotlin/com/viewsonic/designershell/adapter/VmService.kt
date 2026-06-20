package com.viewsonic.designershell.adapter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicInteger

/**
 * Minimal Dart VM Service client (JSON-RPC over the JDK's built-in WebSocket).
 * Used by the Flutter Designer Shell to drive the docking + inspector service
 * extensions registered by the designer_shell_bridge package (ext.designer.*).
 */
class VmService private constructor(private val ws: WebSocket) {

    private val nextId = AtomicInteger(0)
    private val pending = ConcurrentHashMap<Int, CompletableFuture<JsonObject>>()
    private val incoming = StringBuilder()
    private val json = Json { ignoreUnknownKeys = true }
    private val sendLock = Any()

    var isolateId: String = ""
        private set

    /** Invoked for VM Service stream events (e.g. posted `Extension` events). */
    var onEvent: ((JsonObject) -> Unit)? = null

    private fun onText(data: CharSequence, last: Boolean) {
        incoming.append(data)
        if (!last) return
        val text = incoming.toString()
        incoming.setLength(0)
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        val id = obj["id"]?.jsonPrimitive?.intOrNull
        if (id != null) {
            pending.remove(id)?.complete(obj)
            return
        }
        if (obj["method"]?.jsonPrimitive?.content == "streamNotify") {
            val event = obj["params"]?.jsonObject?.get("event")?.jsonObject ?: return
            onEvent?.invoke(event)
        }
    }

    /** Raw JSON-RPC call. */
    fun call(method: String, params: JsonObject? = null): CompletableFuture<JsonObject> {
        val id = nextId.incrementAndGet()
        val future = CompletableFuture<JsonObject>()
        pending[id] = future
        val msg = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            put("method", method)
            if (params != null) put("params", params)
        }
        synchronized(sendLock) { ws.sendText(msg.toString(), true).join() }
        return future
    }

    /** Call a service extension on the main isolate, blocking for the result. */
    fun ext(method: String, params: Map<String, String> = emptyMap()): JsonObject {
        val p = buildJsonObject {
            put("isolateId", isolateId)
            params.forEach { (k, v) -> put(k, v) }
        }
        return call(method, p).get()
    }

    fun close() {
        runCatching { ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye") }
    }

    companion object {
        /** Connect to a VM Service HTTP URI (e.g. http://127.0.0.1:50077/abc=/). */
        fun connect(httpUri: String): VmService {
            val base = if (httpUri.endsWith("/")) httpUri else "$httpUri/"
            val wsUri = base.replaceFirst("http", "ws") + "ws"
            val holder = arrayOfNulls<VmService>(1)
            val socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(wsUri), object : WebSocket.Listener {
                    override fun onOpen(webSocket: WebSocket) {
                        webSocket.request(1)
                    }

                    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                        holder[0]?.onText(data, last)
                        webSocket.request(1)
                        return null
                    }
                }).join()
            val vm = VmService(socket)
            holder[0] = vm
            val r = vm.call("getVM").get()
            vm.isolateId = r["result"]!!.jsonObject["isolates"]!!.jsonArray[0]
                .jsonObject["id"]!!.jsonPrimitive.content
            return vm
        }
    }
}
