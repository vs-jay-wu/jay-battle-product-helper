package com.viewsonic.designershell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.viewsonic.designershell.adapter.ComposeAdapter
import com.viewsonic.designershell.adapter.FlutterAdapter
import com.viewsonic.designershell.adapter.SelectedNode
import com.viewsonic.designershell.adapter.TargetAdapter

private const val BASE = "/Users/jay.wj.wu/ProjectsWork_GitHub/Battle/jay-battle-product-helper"
private const val FLUTTER_SHOP = "$BASE/flutter_shop"
private const val RAGDOLL_CAT = "$BASE/ragdoll-cat"

private fun adapterFor(session: Session): TargetAdapter =
    if (session.target == "flutter") FlutterAdapter(FLUTTER_SHOP) else ComposeAdapter(RAGDOLL_CAT)

/**
 * Standalone Designer Shell — a generic out-of-process control panel. Sessions
 * (one per target + Claude conversation) are saved and switchable; each hosts a
 * target through a [TargetAdapter] (Flutter / Compose) without compiling it in.
 */
fun main() = application {
    val store = remember { SessionStore() }
    val state = rememberWindowState(width = 560.dp, height = 860.dp)
    Window(onCloseRequest = ::exitApplication, state = state, title = "Designer Shell") {
        var sessions by remember { mutableStateOf(store.list()) }
        var active by remember { mutableStateOf<Session?>(null) }
        var renaming by remember { mutableStateOf(false) }

        fun openNew(target: String) {
            val label = if (target == "flutter") "Flutter" else "Compose"
            val s = store.create("$label ${sessions.count { it.target == target } + 1}", target)
            sessions = store.list()
            active = s
        }

        MenuBar {
            Menu("Session", mnemonic = 'S') {
                Item("新增 Flutter session") { openNew("flutter") }
                Item("新增 Compose session") { openNew("compose") }
                if (active != null) {
                    Item("重新命名…") { renaming = true }
                    Item("關閉(回到清單)") { active = null }
                }
                Separator()
                sessions.forEach { s ->
                    Item("${if (s.id == active?.id) "● " else "   "}${s.name}  ·  ${s.target}") { active = s }
                }
            }
        }

        val current = active
        if (renaming && current != null) {
            RenameDialog(current.name, onConfirm = { newName ->
                val updated = current.copy(name = newName)
                store.save(updated)
                sessions = store.list()
                active = updated
                renaming = false
            }, onDismiss = { renaming = false })
        }

        when (current) {
            null -> SessionPicker(sessions, onOpen = { active = it }, onNew = ::openNew)
            else -> key(current.id) { ControlPanel(current, store) }
        }
    }
}

@Composable
private fun SessionPicker(sessions: List<Session>, onOpen: (Session) -> Unit, onNew: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF4F4F6)).padding(24.dp)) {
        Text("Designer Shell", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
        Text("選擇或新增一個 session（每個 session = 一個目標 + Claude 對話）", color = Color(0xFF797979), fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onNew("flutter") }, modifier = Modifier.fillMaxWidth()) { Text("＋ 新增 Flutter · flutter_shop") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onNew("compose") }, modifier = Modifier.fillMaxWidth()) { Text("＋ 新增 Compose · ragdoll-cat") }
        Spacer(Modifier.height(20.dp))
        Text("已儲存的 session", fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
        Spacer(Modifier.height(8.dp))
        if (sessions.isEmpty()) {
            Text("（還沒有；用上面按鈕新增）", color = Color(0xFF797979), fontSize = 12.sp)
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            sessions.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { onOpen(s) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${s.name}   ·   ${s.target}")
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重新命名 session") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("確定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ControlPanel(session: Session, store: SessionStore) {
    val adapter = remember { adapterFor(session) }
    val claude = remember { ClaudeSession(adapter.workingDir, session.claudeSessionId) }
    val hadHistory = remember { session.transcript.isNotEmpty() }
    var everStarted by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("啟動中…") }
    var designMode by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<SelectedNode?>(null) }

    LaunchedEffect(Unit) {
        claude.seed(session.transcript.map { ClaudeMessage(ClaudeRole.valueOf(it.role), it.text) })
        adapter.onStatus = { status = it }
        adapter.onSelection = { selection = it }
        claude.onComplete = { adapter.hotReload() }
        adapter.start()
    }
    // Persist the transcript whenever it grows.
    LaunchedEffect(Unit) {
        snapshotFlow { claude.transcript.size }.collect {
            session.transcript = claude.transcript.map { StoredMessage(it.role.name, it.text) }
            store.save(session)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { session.transcript = claude.transcript.map { StoredMessage(it.role.name, it.text) }; store.save(session) }
            runCatching { claude.stop() }
            runCatching { adapter.stop() }
        }
    }

    val onSend: (String) -> Unit = { text ->
        if (!claude.running) {
            claude.start(resume = hadHistory && !everStarted)
            everStarted = true
        }
        claude.send(buildPrompt(text, selection))
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF4F4F6)).padding(16.dp)) {
        Text(session.name, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("${adapter.displayName}   ·   $status", color = Color(0xFF797979), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = designMode, label = { Text("設計") }, onClick = {
                designMode = true
                adapter.setDesignMode(true)
            })
            FilterChip(selected = !designMode, label = { Text("互動") }, onClick = {
                designMode = false
                adapter.setDesignMode(false)
            })
        }
        Spacer(Modifier.height(12.dp))

        InspectorCard(selection)
        Spacer(Modifier.height(12.dp))
        ClaudeCard(claude, onSend, Modifier.weight(1f))
    }
}

@Composable
private fun InspectorCard(selection: SelectedNode?) {
    Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
        Text("Inspector", fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
        Spacer(Modifier.height(6.dp))
        if (selection == null) {
            Text("切到「設計」模式，點目標 app 上的元件", color = Color(0xFF797979), fontSize = 12.sp)
        } else {
            Text(selection.desc, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Medium)
            Text(
                "${selection.file}:${selection.line}:${selection.col}",
                color = Color(0xFF4848F0), fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun ClaudeCard(session: ClaudeSession, onSend: (String) -> Unit, modifier: Modifier = Modifier) {
    var prompt by remember { mutableStateOf("") }
    Column(modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
        Text("Claude", fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
        Spacer(Modifier.height(6.dp))
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            session.transcript.takeLast(40).forEach { msg ->
                Text(
                    "${msg.role}: ${msg.text}",
                    fontSize = 12.sp,
                    color = if (msg.role.name == "USER") Color(0xFF4848F0) else Color(0xFF333333),
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("描述想要的變更…") },
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onSend(prompt); prompt = "" },
            enabled = prompt.isNotBlank(),
        ) { Text("送出") }
    }
}

private fun buildPrompt(text: String, selection: SelectedNode?): String {
    if (selection == null) return text
    return "$text\n\n(目標元件：${selection.desc}，位於 ${selection.file}:${selection.line}。" +
        "請定位該元件並修改，只動必要的部分。)"
}
