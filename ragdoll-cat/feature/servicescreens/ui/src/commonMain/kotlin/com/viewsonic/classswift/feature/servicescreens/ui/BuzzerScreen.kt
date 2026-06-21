package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_upload_again
import org.jetbrains.compose.resources.painterResource

enum class BuzzerPhase { INIT, RUNNING, ANSWERED }

/** CMP port of `BuzzerWindow` (service path): title, elapsed time, and a Start circle that becomes
 * a Try-again + the winning participant once a student buzzes in. */
@Composable
fun BuzzerScreen(
    phase: BuzzerPhase = BuzzerPhase.INIT,
    time: String = "00:00",
    seat: String = "",
    name: String = "",
    onStart: () -> Unit = {},
    onTryAgain: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    ToolCard("buzzer", onClose = onClose) {
        Column(
            Modifier.fillMaxWidth().padding(top = 31.66.dp).padding(horizontal = 10.66.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Buzzer", color = Neutral900, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("buzzer_title"))
            Text(
                time, color = Neutral900, fontSize = 40.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 13.33.dp).fillMaxWidth().height(66.66.dp).designNode("buzzer_time"),
            )
            when (phase) {
                BuzzerPhase.INIT -> Box(
                    Modifier.size(106.66.dp).clip(CircleShape).background(BrandBlue).clickable(onClick = onStart).designNode("buzzer_start"),
                    contentAlignment = Alignment.Center,
                ) { Text("Start", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.SemiBold) }
                BuzzerPhase.ANSWERED -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.padding(vertical = 6.66.dp, horizontal = 0.dp).padding(start = 16.dp).size(82.66.dp).clip(CircleShape).background(BrandBlue).designNode("buzzer_seat"),
                        contentAlignment = Alignment.Center,
                    ) { Text(seat, color = Color.White, fontSize = 29.33.sp, fontWeight = FontWeight.SemiBold) }
                    Text(name, color = Color.Black, fontSize = 21.33.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.padding(horizontal = 16.dp).designNode("buzzer_name"))
                }
                BuzzerPhase.RUNNING -> Unit
            }
        }
        if (phase != BuzzerPhase.INIT) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).height(32.dp).clickable(onClick = onTryAgain).designNode("buzzer_try_again"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(Res.drawable.ic_upload_again), null, Modifier.size(32.dp), colorFilter = ColorFilter.tint(BrandBlue))
                Text("Try again", color = BrandBlue, fontSize = 18.66.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
