package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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

private val Cyan400 = Color(0xFF3AC9CC)

/** CMP port of `InAppTutorialWindow` (service path): full-screen onboarding carousel — a guide
 * page over a video frame, page-dot indicator, and Skip / Next actions. */
@Composable
fun InAppTutorialScreen(page: Int = 1, pageCount: Int = 5) {
    Box(Modifier.fillMaxSize().background(Color.White).designNode("in_app_tutorial")) {
        Column(Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Welcome to ClassSwift", color = Violet700, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("tut_title"))
            Text(
                "Take a quick tour of the tools that help you run interactive lessons.",
                color = Neutral500, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
            )
            // Video guide frame (real bordered media area; empty until a clip loads)
            Box(
                Modifier.padding(top = 24.dp).fillMaxWidth().weight(1f)
                    .clip(RoundedCornerShape(12.dp)).background(Neutral100).border(1.dp, Neutral300, RoundedCornerShape(12.dp)).designNode("tut_video"),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Violet700), contentAlignment = Alignment.Center) {
                    Text("▶", color = Color.White, fontSize = 28.sp)
                }
            }
        }
        // Bottom action bar
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 21.33.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.width(140.dp).height(48.dp).clip(RoundedCornerShape(5.33.dp)).border(1.dp, Violet700, RoundedCornerShape(5.33.dp)).designNode("tut_skip"),
                contentAlignment = Alignment.Center,
            ) { Text("Skip", color = Violet700, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(pageCount) { i ->
                    Box(Modifier.size(if (i == page - 1) 10.dp else 8.dp).clip(CircleShape).background(if (i == page - 1) Cyan400 else Neutral500))
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.width(140.dp).height(48.dp).clip(RoundedCornerShape(5.33.dp)).background(Violet700).designNode("tut_next"),
                contentAlignment = Alignment.Center,
            ) { Text(if (page >= pageCount) "I'm Ready!" else "Next", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
        }
    }
}
