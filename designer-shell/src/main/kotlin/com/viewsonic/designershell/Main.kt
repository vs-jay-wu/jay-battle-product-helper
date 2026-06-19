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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.viewsonic.designershell.adapter.FlutterAdapter
import com.viewsonic.designershell.adapter.SelectedNode
import com.viewsonic.designershell.adapter.TargetAdapter

private const val FLUTTER_SHOP =
    "/Users/jay.wj.wu/ProjectsWork_GitHub/Battle/jay-battle-product-helper/flutter_shop"

/**
 * Standalone Designer Shell — a generic out-of-process control panel. It hosts
 * any target through a [TargetAdapter] (Flutter today; Android/etc. later), so a
 * single shell serves many independent app repos without compiling against them.
 */
fun main() = application {
    val state = rememberWindowState(width = 560.dp, height = 840.dp)
    Window(onCloseRequest = ::exitApplication, state = state, title = "Designer Shell · 控制面板") {
        // Target selection will become a picker; for now, the Flutter adapter.
        ControlPanel(remember { FlutterAdapter(FLUTTER_SHOP) })
    }
}

@Composable
private fun ControlPanel(adapter: TargetAdapter) {
    var status by remember { mutableStateOf("啟動中…") }
    var designMode by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<SelectedNode?>(null) }
    val session = remember { ClaudeSession(adapter.workingDir) }

    LaunchedEffect(Unit) {
        adapter.onStatus = { status = it }
        adapter.onSelection = { selection = it }
        session.onComplete = { adapter.hotReload() }
        adapter.start()
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF4F4F6)).padding(16.dp)) {
        Text("Designer Shell · 控制面板", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
        ClaudeCard(session, selection, Modifier.weight(1f))
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
private fun ClaudeCard(session: ClaudeSession, selection: SelectedNode?, modifier: Modifier = Modifier) {
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
            onClick = {
                if (!session.running) session.start()
                session.send(buildPrompt(prompt, selection))
                prompt = ""
            },
            enabled = prompt.isNotBlank(),
        ) { Text("送出") }
    }
}

private fun buildPrompt(text: String, selection: SelectedNode?): String {
    if (selection == null) return text
    return "$text\n\n(目標元件：${selection.desc}，位於 ${selection.file}:${selection.line}。" +
        "請定位該元件並修改，只動必要的部分。)"
}
