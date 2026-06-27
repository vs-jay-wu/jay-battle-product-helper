package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_maintenance
import org.jetbrains.compose.resources.painterResource

/**
 * Shared 413dp maintenance card — faithful to `window_under_maintenance.xml` /
 * `window_upcoming_maintenance.xml` (identical layout, different copy): illustration,
 * title, description, blue "Got it" button, close icon.
 */
@Composable
private fun MaintenanceCard(
    title: String,
    description: AnnotatedString,
    nodeId: String,
    onGotIt: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier.width(413.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.33.dp, BorderC2C2C2, RoundedCornerShape(8.dp))
            .designNode(nodeId),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 26.66.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_maintenance),
                contentDescription = null,
                modifier = Modifier.padding(top = 21.33.dp).size(207.dp, 155.dp).designNode("${nodeId}_image"),
            )
            Text(
                title,
                color = Dark2E3133,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.66.dp).fillMaxWidth().designNode("${nodeId}_title"),
            )
            Text(
                description,
                color = Dark2E3133,
                fontSize = 13.3.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth().designNode("${nodeId}_description"),
            )
            Box(
                Modifier.padding(top = 16.dp, bottom = 26.66.dp)
                    .fillMaxWidth()
                    .height(37.5.dp)
                    .clip(RoundedCornerShape(5.33.dp))
                    .background(BrandBlue)
                    .clickable(onClick = onGotIt)
                    .designNode("${nodeId}_got_it"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Got it", color = Color.White, fontSize = 16.sp)
            }
        }
        Image(
            painter = painterResource(Res.drawable.ic_close),
            contentDescription = "Close",
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 10.7.dp, end = 10.7.dp)
                .size(21.3.dp)
                .clickable(onClick = onClose)
                .designNode("${nodeId}_close"),
        )
    }
}

/**
 * Builds a maintenance description where the date/time is tinted brand blue — mirrors the app's
 * `MaintenanceAnnouncementsUiManager.replacePlaceholders` (ForegroundColorSpan #0A8CF0). Used for
 * the Designer Shell preview defaults; the app injects the real converted text.
 */
private fun maintenanceDesc(before: String, blueDateTime: String, after: String): AnnotatedString =
    buildAnnotatedString {
        append(before)
        withStyle(SpanStyle(color = BrandBlue)) { append(blueDateTime) }
        append(after)
    }

/** CMP port of `UnderMaintenanceWindow` (service path). */
@Composable
fun UnderMaintenanceScreen(
    title: String = "ClassSwift is Under Maintenance",
    description: AnnotatedString = maintenanceDesc(
        "We’ll be back on ",
        "Sep. 22, 2025 (Tue) 17:00",
        ".\nDuring maintenance, the ClassSwift application, ClassSwift Hub and account " +
            "registration will be unavailable.\nThank you for your patience!",
    ),
    onGotIt: () -> Unit = {},
    onClose: () -> Unit = {},
) = MaintenanceCard(title, description, "under_maintenance", onGotIt, onClose)

/** CMP port of `UpcomingMaintenanceWindow` (service path) — same card, scheduled-downtime copy. */
@Composable
fun UpcomingMaintenanceScreen(
    title: String = "Upcoming Maintenance",
    description: AnnotatedString = maintenanceDesc(
        "ClassSwift will be down for scheduled maintenance at ",
        "Sep. 22, 2025 (Tue) 17:00",
        ". During maintenance, ClassSwift APP and Hub will not be accessible.\n\n" +
            "Thank you for your patience as ClassSwift team is working hard to improve your experience.",
    ),
    onGotIt: () -> Unit = {},
    onClose: () -> Unit = {},
) = MaintenanceCard(title, description, "upcoming_maintenance", onGotIt, onClose)
