package com.viewsonic.classswift.feature.servicescreens.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode

private val avatarColors = listOf(
    Color(0xFF0A8CF0), Color(0xFF78CB3D), Color(0xFFF4BA00), Color(0xFFF04869),
    Color(0xFF4848F0), Color(0xFF00B5AD), Color(0xFFFF7A45), Color(0xFF9254DE),
)

@Composable
private fun CodeTile(ch: Char) {
    Box(
        Modifier.width(18.dp).height(34.66.dp).clip(RoundedCornerShape(5.33.dp)).background(Neutral300),
        contentAlignment = Alignment.Center,
    ) { Text(ch.toString(), color = Dark2E3133, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun AttendeeCell(name: String, index: Int, modifier: Modifier = Modifier) {
    Column(modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(avatarColors[index % avatarColors.size]),
            contentAlignment = Alignment.Center,
        ) { Text(name.first().toString(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        Text(name, color = Dark2E3133, fontSize = 9.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * CMP port of `JoinClassWindow` (service path) — primary "join" state: class header, join URL +
 * class-code tiles, QR, and the live attendance grid. (The alternate states — expanded QR, empty/
 * disconnected, and the leave/remove dialogs & tooltips — are deferred enhancements.)
 */
@Composable
fun JoinClassScreen(
    className: String = "Grade 5 — Mathematics",
    joinUrl: String = "classswift.io/j",
    classCode: String = "CSW4821X",
    attendees: List<String> = sampleStudents.take(11).map { it.name },
    capacity: Int = 30,
    onClose: () -> Unit = {},
) {
    Column(
        Modifier.width(333.33.dp).height(565.33.dp)
            .clip(RoundedCornerShape(10.66.dp))
            .background(Color.White)
            .border(1.33.dp, BorderC2C2C2, RoundedCornerShape(10.66.dp))
            .padding(16.dp)
            .designNode("join_class"),
    ) {
        // ---- Title bar ----
        Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Join Class", color = Dark2E3133, fontSize = 14.4.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("jc_title"))
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(16.dp).clip(CircleShape).background(Neutral300).clickable(onClick = onClose).designNode("jc_close"), contentAlignment = Alignment.Center) {
                Text("×", color = CloseGray, fontSize = 13.sp)
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(1.dp).background(Neutral300))

        // ---- Class info row ----
        Row(Modifier.fillMaxWidth().height(45.33.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(className, color = Dark2E3133, fontSize = 13.3.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f).designNode("jc_class_name"))
            CSButton("Switch Class", backgroundColor = Color.White, textColor = Violet4848F0, borderColor = Violet4848F0, textSize = 10.sp, nodeId = "jc_switch", modifier = Modifier.height(28.dp).width(96.dp).padding(start = 4.dp))
        }

        // ---- Join info card (URL + code tiles | QR) ----
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Neutral100).padding(12.dp).designNode("jc_join_info"),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Step 1 · Open the link", color = CloseGray, fontSize = 9.sp)
                Text(joinUrl, color = Violet4848F0, fontSize = 18.7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("jc_url"))
                Text("Step 2 · Enter the code", color = CloseGray, fontSize = 9.sp, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    classCode.forEach { CodeTile(it) }
                }
            }
            Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Scan QR", color = CloseGray, fontSize = 9.sp, modifier = Modifier.padding(bottom = 4.dp))
                QrMatrix(joinUrl + classCode, Modifier.size(81.33.dp).designNode("jc_qr"))
            }
        }

        // ---- Attendance ----
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Whole Class", color = Dark2E3133, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${attendees.size} joined / $capacity students", color = CloseGray, fontSize = 9.sp, modifier = Modifier.designNode("jc_count"))
        }
        Column(
            Modifier.padding(top = 8.dp).weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Neutral100).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            attendees.chunked(4).forEachIndexed { rowIdx, rowNames ->
                Row(Modifier.fillMaxWidth()) {
                    rowNames.forEachIndexed { i, name ->
                        AttendeeCell(name, rowIdx * 4 + i, Modifier.weight(1f))
                    }
                    repeat(4 - rowNames.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
