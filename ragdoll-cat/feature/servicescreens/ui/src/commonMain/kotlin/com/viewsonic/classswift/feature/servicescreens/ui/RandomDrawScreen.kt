package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_dice
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_upload_again
import org.jetbrains.compose.resources.painterResource

enum class RandomDrawPhase { INIT, NO_STUDENTS, ROLLING, RESULT }

/** CMP port of `RandomDrawWindow` (service path): tappable blue dice that draws a random student;
 * shows the picked student + Try again, or a "no participants" hint. (Dice-roll Lottie deferred.) */
@Composable
fun RandomDrawScreen(
    phase: RandomDrawPhase = RandomDrawPhase.INIT,
    seat: String = "",
    name: String = "",
    onDraw: () -> Unit = {},
    onTryAgain: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    ToolCard("random_draw", onClose = onClose) {
        Text(
            "Random Draw", color = Neutral900, fontSize = 32.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 34.66.dp, start = 10.66.dp, end = 10.66.dp).designNode("rd_title"),
        )
        when (phase) {
            RandomDrawPhase.RESULT -> Row(
                Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 26.66.dp).designNode("rd_participant"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(82.66.dp).clip(CircleShape).background(BrandBlue), contentAlignment = Alignment.Center) {
                    Text(seat, color = Color.White, fontSize = 29.33.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(name, color = Color.Black, fontSize = 21.33.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.padding(start = 16.dp))
            }
            else -> Box(
                Modifier.align(Alignment.Center).size(125.3.dp).clip(CircleShape).background(BrandBlue)
                    .then(if (phase == RandomDrawPhase.INIT) Modifier.clickable(onClick = onDraw) else Modifier)
                    .designNode("rd_dice"),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(Res.drawable.ic_dice), "Draw", Modifier.size(106.66.dp), colorFilter = ColorFilter.tint(Color.White))
            }
        }
        when (phase) {
            RandomDrawPhase.NO_STUDENTS -> Text(
                "No Current Participants", color = BorderC2C2C2, fontSize = 18.66.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 31.66.dp, start = 10.66.dp, end = 10.66.dp).designNode("rd_no_students"),
            )
            RandomDrawPhase.RESULT -> Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).height(32.dp).clickable(onClick = onTryAgain).designNode("rd_try_again"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(Res.drawable.ic_upload_again), null, Modifier.size(32.dp), colorFilter = ColorFilter.tint(BrandBlue))
                Text("Try again", color = BrandBlue, fontSize = 18.66.sp, fontWeight = FontWeight.Bold)
            }
            else -> Unit
        }
    }
}
