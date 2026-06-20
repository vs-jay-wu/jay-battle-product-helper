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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_avatar_student_01
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_avatar_student_02
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_avatar_student_03
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_avatar_student_04
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_avatar_student_joining
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_avatar_student_not_joined
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_class_management
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_copy_link
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_info_circle
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_join_class_empty
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_minus_32dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_join_class_spinner
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_bring_to_front_64dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_sign_out
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_user_document
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_zoom_in
import org.jetbrains.compose.resources.painterResource

private val Divider = Color(0xFFE6E6E6)
private val Neutral400Grey = Color(0xFFCFCFCF)
private val Neutral600Grey = Color(0xFF999999)
private val joinedAvatars = listOf(
    Res.drawable.ic_avatar_student_01, Res.drawable.ic_avatar_student_02,
    Res.drawable.ic_avatar_student_03, Res.drawable.ic_avatar_student_04,
)

enum class AttendeeState { JOINED, NOT_JOINED, JOINING }

/** A class member; mirrors `item_join_class_student.xml`: name on top + avatar illustration below. */
data class JoinAttendee(val name: String, val state: AttendeeState = AttendeeState.JOINED, val avatarIndex: Int = 0)

/** `item_class_code_tile.xml`: 18×34.66dp, neutral_300 bg radius 5.33, #333333 14.4sp bold. */
@Composable
private fun CodeTile(ch: Char) {
    Box(
        Modifier.padding(end = 2.66.dp).width(18.dp).height(34.66.dp)
            .clip(RoundedCornerShape(5.33.dp)).background(Neutral300),
        contentAlignment = Alignment.Center,
    ) { Text(ch.toString(), color = Neutral900, fontSize = 14.4.sp, fontWeight = FontWeight.Bold) }
}

/** `item_join_class_student.xml`: 69.33dp white card, name (10sp, 2 lines, top) + avatar (42×24, bottom). */
@Composable
private fun AttendeeCell(attendee: JoinAttendee, modifier: Modifier = Modifier) {
    val avatar = when (attendee.state) {
        AttendeeState.JOINED -> joinedAvatars[attendee.avatarIndex.mod(joinedAvatars.size)]
        AttendeeState.NOT_JOINED -> Res.drawable.ic_avatar_student_not_joined
        AttendeeState.JOINING -> Res.drawable.ic_avatar_student_joining
    }
    val nameColor = when (attendee.state) {
        AttendeeState.JOINED -> Neutral900
        AttendeeState.NOT_JOINED -> Neutral400Grey
        AttendeeState.JOINING -> Neutral600Grey
    }
    // Layout per item_join_class_student.xml: empty top, then name (bottom-anchored to the avatar),
    // then the avatar flush to the card bottom.
    Column(
        modifier.padding(2.dp).height(69.33.dp).clip(RoundedCornerShape(5.33.dp)).background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            attendee.name, color = nameColor, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.width(54.dp).height(24.67.dp),
        )
        Image(painterResource(avatar), null, Modifier.width(42.dp).height(24.dp))
    }
}

/**
 * CMP port of `JoinClassWindow` (service path) — faithful to `window_join_class.xml` primary state:
 * title bar (icon + minimize/front/close), class-info row, join card (Step 1 link + copy, Step 2
 * code tiles | Scan-to-join QR + expand), and the Whole-Class attendance panel (empty illustration
 * or student grid). [qr] is a slot so the app can inject the real scannable QR bitmap.
 */
@Composable
fun JoinClassScreen(
    className: String = "Science 509",
    joinUrl: String = "s.mvb.fyi/join",
    classCode: String = "31730488",
    attendees: List<JoinAttendee> = emptyList(),
    joinedCount: Int = attendees.count { it.state == AttendeeState.JOINED },
    capacity: Int = 14,
    isGuestMode: Boolean = false,
    showFractionCount: Boolean = true,
    spinnerVisible: Boolean = true,
    qr: @Composable (Modifier) -> Unit = { m -> QrMatrix(joinUrl + classCode, m) },
    onClose: () -> Unit = {},
    onCopyLink: () -> Unit = {},
    onExpandQr: () -> Unit = {},
    onSwitchClass: () -> Unit = {},
) {
    Column(
        Modifier.width(333.33.dp).height(565.33.dp)
            .clip(RoundedCornerShape(10.66.dp))
            .background(Color.White)
            .border(1.33.dp, BorderC2C2C2, RoundedCornerShape(10.66.dp))
            .designNode("join_class"),
    ) {
        // ── Title bar ──
        Row(
            Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 10.66.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(Res.drawable.ic_class_management), null, Modifier.size(21.33.dp))
            Text("Join Class", color = Dark2E3133, fontSize = 14.4.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 5.33.dp, end = 8.dp).designNode("jc_title"))
            Image(painterResource(Res.drawable.ic_minus_32dp), "Minimize", Modifier.padding(end = 8.dp).size(21.3.dp), colorFilter = ColorFilter.tint(Neutral900))
            Image(painterResource(Res.drawable.ic_toolbar_bring_to_front_64dp), "Bring to front", Modifier.padding(end = 8.dp).size(21.3.dp), colorFilter = ColorFilter.tint(Neutral900))
            Image(painterResource(Res.drawable.ic_close), "Close", Modifier.size(21.3.dp).clickable(onClick = onClose).designNode("jc_close"), colorFilter = ColorFilter.tint(Neutral900))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))

        // ── ① Class info row ──
        Row(
            Modifier.fillMaxWidth().height(45.33.dp).padding(horizontal = 10.66.dp).padding(top = 10.66.dp)
                .clip(RoundedCornerShape(10.66.dp)).background(Neutral100).padding(horizontal = 10.66.dp)
                .designNode("jc_class_info"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(Res.drawable.ic_user_document), null, Modifier.size(18.66.dp))
            Text(className, color = Dark2E3133, fontSize = 13.3.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 5.33.dp))
            if (!isGuestMode) {
                Row(
                    Modifier.height(28.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).border(0.66.dp, Neutral300, RoundedCornerShape(8.dp)).padding(horizontal = 10.66.dp).clickable(onClick = onSwitchClass).designNode("jc_switch"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(Res.drawable.ic_toolbar_irs_sign_out), null, Modifier.size(10.66.dp))
                    Text("Switch Class", color = Dark2E3133, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        // ── ② Join info card ──
        Row(Modifier.fillMaxWidth().height(132.dp).padding(horizontal = 10.66.dp).padding(top = 10.66.dp).designNode("jc_join_info")) {
            // Left: steps
            Column(Modifier.width(184.dp).fillMaxHeight().clip(RoundedCornerShape(10.66.dp)).background(Neutral100).padding(10.66.dp)) {
                Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Step 1: Join the class", color = Dark2E3133, fontSize = 10.7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Image(painterResource(Res.drawable.ic_copy_link), "Copy link", Modifier.padding(start = 4.dp).size(24.dp).clickable(onClick = onCopyLink).designNode("jc_copy"))
                }
                Text(joinUrl, color = Violet4848F0, fontSize = 18.7.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp).designNode("jc_url"))
                Text("Step 2: Enter class ID", color = Neutral900, fontSize = 10.7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.padding(top = 4.dp)) { classCode.forEach { CodeTile(it) } }
            }
            // Right: QR
            Column(Modifier.padding(start = 10.66.dp).width(117.33.dp).fillMaxHeight().clip(RoundedCornerShape(10.66.dp)).background(Neutral100).padding(10.66.dp)) {
                Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Scan to join", color = Neutral900, fontSize = 10.7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Image(painterResource(Res.drawable.ic_zoom_in), "Expand QR", Modifier.size(24.dp).clickable(onClick = onExpandQr).designNode("jc_qr_expand"))
                }
                qr(Modifier.padding(top = 2.66.dp).size(81.33.dp).designNode("jc_qr"))
            }
        }

        // ── ④ Whole-class attendance panel ──
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp).padding(top = 10.66.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(10.66.dp)).background(Neutral100).padding(start = 10.66.dp, top = 10.66.dp, end = 10.66.dp)
                .designNode("jc_student_panel"),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Whole Class", color = Neutral900, fontSize = 10.7.sp, fontWeight = FontWeight.Bold)
                if (spinnerVisible) {
                    Image(painterResource(Res.drawable.ic_mvb_join_class_spinner), "Spinner", Modifier.padding(start = 5.33.dp).size(24.dp).designNode("jc_spinner"))
                }
                Spacer(Modifier.weight(1f))
                val countText = if (showFractionCount) "$joinedCount joined / $capacity students" else "$joinedCount joined"
                Text(countText, color = Neutral900, fontSize = 10.7.sp, modifier = Modifier.designNode("jc_count"))
                Image(painterResource(Res.drawable.ic_info_circle), null, Modifier.padding(start = 5.33.dp).size(16.dp), colorFilter = ColorFilter.tint(Neutral900))
            }
            if (attendees.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(horizontal = 10.66.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Image(painterResource(Res.drawable.ic_join_class_empty), null, Modifier.size(100.dp).designNode("jc_empty_img"))
                    Text("Invite students to join\nwith a class code or QR code.", color = Gray999, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp).fillMaxWidth().designNode("jc_empty_text"))
                }
            } else {
                Column(Modifier.padding(top = 4.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    attendees.chunked(4).forEach { rowAttendees ->
                        Row(Modifier.fillMaxWidth()) {
                            rowAttendees.forEach { a -> AttendeeCell(a, Modifier.weight(1f)) }
                            repeat(4 - rowAttendees.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}
