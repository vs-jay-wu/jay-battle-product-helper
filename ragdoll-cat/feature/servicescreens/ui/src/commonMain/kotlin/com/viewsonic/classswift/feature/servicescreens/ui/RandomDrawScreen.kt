package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_dice
import org.jetbrains.compose.resources.painterResource

/** CMP port of `RandomDrawWindow` (service path): title + tappable blue dice circle. */
@Composable
fun RandomDrawScreen(onClose: () -> Unit = {}) {
    ToolCard("random_draw", onClose = onClose) {
        Text(
            "Random Draw",
            color = Neutral900,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 34.66.dp, start = 10.66.dp).designNode("rd_title"),
        )
        Box(
            Modifier.align(Alignment.Center).size(125.3.dp).clip(CircleShape).background(BrandBlue).designNode("rd_dice"),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(Res.drawable.ic_dice),
                contentDescription = "Draw",
                modifier = Modifier.size(106.66.dp),
                colorFilter = ColorFilter.tint(Color.White),
            )
        }
    }
}
