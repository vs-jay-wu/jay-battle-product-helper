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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_minus_32dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_bring_to_front_64dp
import org.jetbrains.compose.resources.painterResource

private data class Ranked(val name: String, val score: Int)

private val sampleRanking = listOf(
    "Emily Chen" to 980, "Sophia Liu" to 940, "Marcus Lee" to 910, "Olivia Yang" to 870,
    "Daniel Wu" to 850, "Mia Hsu" to 820, "Ethan Kao" to 790, "Lucas Lin" to 760,
    "Chloe Tsai" to 740, "Brandon Wang" to 700,
).map { Ranked(it.first, it.second) }

private val podiumColors = listOf(Color(0xFFF4BA00), Color(0xFFB8BCC0), Color(0xFFCD7F32))

@Composable
private fun Podium(rank: Int, r: Ranked, barHeight: androidx.compose.ui.unit.Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.width(120.dp)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(podiumColors[rank - 1]), contentAlignment = Alignment.Center) {
            Text(r.name.first().toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(r.name, color = Dark2E3133, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        Text("${r.score}", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier.padding(top = 6.dp).width(80.dp).height(barHeight)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(podiumColors[rank - 1]),
            contentAlignment = Alignment.TopCenter,
        ) { Text("$rank", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
    }
}

/** CMP port of `LeaderboardWindow` (service path). The live app shows a web leaderboard; here we
 * render its real content — a top-3 podium plus a ranked score list — in the window chrome. */
@Composable
fun LeaderboardScreen(ranking: List<Pair<String, Int>> = sampleRanking.map { it.name to it.score }, onClose: () -> Unit = {}) {
    val ranked = ranking.map { Ranked(it.first, it.second) }
    Box(
        Modifier.size(854.dp, 520.dp).clip(RoundedCornerShape(10.66.dp)).background(Color.White).border(0.66.dp, BorderC2C2C2, RoundedCornerShape(10.66.dp)).designNode("leaderboard"),
    ) {
        Row(Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(Res.drawable.ic_minus_32dp), "Minimize", Modifier.size(21.3.dp), colorFilter = ColorFilter.tint(Neutral900))
            Image(painterResource(Res.drawable.ic_toolbar_bring_to_front_64dp), "Front", Modifier.size(21.3.dp), colorFilter = ColorFilter.tint(Neutral900))
            Image(painterResource(Res.drawable.ic_close), "Close", Modifier.size(21.3.dp).clickable(onClick = onClose).designNode("lb_close"), colorFilter = ColorFilter.tint(Neutral900))
        }
        Column(Modifier.fillMaxSize().padding(40.dp)) {
            Text("Leaderboard", color = Dark2E3133, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("lb_title"))
            // Podium (2nd, 1st, 3rd)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                if (ranked.size > 1) Podium(2, ranked[1], 70.dp)
                if (ranked.isNotEmpty()) Podium(1, ranked[0], 100.dp)
                if (ranked.size > 2) Podium(3, ranked[2], 50.dp)
            }
            // Remaining ranked list
            LazyColumn(Modifier.padding(top = 16.dp).weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(ranked.drop(3)) { i, r ->
                    Row(
                        Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(6.dp)).background(Neutral100).padding(horizontal = 12.dp).designNode("lb_row_${i + 4}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${i + 4}", color = Neutral500, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                        Text(r.name, color = Dark2E3133, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("${r.score}", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
