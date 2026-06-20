package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_logo_with_transparent_background
import org.jetbrains.compose.resources.painterResource

/**
 * CMP port of `UpcomingMaintenanceCornerPromptWindow` — the 360dp toast that docks to the
 * screen's right edge (left corners rounded 12dp, right edge flat). Faithful to
 * `window_upcoming_maintenance_corner_prompt.xml`.
 */
@Composable
fun UpcomingMaintenanceCornerPrompt(
    title: String = "ClassSwift Downtime Notice",
    description: String =
        "ClassSwift will be down for scheduled maintenance in 5 minutes. During maintenance, " +
            "ClassSwift APP and Hub will not be accessible.\n" +
            "The system will automatically save your class records before the downtime begins.",
    onGotIt: () -> Unit = {},
) {
    val leftRounded = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
    Column(
        Modifier.width(360.dp)
            .clip(leftRounded)
            .background(Color.White)
            .border(0.95.dp, CornerBorderD6D6D6, leftRounded)
            .padding(start = 21.33.dp, end = 16.dp)
            .padding(vertical = 16.dp)
            .designNode("upcoming_maintenance_corner"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(Res.drawable.ic_logo_with_transparent_background),
                contentDescription = null,
                modifier = Modifier.size(26.7.dp).designNode("corner_logo"),
            )
            Text(
                title,
                color = Color.Black,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp).designNode("corner_title"),
            )
        }
        Text(
            description,
            color = Dark2E3133,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.66.dp).designNode("corner_description"),
        )
        Box(
            Modifier.padding(top = 10.66.dp)
                .width(54.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(5.33.dp))
                .border(1.dp, BrandBlue, RoundedCornerShape(5.33.dp))
                .clickable(onClick = onGotIt)
                .designNode("corner_got_it"),
            contentAlignment = Alignment.Center,
        ) {
            Text("Got it", color = BrandBlue, fontSize = 12.sp)
        }
    }
}
