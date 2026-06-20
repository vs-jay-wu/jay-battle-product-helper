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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource

/** Per-question tally: correct / incorrect / no-answer counts. */
private data class QResult(val correct: Int, val incorrect: Int, val noAnswer: Int)

private val sampleResults = listOf(
    QResult(24, 4, 2), QResult(18, 9, 3), QResult(27, 2, 1), QResult(15, 12, 3),
    QResult(21, 6, 3), QResult(12, 14, 4), QResult(25, 3, 2), QResult(19, 8, 3),
)

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
        Box(Modifier.size(16.dp).clip(RoundedCornerShape(1.33.dp)).background(color))
        Text(label, color = Dark2E3133, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

/** CMP port of `BatchQuizResultWindow` (service path): per-question stacked accuracy bars + legend. */
@Composable
fun BatchQuizResultScreen(results: List<Triple<Int, Int, Int>> = sampleResults.map { Triple(it.correct, it.incorrect, it.noAnswer) }, onClose: () -> Unit = {}) {
    val rs = results.map { QResult(it.first, it.second, it.third) }
    Box(
        Modifier.size(933.33.dp, 569.33.dp).clip(RoundedCornerShape(10.66.dp)).background(Neutral100).border(0.66.dp, Neutral400, RoundedCornerShape(10.66.dp)).designNode("batch_quiz_result"),
    ) {
        Image(
            painterResource(Res.drawable.ic_close), "Close",
            Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp).size(21.3.dp).clickable(onClick = onClose).designNode("bqr_close"),
            colorFilter = ColorFilter.tint(Neutral900),
        )
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Quiz Builder", color = Neutral900, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("bqr_title"))
                Spacer(Modifier.weight(1f))
                LegendDot(Green500, "Correct")
                LegendDot(Red400, "Incorrect")
                LegendDot(BorderC2C2C2, "No Answer")
            }
            // Bars: one column per question, stacked correct/incorrect/no-answer.
            Row(Modifier.padding(top = 24.dp).weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                rs.forEachIndexed { i, r ->
                    val total = (r.correct + r.incorrect + r.noAnswer).coerceAtLeast(1).toFloat()
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text("${(r.correct / total * 100).toInt()}%", color = Dark2E3133, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Column(Modifier.padding(top = 4.dp).width(40.dp).weight(1f).clip(RoundedCornerShape(4.dp)).designNode("bqr_bar_${i + 1}")) {
                            if (r.noAnswer > 0) Box(Modifier.fillMaxWidth().weight(r.noAnswer / total).background(BorderC2C2C2))
                            if (r.incorrect > 0) Box(Modifier.fillMaxWidth().weight(r.incorrect / total).background(Red400))
                            if (r.correct > 0) Box(Modifier.fillMaxWidth().weight(r.correct / total).background(Green500))
                        }
                        Text("Q${i + 1}", color = Neutral500, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
