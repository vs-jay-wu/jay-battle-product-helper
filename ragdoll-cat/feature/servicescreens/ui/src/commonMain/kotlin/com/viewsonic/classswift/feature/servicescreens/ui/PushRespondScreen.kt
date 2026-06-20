package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource

@Composable
private fun Tab(label: String, selected: Boolean, onClick: () -> Unit, nodeId: String) {
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp).designNode(nodeId),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = if (selected) BrandBlue else CloseGray, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(vertical = 8.dp))
        Box(Modifier.width(80.dp).height(2.dp).background(if (selected) BrandBlue else Color.Transparent))
    }
}

/** CMP port of `PushRespondWindow` (service path): a sketch-response review window — title,
 * Content / Records tabs, and a grid of student submission cards. */
@Composable
fun PushRespondScreen(
    submissions: List<String> = sampleStudents.take(12).map { it.name },
    onClose: () -> Unit = {},
) {
    var tab by remember { mutableStateOf(0) }
    Box(
        Modifier.size(1080.dp, 588.dp).clip(RoundedCornerShape(10.66.dp)).background(WindowBgF5F5F5).border(0.96.dp, StrokeC3C7C7, RoundedCornerShape(10.66.dp)).designNode("push_respond"),
    ) {
        Image(
            painterResource(Res.drawable.ic_close), "Close",
            Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp).size(21.33.dp).clickable(onClick = onClose).designNode("pr_close"),
            colorFilter = ColorFilter.tint(Neutral900),
        )
        Column(Modifier.fillMaxSize().padding(32.dp)) {
            Text("Sketch Response", color = Neutral900, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("pr_title"))
            Row(Modifier.padding(top = 12.dp)) {
                Tab("Content", tab == 0, { tab = 0 }, "pr_tab_content")
                Tab("Records", tab == 1, { tab = 1 }, "pr_tab_records")
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Neutral300))
            // Submission grid (4 columns)
            Column(Modifier.padding(top = 16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                submissions.chunked(4).forEach { rowNames ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowNames.forEach { name -> SubmissionCard(name, Modifier.weight(1f)) }
                        repeat(4 - rowNames.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmissionCard(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier.height(110.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).border(0.66.dp, Neutral300, RoundedCornerShape(8.dp)).designNode("pr_card_$name"),
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text("✎", color = Neutral400, fontSize = 28.sp)
        }
        Text(name, color = Dark2E3133, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(8.dp))
    }
}
