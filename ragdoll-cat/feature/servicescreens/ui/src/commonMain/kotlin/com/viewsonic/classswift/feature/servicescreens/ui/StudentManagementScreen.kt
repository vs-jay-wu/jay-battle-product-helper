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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_copy_blue
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_leaderboard
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_left_arrow_40dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_minus_32dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_people
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_bring_to_front_64dp
import org.jetbrains.compose.resources.painterResource

/** A student seat: roster number, display name, running score. */
data class Student(val id: String, val seat: String, val name: String, val score: Int)

val sampleStudents = listOf(
    Student("1", "01", "Brandon Wang", 5), Student("2", "02", "Emily Chen", 8),
    Student("3", "03", "Marcus Lee", 3), Student("4", "04", "Sophia Liu", 6),
    Student("5", "05", "Daniel Wu", 0), Student("6", "06", "Olivia Yang", 9),
    Student("7", "07", "Ethan Kao", 2), Student("8", "08", "Mia Hsu", 7),
    Student("9", "09", "Lucas Lin", 4), Student("10", "10", "Chloe Tsai", 5),
)

@Composable
private fun ScorePill(label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.width(24.7.dp).padding(vertical = 3.6.dp).clip(RoundedCornerShape(4.8.dp)).background(tint).clickable(onClick = onClick).padding(vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 10.8.sp) }
}

/** One seat card — `item_student_list.xml`: blue number header, name, −1/score/+1 row. */
@Composable
private fun StudentCard(s: Student, onInc: () -> Unit, onDec: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Column(
        modifier.padding(6.dp).clip(shape).background(Color.White).border(0.8.dp, BrandBlue, shape).designNode("sm_student_${s.id}"),
    ) {
        Text(
            s.seat, color = Color.White, fontSize = 14.4.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(BrandBlue).padding(vertical = 3.dp),
        )
        Text(s.name, color = Dark2E3133, fontSize = 16.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().height(42.6.dp).padding(horizontal = 4.dp))
        Box(Modifier.fillMaxWidth().height(0.8.dp).background(BrandBlue))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(4.2.dp))
            ScorePill("- 1", ScoreRed, onDec)
            Text(s.score.toString(), color = Dark2E3133, fontSize = 14.4.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            ScorePill("+ 1", ScoreGreen, onInc)
            Spacer(Modifier.width(4.2.dp))
        }
    }
}

@Composable
private fun SmTabItem(label: String, selected: Boolean, onClick: () -> Unit, nodeId: String) {
    Box(
        Modifier.height(33.6.dp).widthIn().clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(if (selected) Color.White else Color(0xFFD6D6D6))
            .clickable(onClick = onClick).padding(horizontal = 14.4.dp).designNode(nodeId),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) BrandBlue else Dark2E3133, fontSize = 14.4.sp) }
}

private fun Modifier.widthIn() = this.width(114.dp)

/** Which body the student panel shows. */
enum class SmPhase { LOADING, LIST, FAILED }

/**
 * CMP port of `StudentManagementWindow` (service path) — STEP 1: Student List tab. Left = class
 * title/count/leaderboard, ID + Link (copy), QR; right = tabs + 5-col student grid with score +/-.
 * Groups tab, more-menu, edit-mode (remove), reload action are deferred to step 2. [qr] is a slot
 * for the real scannable code.
 */
@Composable
fun StudentManagementScreen(
    classTitle: String = "Join 6th Grade Science",
    classId: String = "ID: X58E9647",
    countText: String = "00/30",
    students: List<Student> = sampleStudents,
    phase: SmPhase = SmPhase.LIST,
    backVisible: Boolean = true,
    qr: @Composable (Modifier) -> Unit = { m -> QrMatrix(classId, m) },
    onIncrease: (String) -> Unit = {},
    onDecrease: (String) -> Unit = {},
    onCopyId: () -> Unit = {},
    onCopyLink: () -> Unit = {},
    onLeaderboard: () -> Unit = {},
    onBack: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    Box(
        Modifier.width(976.8.dp).height(556.dp).clip(RoundedCornerShape(10.66.dp)).background(WindowBgF5F5F5).border(0.96.dp, StrokeC3C7C7, RoundedCornerShape(10.66.dp)).designNode("student_management"),
    ) {
        Row(Modifier.fillMaxSize()) {
            // ---- Left: class info + QR ----
            Column(Modifier.width(374.46.dp).fillMaxHeight().padding(start = 30.66.dp).padding(vertical = 30.66.dp)) {
                Row(
                    Modifier.height(32.dp).then(if (backVisible) Modifier.clickable(onClick = onBack) else Modifier).designNode("sm_back"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (backVisible) {
                        Image(painterResource(Res.drawable.ic_left_arrow_40dp), null, Modifier.size(13.3.dp))
                        Text("Back to Class List", color = Dark2E3133, fontSize = 13.3.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Row(Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(classTitle, color = Dark2E3133, fontSize = 21.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).designNode("sm_title"))
                    Image(painterResource(Res.drawable.ic_people), null, Modifier.padding(start = 8.dp).size(21.6.dp))
                    Text(countText, color = Dark2E3133, fontSize = 14.4.sp, modifier = Modifier.padding(start = 1.33.dp).designNode("sm_count"))
                    Spacer(Modifier.weight(1f))
                    Image(painterResource(Res.drawable.ic_leaderboard), "Leaderboard", Modifier.padding(start = 8.dp).size(32.dp).clickable(onClick = onLeaderboard).padding(6.dp).designNode("sm_leaderboard"))
                }
                Row(Modifier.padding(top = 14.6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(classId, color = Dark2E3133, fontSize = 19.2.sp, maxLines = 1, modifier = Modifier.designNode("sm_id"))
                    Image(painterResource(Res.drawable.ic_copy_blue), "Copy ID", Modifier.padding(start = 8.dp).size(24.dp).clickable(onClick = onCopyId).designNode("sm_copy_id"))
                    Text("Link", color = Dark2E3133, fontSize = 19.2.sp, modifier = Modifier.padding(start = 30.6.dp))
                    Image(painterResource(Res.drawable.ic_copy_blue), "Copy Link", Modifier.padding(start = 8.dp).size(24.dp).clickable(onClick = onCopyLink).designNode("sm_copy_link"))
                }
                qr(Modifier.padding(top = 15.32.dp).fillMaxWidth().aspectRatio(1f).designNode("sm_qr"))
            }
            // ---- Right: tabs + student grid ----
            Column(Modifier.width(521.33.dp).fillMaxHeight().padding(start = 19.69.dp, end = 30.66.dp).padding(vertical = 30.66.dp)) {
                Row(Modifier.padding(horizontal = 27.33.dp)) {
                    SmTabItem("Whole Class", selected = true, onClick = {}, nodeId = "sm_tab_students")
                    Spacer(Modifier.width(8.dp))
                    SmTabItem("Groups", selected = false, onClick = {}, nodeId = "sm_tab_groups")
                }
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Color.White).padding(horizontal = 20.6.dp), contentAlignment = Alignment.Center) {
                    when (phase) {
                        SmPhase.LOADING -> CircularProgressIndicator(Modifier.size(27.15.dp), color = BrandBlue, strokeWidth = 2.dp)
                        SmPhase.FAILED -> Text("Failed to find student list", color = Dark2E3133, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        SmPhase.LIST -> Column(Modifier.fillMaxSize().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            students.chunked(5).forEach { rowStudents ->
                                Row(Modifier.fillMaxWidth()) {
                                    rowStudents.forEach { s -> StudentCard(s, { onIncrease(s.id) }, { onDecrease(s.id) }, Modifier.weight(1f)) }
                                    repeat(5 - rowStudents.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        }
        // ---- Window controls ----
        Row(Modifier.align(Alignment.TopEnd).padding(top = 10.6.dp, end = 10.6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(Res.drawable.ic_minus_32dp), "Minimize", Modifier.size(21.3.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Neutral900))
            Image(painterResource(Res.drawable.ic_toolbar_bring_to_front_64dp), "Bring to front", Modifier.size(21.3.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Neutral900))
            Image(painterResource(Res.drawable.ic_close), "Close", Modifier.size(21.3.dp).clickable(onClick = onClose).designNode("sm_close"), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Neutral900))
        }
    }
}
