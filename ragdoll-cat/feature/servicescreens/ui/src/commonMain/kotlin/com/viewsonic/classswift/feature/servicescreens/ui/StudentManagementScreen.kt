package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode

/** A student seat: roster number, display name, running score. */
data class Student(val number: String, val name: String, val score: Int)

val sampleStudents = listOf(
    Student("01", "Brandon Wang", 5), Student("02", "Emily Chen", 8),
    Student("03", "Marcus Lee", 3), Student("04", "Sophia Liu", 6),
    Student("05", "Daniel Wu", 0), Student("06", "Olivia Yang", 9),
    Student("07", "Ethan Kao", 2), Student("08", "Mia Hsu", 7),
    Student("09", "Lucas Lin", 4), Student("10", "Chloe Tsai", 5),
).let { it + it.take(5).mapIndexed { i, s -> s.copy(number = "1${i + 1}", name = s.name + " Jr.") } }

@Composable
private fun ScorePill(label: String, tint: Color) {
    Box(
        Modifier.width(24.7.dp).padding(vertical = 3.6.dp).clip(RoundedCornerShape(4.8.dp)).background(tint).padding(vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 10.8.sp) }
}

/** One seat card — blue number header, name, ± score row. Mirrors `item_student_list.xml`. */
@Composable
private fun StudentCard(s: Student, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Column(
        modifier.clip(shape).background(Color.White).border(0.8.dp, BrandBlue, shape).designNode("sm_student_${s.number}"),
    ) {
        Text(
            s.number, color = Color.White, fontSize = 14.4.sp,
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                .background(BrandBlue)
                .padding(vertical = 3.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            s.name, color = Dark2E3133, fontSize = 16.8.sp, maxLines = 1,
            modifier = Modifier.fillMaxWidth().height(42.6.dp).padding(horizontal = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(Modifier.fillMaxWidth().height(0.8.dp).background(BrandBlue))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(4.2.dp))
            ScorePill("- 1", ScoreRed)
            Text(s.score.toString(), color = Dark2E3133, fontSize = 14.4.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            ScorePill("+ 1", ScoreGreen)
            Spacer(Modifier.width(4.2.dp))
        }
    }
}

/** A generated QR-style matrix (deterministic from [seed]) — real rendered pixels, not a placeholder. */
@Composable
private fun QrMatrix(seed: String, modifier: Modifier = Modifier) {
    val n = 25
    val bits = BooleanArray(n * n)
    var h = 2166136261.toInt()
    for (c in seed) h = (h xor c.code) * 16777619
    for (i in bits.indices) {
        h = h * 1103515245 + 12345
        bits[i] = (h ushr 16 and 1) == 1
    }
    // Finder squares in 3 corners (top-left, top-right, bottom-left).
    fun finder(r0: Int, c0: Int) {
        for (r in 0..6) for (c in 0..6) {
            val edge = r == 0 || r == 6 || c == 0 || c == 6
            val core = r in 2..4 && c in 2..4
            bits[(r0 + r) * n + (c0 + c)] = edge || core
            if (!edge && !core) bits[(r0 + r) * n + (c0 + c)] = false
        }
    }
    finder(0, 0); finder(0, n - 7); finder(n - 7, 0)
    Canvas(modifier.background(Color.White)) {
        val cell = size.minDimension / n
        for (r in 0 until n) for (c in 0 until n) {
            if (bits[r * n + c]) {
                drawRect(Color.Black, topLeft = Offset(c * cell, r * cell), size = Size(cell, cell))
            }
        }
    }
}

@Composable
private fun CopyLine(label: String, value: String, nodeId: String) {
    Column(Modifier.padding(top = 12.dp).designNode(nodeId)) {
        Text(label, color = Neutral500, fontSize = 11.sp)
        Text(value, color = Dark2E3133, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** CMP port of `StudentManagementWindow` (service path): class info + QR (left), student grid (right). */
@Composable
fun StudentManagementScreen(
    className: String = "Grade 5 — Mathematics",
    joined: Int = 16,
    capacity: Int = 30,
    classId: String = "CSW-4821",
    joinLink: String = "classswift.io/j/4821",
    students: List<Student> = sampleStudents,
) {
    Box(
        Modifier.width(896.dp).height(470.dp)
            .clip(RoundedCornerShape(10.66.dp))
            .background(WindowBgF5F5F5)
            .border(0.96.dp, StrokeC3C7C7, RoundedCornerShape(10.66.dp))
            .designNode("student_management"),
    ) {
        Row(Modifier.fillMaxSize().padding(24.dp)) {
            // ---- Left: class info + QR ----
            Column(Modifier.width(320.dp).fillMaxSize()) {
                Text(className, color = Dark2E3133, fontSize = 21.6.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.designNode("sm_class_title"))
                Text("Students  $joined/$capacity", color = BrandBlue, fontSize = 14.4.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp).designNode("sm_count"))
                CopyLine("Class ID", classId, "sm_class_id")
                CopyLine("Join link", joinLink, "sm_link")
                QrMatrix(
                    joinLink,
                    Modifier.padding(top = 16.dp).size(150.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, StrokeC3C7C7, RoundedCornerShape(10.dp)).padding(8.dp).designNode("sm_qr"),
                )
            }
            Spacer(Modifier.width(16.dp))
            // ---- Right: tabs + student grid ----
            Column(Modifier.weight(1f).fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tab("Student List", selected = true)
                    Spacer(Modifier.width(8.dp))
                    Tab("Groups", selected = false)
                }
                Column(
                    Modifier.padding(top = 12.dp).weight(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)).background(Color.White).padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    students.chunked(5).forEach { rowStudents ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowStudents.forEach { s ->
                                StudentCard(s, Modifier.weight(1f))
                            }
                            repeat(5 - rowStudents.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tab(label: String, selected: Boolean) {
    val textColor = if (selected) BrandBlue else CloseGray
    Box(
        Modifier.width(114.dp).height(33.6.dp)
            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
            .background(if (selected) Color.White else WindowBgF5F5F5)
            .designNode("sm_tab_${label.replace(' ', '_')}"),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = textColor, fontSize = 14.4.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
}
