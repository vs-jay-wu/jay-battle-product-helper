package com.viewsonic.designershell

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A project's `.designer-shell.json` — how a repo declares that it supports the
 * Designer Shell and how the shell should build/run it. The shell stays generic:
 * it discovers projects by this file and drives them through the matching adapter,
 * so adding a new app needs no shell code change.
 *
 * ```jsonc
 * { "name": "ragdoll-cat", "sdk": "compose",
 *   "run": "./gradlew :designer-shell:hotRunJvm",
 *   "reload": "./gradlew :designer-shell:reload",
 *   "sourceDirs": ["feature", "core"] }
 * ```
 */
@Serializable
data class ProjectDescriptor(
    val name: String,
    val sdk: String,                       // "compose" | "flutter"
    val run: String,                       // launch command, run in the project dir
    val reload: String? = null,            // compose: hot-reload command (optional)
    val sourceDirs: List<String> = emptyList(),
) {
    /** Absolute path the descriptor was loaded from; set on load, not part of the JSON. */
    var projectDir: String = ""

    val label: String
        get() = when (sdk) {
            "flutter" -> "Flutter · $name"
            "compose" -> "Compose · $name"
            else -> name
        }

    val supported: Boolean get() = sdk == "compose" || sdk == "flutter"
}

const val DESCRIPTOR_FILENAME = ".designer-shell.json"

private val descriptorJson = Json { ignoreUnknownKeys = true }

/** Read [dir]'s descriptor, or null if it has none / it's invalid / its SDK is unsupported. */
fun readDescriptor(dir: File): ProjectDescriptor? {
    val file = File(dir, DESCRIPTOR_FILENAME)
    if (!file.isFile) return null
    return runCatching { descriptorJson.decodeFromString<ProjectDescriptor>(file.readText()) }
        .getOrNull()
        ?.takeIf { it.supported }
        ?.also { it.projectDir = dir.absolutePath }
}

/** Discover supported projects among [workspace]'s immediate subdirectories. */
fun scanWorkspace(workspace: File): List<ProjectDescriptor> =
    (workspace.listFiles()?.filter { it.isDirectory } ?: emptyList())
        .mapNotNull { readDescriptor(it) }
        .sortedBy { it.name }

/**
 * Default workspace to scan: the repo's `examples/` dir (sibling to the shell module),
 * where adopter apps live. Override with -Ddesigner.workspace=/abs/dir.
 */
fun defaultWorkspace(): File {
    System.getProperty("designer.workspace")?.let { return File(it) }
    val repoRoot = File(System.getProperty("user.dir")).parentFile ?: File(".")
    return File(repoRoot, "examples")
}
