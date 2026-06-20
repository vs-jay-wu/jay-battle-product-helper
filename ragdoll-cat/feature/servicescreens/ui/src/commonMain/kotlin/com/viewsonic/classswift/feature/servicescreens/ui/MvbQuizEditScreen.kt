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
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_add
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_cross
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_minus_32dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_quiz_v2
import org.jetbrains.compose.resources.painterResource

/** How the answer area of the quiz editor is configured per quiz type. */
private data class EditConfig(val boxes: List<String>, val canAddOption: Boolean, val answerType: String, val answerOptions: String)

private fun configFor(type: MvbQuizType): EditConfig = when (type) {
    MvbQuizType.MULTIPLE_CHOICE -> EditConfig(listOf("A", "B", "C", "D"), true, "Single answer", "4")
    MvbQuizType.POLL -> EditConfig(listOf("A", "B", "C", "D"), true, "Poll", "4")
    MvbQuizType.TRUE_FALSE, MvbQuizType.TEXT_TRUE_FALSE -> EditConfig(listOf("T", "F"), false, "True / False", "2")
    MvbQuizType.SHORT_ANSWER, MvbQuizType.TEXT_SHORT_ANSWER -> EditConfig(emptyList(), false, "Short answer", "—")
    MvbQuizType.AUDIO -> EditConfig(emptyList(), false, "Audio recording", "—")
    MvbQuizType.SKETCH -> EditConfig(emptyList(), false, "Sketch response", "—")
}

@Composable
private fun OptionBox(label: String, correct: Boolean) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(5.33.dp))
            .background(if (correct) BrandBlue else Neutral100)
            .border(1.dp, if (correct) BrandBlue else Neutral300, RoundedCornerShape(5.33.dp))
            .designNode("edit_opt_$label"),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (correct) Color.White else Neutral900, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

/** A labelled settings dropdown row (Answer types / Answer options). */
@Composable
private fun SettingDropdown(label: String, value: String, nodeId: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Neutral500, fontSize = 9.33.sp)
        Row(
            Modifier.padding(top = 4.dp).fillMaxWidth().height(37.33.dp)
                .clip(RoundedCornerShape(5.33.dp)).background(Color.White).border(1.dp, Neutral300, RoundedCornerShape(5.33.dp))
                .padding(horizontal = 10.dp).designNode(nodeId),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, color = Neutral900, fontSize = 10.67.sp, modifier = Modifier.weight(1f))
            Text("▾", color = Neutral500, fontSize = 10.sp)
        }
    }
}

/**
 * CMP port of the 6 `Mvb*EditWindow` quiz-editor variants (service path): 541×626 card, header,
 * question-image upload frame, option panel (answer boxes + add, answer-type/options settings),
 * Cancel / Start-question action bar. Differs per type via [configFor].
 */
@Composable
fun MvbQuizEditScreen(type: MvbQuizType = MvbQuizType.MULTIPLE_CHOICE, onClose: () -> Unit = {}) {
    val cfg = configFor(type)
    Box(Modifier.size(541.33.dp, 626.dp).padding(8.dp)) {
        Column(
            Modifier.fillMaxSize().clip(RoundedCornerShape(5.33.dp)).background(Color.White).border(0.66.dp, Neutral300, RoundedCornerShape(5.33.dp)).designNode("mvb_quiz_edit"),
        ) {
            // Header
            Row(Modifier.fillMaxWidth().padding(start = 10.66.dp, end = 10.66.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(Res.drawable.ic_quiz_v2), null, Modifier.size(21.33.dp))
                Text("Question", color = Neutral900, fontSize = 10.66.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.33.dp).weight(1f).designNode("edit_title"))
                Image(painterResource(Res.drawable.ic_minus_32dp), "Minimize", Modifier.size(24.dp).padding(4.dp))
                Image(painterResource(Res.drawable.ic_cross), "Close", Modifier.size(24.dp).padding(4.dp).clickable(onClick = onClose).designNode("edit_close"))
            }
            Box(Modifier.fillMaxWidth().height(0.66.dp).background(Color(0xFFE6E6E6)))
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                // Question image upload frame
                Box(
                    Modifier.fillMaxWidth().height(277.33.dp).clip(RoundedCornerShape(5.33.dp)).background(Color.White).border(0.66.dp, Neutral300, RoundedCornerShape(5.33.dp)).designNode("edit_image"),
                    contentAlignment = Alignment.Center,
                ) { Text("Tap to add a question image", color = Neutral500, fontSize = 11.sp) }

                // Answer option panel
                if (cfg.boxes.isNotEmpty()) {
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        cfg.boxes.forEachIndexed { i, b -> OptionBox(b, correct = i == 0 && type != MvbQuizType.POLL) }
                        if (cfg.canAddOption) {
                            Box(
                                Modifier.size(24.dp).clip(RoundedCornerShape(5.33.dp)).background(Neutral100).clickable {}.designNode("edit_add_option"),
                                contentAlignment = Alignment.Center,
                            ) { Image(painterResource(Res.drawable.ic_add), "Add option", Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900)) }
                        }
                    }
                }
                Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(Neutral100).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.66.dp)) {
                    SettingDropdown("Answer types", cfg.answerType, "edit_answer_type", Modifier.weight(1f))
                    SettingDropdown("Answer options", cfg.answerOptions, "edit_answer_options", Modifier.weight(1f))
                }

                Spacer(Modifier.weight(1f))
                // Action bar
                Row(Modifier.fillMaxWidth().height(37.33.dp), horizontalArrangement = Arrangement.spacedBy(10.66.dp)) {
                    CSButton("Cancel question", backgroundColor = Color.White, textColor = Neutral900, borderColor = Neutral300, textSize = 12.sp, nodeId = "edit_cancel", modifier = Modifier.weight(1f).height(37.33.dp))
                    CSButton("Start question", backgroundColor = Violet4848F0, textColor = Color.White, textSize = 12.sp, nodeId = "edit_start", modifier = Modifier.weight(1f).height(37.33.dp))
                }
            }
        }
    }
}
