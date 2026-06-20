package com.viewsonic.designershell

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** One stored Claude message (for restoring a session's transcript on switch). */
@Serializable
data class StoredMessage(val role: String, val text: String)

/**
 * A saved designer session: which target it drives, a stable Claude session id
 * (so the conversation can resume), and the transcript for display.
 */
@Serializable
data class Session(
    val id: String,
    var name: String,
    val target: String, // "flutter" | "compose"
    val claudeSessionId: String,
    var transcript: List<StoredMessage> = emptyList(),
)

/** Persists sessions as JSON files under ~/.designer-shell/sessions/. */
class SessionStore(
    private val dir: File = File(System.getProperty("user.home"), ".designer-shell/sessions"),
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        dir.mkdirs()
    }

    fun list(): List<Session> =
        dir.listFiles { f -> f.isFile && f.extension == "json" }
            ?.mapNotNull { runCatching { json.decodeFromString<Session>(it.readText()) }.getOrNull() }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    fun save(session: Session) {
        runCatching { File(dir, "${session.id}.json").writeText(json.encodeToString(session)) }
    }

    fun delete(id: String) {
        runCatching { File(dir, "$id.json").delete() }
    }

    fun create(name: String, target: String): Session =
        Session(
            id = UUID.randomUUID().toString(),
            name = name,
            target = target,
            claudeSessionId = UUID.randomUUID().toString(),
        ).also { save(it) }
}
