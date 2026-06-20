package com.viewsonic.designershell.probe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay

/**
 * De-risk: tap point -> composable source file:line on Compose. Replicates the
 * core of ui-tooling's Inspectable (collectParameterInformation + compositionData)
 * since that helper isn't shipped for Compose Desktop. Output to stdout.
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "source-location probe") {
        var data by remember { mutableStateOf<CompositionData?>(null) }
        var done by remember { mutableStateOf(false) }
        Capture(onData = { data = it }) { ProbeUi() }
        LaunchedEffect(data) {
            val d = data
            if (d != null && !done) {
                delay(800)
                analyze(d)
                done = true
            }
        }
    }
}

@OptIn(InternalComposeApi::class)
@Composable
private fun Capture(onData: (CompositionData) -> Unit, content: @Composable () -> Unit) {
    currentComposer.collectParameterInformation()
    onData(currentComposer.compositionData)
    content()
}

@Composable
private fun ProbeUi() {
    Column(Modifier.padding(24.dp)) {
        Text("Alpha title")
        Text("Bravo subtitle")
        Button(onClick = {}) { Text("Charlie button") }
    }
}

@OptIn(UiToolingDataApi::class)
private fun analyze(data: CompositionData) {
    val all = mutableListOf<Group>()
    val withLoc = mutableListOf<Group>()
    fun walk(g: Group) {
        all.add(g)
        if (g.location != null) withLoc.add(g)
        g.children.forEach(::walk)
    }
    walk(data.asTree())

    println("=== PROBE: total groups=${all.size}, withLocation=${withLoc.size} ===")
    withLoc.filter { it.name != null }.take(30).forEach { g ->
        val l = g.location!!
        println("  ${g.name}  ${l.sourceFile}:${l.lineNumber}  box=${g.box}")
    }
    for (pt in listOf(IntOffset(60, 50), IntOffset(60, 90), IntOffset(80, 140))) {
        val hit = withLoc
            .filter { it.box.contains(pt) && it.box.width > 0 && it.box.height > 0 }
            .minByOrNull { it.box.width.toLong() * it.box.height.toLong() }
        val loc = hit?.location
        println("  HIT $pt -> ${if (loc == null) "(none)" else "${hit.name} @ ${loc.sourceFile}:${loc.lineNumber}"}")
    }
    println("=== PROBE END ===")
}
