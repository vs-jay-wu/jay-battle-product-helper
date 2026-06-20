package com.viewsonic.designershell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.viewsonic.designershell.adapter.PageInfo
import com.viewsonic.designershell.adapter.SelectedNode
import com.viewsonic.designershell.adapter.TargetAdapter
import com.viewsonic.designershell.adapter.TreeNode

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
            val s = store.create("對話 ${sessions.count { it.target == target } + 1}", target)
            sessions = store.list()
            active = s
        }

        MenuBar {
            Menu("Session", mnemonic = 'S') {
                Item("新的 Flutter 對話") { openNew("flutter") }
                Item("新的 Compose 對話") { openNew("compose") }
                if (active != null) {
                    Item("重新命名對話…") { renaming = true }
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

private fun repoLabel(target: String): String =
    if (target == "flutter") "Flutter · flutter_shop" else "Compose · ragdoll-cat"

private fun sessionPreview(s: Session): String =
    if (s.transcript.isEmpty()) {
        "尚無對話"
    } else {
        "${s.transcript.size} 則訊息 · ${s.transcript.last().text.take(34).replace("\n", " ")}…"
    }

@Composable
private fun SessionPicker(sessions: List<Session>, onOpen: (Session) -> Unit, onNew: (String) -> Unit) {
    var repo by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Color(0xFFF4F4F6)).padding(24.dp)) {
        Text("Designer Shell", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
        Spacer(Modifier.height(16.dp))

        val selected = repo
        if (selected == null) {
            // Step 1 — choose the project (repo).
            Text("選擇專案", fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Spacer(Modifier.height(10.dp))
            Button(onClick = { repo = "flutter" }, modifier = Modifier.fillMaxWidth()) { Text(repoLabel("flutter")) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { repo = "compose" }, modifier = Modifier.fillMaxWidth()) { Text(repoLabel("compose")) }
        } else {
            // Step 2 — sessions for the chosen project.
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = { repo = null }) { Text("← 專案") }
                Spacer(Modifier.width(8.dp))
                Text(repoLabel(selected), fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { onNew(selected) }, modifier = Modifier.fillMaxWidth()) { Text("＋ 新的 Claude 對話") }
            Spacer(Modifier.height(16.dp))
            Text("Claude 對話（session）", fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Spacer(Modifier.height(8.dp))
            val repoSessions = sessions.filter { it.target == selected }
            if (repoSessions.isEmpty()) {
                Text("（這個專案還沒有對話）", color = Color(0xFF797979), fontSize = 12.sp)
            }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                repoSessions.forEach { s ->
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            .clickable { onOpen(s) }
                            .background(Color.White)
                            .padding(12.dp),
                    ) {
                        Text(s.name, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                        Text(sessionPreview(s), fontSize = 11.sp, color = Color(0xFF797979))
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
    var tree by remember { mutableStateOf<List<TreeNode>>(emptyList()) }
    var pages by remember { mutableStateOf<List<PageInfo>>(emptyList()) }
    var currentPageId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        claude.seed(session.transcript.map { ClaudeMessage(ClaudeRole.valueOf(it.role), it.text) })
        adapter.onStatus = { status = it; if (it == "已連線") adapter.requestPages() }
        adapter.onSelection = { selection = it }
        adapter.onTree = { tree = it }
        adapter.onPages = { pages = it; if (currentPageId == null) currentPageId = it.firstOrNull()?.id }
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
        Spacer(Modifier.height(8.dp))

        if (pages.size > 1) {
            PagePicker(pages, currentPageId, onSelect = { id ->
                currentPageId = id
                adapter.setPage(id)
                selection = null
                if (designMode) adapter.requestTree()
            })
            Spacer(Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = designMode, label = { Text("設計") }, onClick = {
                designMode = true
                adapter.setDesignMode(true)
                adapter.requestTree()
            })
            FilterChip(selected = !designMode, label = { Text("互動") }, onClick = {
                designMode = false
                adapter.setDesignMode(false)
            })
        }
        Spacer(Modifier.height(12.dp))

        InspectorCard(selection)
        Spacer(Modifier.height(12.dp))
        StructureCard(tree, onRefresh = { adapter.requestTree() }, onSelect = { adapter.selectNode(it) }, Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        ClaudeCard(claude, onSend, Modifier.weight(1f))
    }
}

@Composable
private fun StructureCard(
    tree: List<TreeNode>,
    onRefresh: () -> Unit,
    onSelect: (TreeNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Structure", fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onRefresh) { Text("↻ 重新整理") }
        }
        Spacer(Modifier.height(4.dp))
        if (tree.isEmpty()) {
            Text("切到「設計」模式後顯示元件樹", color = Color(0xFF797979), fontSize = 12.sp)
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                tree.forEach { TreeRow(it, 0, onSelect) }
            }
        }
    }
}

@Composable
private fun TreeRow(node: TreeNode, depth: Int, onSelect: (TreeNode) -> Unit) {
    var expanded by remember { mutableStateOf(depth < 2) }
    Row(
        Modifier.fillMaxWidth()
            .clickable { onSelect(node) }
            .padding(start = (depth * 14).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        if (node.children.isNotEmpty()) {
            Text(
                if (expanded) "▾ " else "▸ ",
                fontSize = 11.sp,
                color = Color(0xFF797979),
                modifier = Modifier.clickable { expanded = !expanded },
            )
        } else {
            Text("  ", fontSize = 11.sp)
        }
        Text(node.label, fontSize = 12.sp, color = Color(0xFF1A1A1A))
        if (node.line > 0) {
            Text("  :${node.line}", fontSize = 11.sp, color = Color(0xFF9AA0A6), fontFamily = FontFamily.Monospace)
        }
    }
    if (expanded) node.children.forEach { TreeRow(it, depth + 1, onSelect) }
}

@Composable
private fun PagePicker(pages: List<PageInfo>, currentId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = pages.firstOrNull { it.id == currentId } ?: pages.first()
    Column {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("頁面：${current.label}  ▾")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            pages.forEach { p ->
                DropdownMenuItem(text = { Text(p.label) }, onClick = { expanded = false; onSelect(p.id) })
            }
        }
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
