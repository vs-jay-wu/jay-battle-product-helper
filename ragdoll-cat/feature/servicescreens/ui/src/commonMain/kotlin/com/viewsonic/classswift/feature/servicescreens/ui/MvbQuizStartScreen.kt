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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_arrow_clockwise_16
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_qc_chip_audio
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_qc_chip_multiple_choice
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_qc_chip_poll
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_qc_chip_short_answer
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_qc_chip_true_false
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_header
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_options
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_responses
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_stopwatch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** The 8 service-path quiz-start variants — type icon, label, and option-chip layout. */
enum class MvbQuizType(val icon: DrawableResource, val label: String, val chips: List<String>) {
    MULTIPLE_CHOICE(Res.drawable.ic_mvb_qc_chip_multiple_choice, "Multiple Selection", listOf("A", "B", "C", "D")),
    TRUE_FALSE(Res.drawable.ic_mvb_qc_chip_true_false, "True/False", listOf("True", "False")),
    SHORT_ANSWER(Res.drawable.ic_mvb_qc_chip_short_answer, "Short Answer", emptyList()),
    POLL(Res.drawable.ic_mvb_qc_chip_poll, "Poll", listOf("A", "B", "C", "D")),
    AUDIO(Res.drawable.ic_mvb_qc_chip_audio, "Audio", emptyList()),
    SKETCH(Res.drawable.ic_mvb_qc_chip_short_answer, "Sketch Response", emptyList()),
    TEXT_SHORT_ANSWER(Res.drawable.ic_mvb_qc_chip_short_answer, "Short Answer (Text)", emptyList()),
    TEXT_TRUE_FALSE(Res.drawable.ic_mvb_qc_chip_true_false, "True/False (Text)", listOf("True", "False")),
}

@Composable
private fun SectionLabel(icon: DrawableResource, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900))
        Text(text, color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.66.dp))
    }
}

@Composable
private fun OptionChip(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxHeight().clip(RoundedCornerShape(5.33.dp)).background(Neutral100).border(1.dp, Neutral300, RoundedCornerShape(5.33.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Neutral900, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun ResponseCell(name: String, index: Int, modifier: Modifier = Modifier) {
    Column(modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE5E5E5)), contentAlignment = Alignment.Center) {
            Text("${index + 1}", color = CloseGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(name, color = Dark2E3133, fontSize = 9.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * CMP port of the 8 `Mvb*StartWindow` quiz-start variants (service path), quizzing state:
 * shared 853×480 shell + `panel_mvb_quizzing` — question section (type, stopwatch, screenshot,
 * option chips, End-and-review) and student responses. (Disclose & result modes are deferred.)
 */
@Composable
fun MvbQuizStartScreen(
    type: MvbQuizType = MvbQuizType.MULTIPLE_CHOICE,
    joined: Int = 21,
    capacity: Int = 30,
    responders: List<String> = sampleStudents.take(21).map { it.name },
    onClose: () -> Unit = {},
) {
    Column(
        Modifier.size(853.dp, 480.dp).clip(RoundedCornerShape(10.66.dp)).background(Color.White).designNode("mvb_quiz_start"),
    ) {
        // ---- Header ----
        Row(Modifier.fillMaxWidth().height(32.dp).background(Color.White).padding(horizontal = 10.66.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.ic_mvb_quizzing_header), null, Modifier.size(21.33.dp))
            Text("Question", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.33.dp).weight(1f).designNode("qs_title"))
            Image(painterResource(Res.drawable.ic_close), "Close", Modifier.size(16.dp).clickable(onClick = onClose).designNode("qs_close"))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE6E6E6)))
        // ---- Panel ----
        Row(Modifier.weight(1f).fillMaxWidth().background(Color.White)) {
            // Left: question section
            Column(Modifier.width(333.33.dp).fillMaxHeight().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(type.icon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900))
                    Text(type.label, color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.66.dp).designNode("qs_type"))
                    Spacer(Modifier.weight(1f))
                    Image(painterResource(Res.drawable.ic_mvb_quizzing_stopwatch), null, Modifier.size(16.dp))
                    Text("00:00", color = Neutral500, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 2.66.dp).designNode("qs_stopwatch"))
                }
                // Screenshot preview (empty capture frame)
                Box(
                    Modifier.padding(top = 10.66.dp).fillMaxWidth().height(169.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Color.White).border(0.66.dp, Neutral300, RoundedCornerShape(8.dp))
                        .designNode("qs_screenshot"),
                )
                // Options
                if (type.chips.isNotEmpty()) {
                    Box(Modifier.padding(top = 10.66.dp)) { SectionLabel(Res.drawable.ic_mvb_quizzing_options, "Options") }
                    Row(Modifier.padding(top = 10.66.dp).fillMaxWidth().height(67.33.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        type.chips.forEach { OptionChip(it, Modifier.weight(1f).designNode("qs_chip_$it")) }
                    }
                }
                Spacer(Modifier.weight(1f))
                // End and review
                Box(
                    Modifier.padding(top = 10.66.dp).fillMaxWidth().height(37.33.dp)
                        .clip(RoundedCornerShape(5.33.dp)).background(Color.White).border(0.66.dp, RedDB0025, RoundedCornerShape(5.33.dp))
                        .designNode("qs_end_review"),
                    contentAlignment = Alignment.Center,
                ) { Text("End and review question", color = RedDB0025, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
            }
            // Right: student responses
            Column(Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp).padding(vertical = 16.dp)) {
                Column(Modifier.fillMaxSize().clip(RoundedCornerShape(10.66.dp)).background(Neutral100).padding(10.66.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(Res.drawable.ic_mvb_quizzing_responses, "Responses")
                        Row(
                            Modifier.padding(start = 10.66.dp).height(21.33.dp).clip(RoundedCornerShape(5.33.dp)).border(0.66.dp, Neutral300, RoundedCornerShape(5.33.dp)).padding(horizontal = 8.dp).designNode("qs_refresh"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(painterResource(Res.drawable.ic_arrow_clockwise_16), null, Modifier.size(10.66.dp), colorFilter = ColorFilter.tint(Neutral900))
                            Text("Refresh", color = Neutral900, fontSize = 10.sp, modifier = Modifier.padding(start = 2.66.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Text("$joined/$capacity", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("qs_count"))
                    }
                    Column(Modifier.padding(top = 10.66.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        responders.chunked(7).forEachIndexed { rowIdx, rowNames ->
                            Row(Modifier.fillMaxWidth()) {
                                rowNames.forEachIndexed { i, n -> ResponseCell(n, rowIdx * 7 + i, Modifier.weight(1f)) }
                                repeat(7 - rowNames.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}