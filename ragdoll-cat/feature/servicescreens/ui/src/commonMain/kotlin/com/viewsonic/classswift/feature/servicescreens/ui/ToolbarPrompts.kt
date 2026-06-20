package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_premium
import org.jetbrains.compose.resources.painterResource

/** The yellow uppercase "SOON" pill — `tv_soon` / `bg_toolbar_prompt_button` tinted `color_F4BA00`. */
@Composable
private fun SoonBadge() {
    Text(
        "SOON",
        color = Color.White,
        fontSize = 7.53.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.designNode("prompt_soon")
            .clip(RoundedCornerShape(2.52.dp))
            .background(SoonYellow)
            .padding(horizontal = 1.26.dp, vertical = 1.dp),
    )
}

/** Shared black rounded toolbar-prompt container — `bg_window_prompt` (black, radius 4.8dp). */
@Composable
private fun PromptContainer(nodeId: String, content: @Composable () -> Unit) {
    Row(
        Modifier.designNode(nodeId)
            .clip(RoundedCornerShape(4.8.dp))
            .background(Color.Black)
            .padding(horizontal = 9.6.dp, vertical = 7.2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun PromptTitle(title: String) =
    Text(title, color = Color.White, fontSize = 15.sp, modifier = Modifier.designNode("prompt_title"))

/** CMP port of `ComingSoonPromptWindow` — title + "SOON" pill. */
@Composable
fun ComingSoonPrompt(title: String = "Instant Push") {
    PromptContainer("coming_soon_prompt") {
        PromptTitle(title)
        Box(Modifier.padding(start = 6.dp)) { SoonBadge() }
    }
}

/** CMP port of `UpgradePromptWindow` — title + premium icon + "SOON" pill. */
@Composable
fun UpgradePrompt(title: String = "Instant Push") {
    PromptContainer("upgrade_prompt") {
        PromptTitle(title)
        Image(
            painter = painterResource(Res.drawable.ic_premium),
            contentDescription = null,
            modifier = Modifier.padding(start = 2.6.dp).size(16.dp).padding(2.dp).designNode("prompt_premium"),
        )
        Box(Modifier.padding(start = 6.dp)) { SoonBadge() }
    }
}
