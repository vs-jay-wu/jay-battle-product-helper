package com.viewsonic.designershell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
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

private fun adapterFor(target: String): TargetAdapter =
    if (target == "flutter") FlutterAdapter(FLUTTER_SHOP) else ComposeAdapter(RAGDOLL_CAT)

private fun repoLabel(target: String): String =
    if (target == "flutter") "Flutter · flutter_shop" else "Compose · ragdoll-cat"

private fun sessionPreview(s: Session): String =
    if (s.transcript.isEmpty()) "尚無對話"
    else "${s.transcript.size} 則訊息 · ${s.transcript.last().text.take(34).replace("\n", " ")}…"

/**
 * Standalone Designer Shell. Pick a project (repo) → its app launches once and
 * stays running; the user switches between Claude conversations ("sessions")
 * that all work on that same app, without rebuilding it.
 */
fun main() = application {
    val store = remember { SessionStore() }
    val state = rememberWindowState(width = 560.dp, height = 880.dp)
    Window(onCloseRequest = ::exitApplication, state = state, title = "Designer Shell") {
        var repo by remember { mutableStateOf<String?>(null) }
        MenuBar {
            Menu("專案", mnemonic = 'P') {
                Item("Flutter · flutter_shop") { repo = "flutter" }
                Item("Compose · ragdoll-cat") { repo = "compose" }
                Separator()
                if (repo != null) Item("切換專案(回到選擇)") { repo = null }
            }
        }
        when (val r = repo) {
            null -> RepoPicker(onPick = { repo = it })
            else -> key(r) { RepoWorkspace(r, store) }
        }
    }
}

@Composable
private fun RepoPicker(onPick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF4F4F6)).padding(24.dp)) {
        Text("Designer Shell", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
        Text("選擇一個專案 — 選了就會啟動該 app", color = Color(0xFF797979), fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onPick("flutter") }, modifier = Modifier.fillMaxWidth()) { Text(repoLabel("flutter")) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onPick("compose") }, modifier = Modifier.fillMaxWidth()) { Text(repoLabel("compose")) }
    }
}

/**
 * One running app (per repo). The adapter is started once and persists; switching
 * the active Claude session only swaps the conversation — the app is untouched.
 */
@Composable
private fun RepoWorkspace(repo: String, store: SessionStore) {
    val adapter = remember { adapterFor(repo) }
    var status by remember { mutableStateOf("啟動中…") }
    var designMode by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<SelectedNode?>(null) }
    var tree by remember { mutableStateOf<List<TreeNode>>(emptyList()) }
    var pages by remember { mutableStateOf<List<PageInfo>>(emptyList()) }
    var currentPageId by remember { mutableStateOf<String?>(null) }

    var sessions by remember { mutableStateOf(store.list().filter { it.target == repo }) }
    var active by remember {
        mutableStateOf(
            sessions.firstOrNull() ?: store.create("對話 1", repo).also { sessions = store.list().filter { it.target == repo } },
        )
    }
    var renaming by remember { mutableStateOf(false) }

    // App: started ONCE for this repo, persists across session switches.
    LaunchedEffect(Unit) {
        adapter.onStatus = { status = it; if (it == "已連線") adapter.requestPages() }
        adapter.onSelection = { selection = it }
        adapter.onTree = { tree = it }
        adapter.onPages = { pages = it; if (currentPageId == null) currentPageId = it.firstOrNull()?.id }
        adapter.start()
    }
    DisposableEffect(Unit) { onDispose { runCatching { adapter.stop() } } }

    // Claude conversation: swaps on session switch; the app/adapter is NOT touched.
    val claude = remember(active.id) { ClaudeSession(adapter.workingDir, active.claudeSessionId) }
    val hadHistory = remember(active.id) { active.transcript.isNotEmpty() }
    var everStarted by remember(active.id) { mutableStateOf(false) }
    LaunchedEffect(active.id) {
        claude.seed(active.transcript.map { ClaudeMessage(ClaudeRole.valueOf(it.role), it.text) })
        claude.onComplete = { adapter.hotReload() }
    }
    LaunchedEffect(active.id) {
        val s = active
        snapshotFlow { claude.transcript.size }.collect {
            val updated = s.copy(transcript = claude.transcript.map { StoredMessage(it.role.name, it.text) })
            store.save(updated)
            sessions = sessions.map { if (it.id == updated.id) updated else it }
        }
    }
    DisposableEffect(active.id) {
        val s = active
        val c = claude
        onDispose {
            runCatching { store.save(s.copy(transcript = c.transcript.map { StoredMessage(it.role.name, it.text) })) }
            runCatching { c.stop() }
        }
    }

    // Auto-refresh the structure tree while in design mode (manual ↻ still forces it).
    LaunchedEffect(designMode) {
        while (designMode) {
            adapter.requestTree()
            kotlinx.coroutines.delay(1500)
        }
    }

    val onSend: (String) -> Unit = { text ->
        if (!claude.running) {
            claude.start(resume = hadHistory && !everStarted)
            everStarted = true
        }
        claude.send(buildPrompt(text, selection))
    }

    if (renaming) {
        RenameDialog(active.name, onConfirm = { newName ->
            val updated = active.copy(name = newName)
            store.save(updated)
            sessions = sessions.map { if (it.id == updated.id) updated else it }
            active = updated
            renaming = false
        }, onDismiss = { renaming = false })
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF4F4F6)).padding(16.dp)) {
        Text(repoLabel(repo), color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("app：$status", color = Color(0xFF797979), fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        SessionSwitcher(
            sessions = sessions,
            active = active,
            onSwitch = { active = it },
            onNew = {
                val s = store.create("對話 ${sessions.size + 1}", repo)
                sessions = store.list().filter { it.target == repo }
                active = s
            },
            onRename = { renaming = true },
        )
        Spacer(Modifier.height(10.dp))

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
        ClaudeCard(active.name, claude, onSend, Modifier.weight(1f))
    }
}

@Composable
private fun SessionSwitcher(
    sessions: List<Session>,
    active: Session,
    onSwitch: (Session) -> Unit,
    onNew: () -> Unit,
    onRename: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            Button(onClick = { expanded = true }) { Text("對話：${active.name}  ▾") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                sessions.forEach { s ->
                    DropdownMenuItem(
                        text = { Text((if (s.id == active.id) "● " else "   ") + s.name + "   —   " + sessionPreview(s)) },
                        onClick = { expanded = false; onSwitch(s) },
                    )
                }
            }
        }
        TextButton(onClick = onNew) { Text("＋ 新對話") }
        TextButton(onClick = onRename) { Text("改名") }
    }
}

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重新命名對話") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("確定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun PagePicker(pages: List<PageInfo>, currentId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = pages.firstOrNull { it.id == currentId } ?: pages.first()
    Column {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("頁面：${current.label}  ▾") }
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
private fun StructureCard(
    tree: List<TreeNode>,
    onRefresh: () -> Unit,
    onSelect: (TreeNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        Modifier.fillMaxWidth().clickable { onSelect(node) }.padding(start = (depth * 14).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (node.children.isNotEmpty()) {
            Text(
                if (expanded) "▾ " else "▸ ",
                fontSize = 11.sp, color = Color(0xFF797979),
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
private fun ClaudeCard(sessionName: String, session: ClaudeSession, onSend: (String) -> Unit, modifier: Modifier = Modifier) {
    var prompt by remember { mutableStateOf("") }
    Column(modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
        Text("Claude · $sessionName", fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
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
        Button(onClick = { onSend(prompt); prompt = "" }, enabled = prompt.isNotBlank()) { Text("送出") }
    }
}

private fun buildPrompt(text: String, selection: SelectedNode?): String {
    if (selection == null) return text
    return "$text\n\n(目標元件：${selection.desc}，位於 ${selection.file}:${selection.line}。" +
        "請定位該元件並修改，只動必要的部分。)"
}
