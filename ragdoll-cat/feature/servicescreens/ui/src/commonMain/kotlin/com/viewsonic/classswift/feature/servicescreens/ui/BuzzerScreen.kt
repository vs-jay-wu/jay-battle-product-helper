package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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

/** CMP port of `BuzzerWindow` (service path): title, elapsed time, big circular Start button. */
@Composable
fun BuzzerScreen(time: String = "00:00", onClose: () -> Unit = {}) {
    ToolCard("buzzer", onClose = onClose) {
        Column(
            Modifier.fillMaxWidth().padding(top = 31.66.dp).padding(horizontal = 10.66.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Buzzer", color = Neutral900, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("buzzer_title"))
            Text(
                time,
                color = Neutral900,
                fontSize = 40.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 13.33.dp).fillMaxWidth().height(66.66.dp).designNode("buzzer_time"),
            )
            Box(
                Modifier.size(106.66.dp).clip(CircleShape).background(BrandBlue).designNode("buzzer_start"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Start", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
