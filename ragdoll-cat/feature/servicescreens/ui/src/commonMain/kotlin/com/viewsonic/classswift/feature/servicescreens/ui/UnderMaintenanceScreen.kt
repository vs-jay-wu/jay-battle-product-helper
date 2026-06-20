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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_maintenance
import org.jetbrains.compose.resources.painterResource

private val textColor = Color(0xFF2E3133)
private val brandBlue = Color(0xFF0A8CF0)
private val borderColor = Color(0xFFC2C2C2)

/**
 * CMP port of ragdoll-cat's `UnderMaintenanceWindow` (service path). Faithful to
 * `window_under_maintenance.xml`: 413dp card, maintenance illustration, title,
 * description, blue "Got it" button, close icon.
 */
@Composable
fun UnderMaintenanceScreen(
    title: String = "ClassSwift is Under Maintenance",
    description: String =
        "We’ll be back on Sep. 22, 2025 (Tue) 17:00.\n" +
            "During maintenance, the ClassSwift application, ClassSwift Hub and account registration will be unavailable.\n" +
            "Thank you for your patience!",
    onGotIt: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    Box(
        Modifier.width(413.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.33.dp, borderColor, RoundedCornerShape(8.dp))
            .designNode("under_maintenance"),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 26.66.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_maintenance),
                contentDescription = null,
                modifier = Modifier.padding(top = 21.33.dp).size(207.dp, 155.dp).designNode("um_image"),
            )
            Text(
                title,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.66.dp).fillMaxWidth().designNode("um_title"),
            )
            Text(
                description,
                color = textColor,
                fontSize = 13.3.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth().designNode("um_description"),
            )
            Box(
                Modifier.padding(top = 16.dp, bottom = 26.66.dp)
                    .fillMaxWidth()
                    .height(37.5.dp)
                    .clip(RoundedCornerShape(5.33.dp))
                    .background(brandBlue)
                    .clickable(onClick = onGotIt)
                    .designNode("um_got_it"),
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
                .designNode("um_close"),
        )
    }
}
