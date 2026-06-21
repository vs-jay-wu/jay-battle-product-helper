package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_arrow_clockwise_16
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_check_cross_circle
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_check_white
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_header
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_options
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_responses
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_stopwatch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** The 8 service-path quiz-start variants — label + option-chip layout. (The quizzing panel's
 *  type icon is the shared `ic_check_cross_circle`, per panel_mvb_quizzing.xml, not per-type.) */
enum class MvbQuizType(val label: String, val chips: List<String>) {
    MULTIPLE_CHOICE("Multiple Selection", listOf("A", "B", "C", "D")),
    TRUE_FALSE("True/False", listOf("T", "F")),
    SHORT_ANSWER("Short Answer", emptyList()),
    POLL("Poll", listOf("A", "B", "C", "D")),
    AUDIO("Audio", emptyList()),
    SKETCH("Sketch Response", emptyList()),
    TEXT_SHORT_ANSWER("Short Answer (Text)", emptyList()),
    TEXT_TRUE_FALSE("True/False (Text)", listOf("T", "F")),
}

/** Which sub-state the unified quizzing panel shows (`applyPanelVisibility`). RESULT = 2b-iii. */
enum class QuizPanelState { QUIZZING, DISCLOSE, RESULT }

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

/** Radio indicator — `bg_disclose_radio_*`: 21.33dp circle, unchecked white+black ring,
 *  checked #4848F0 fill with a 9.33dp white center dot. */
@Composable
private fun RadioIndicator(checked: Boolean) {
    if (checked) {
        Box(Modifier.size(21.33.dp).clip(CircleShape).background(Violet4848F0), contentAlignment = Alignment.Center) {
            Box(Modifier.size(9.33.dp).clip(CircleShape).background(Color.White))
        }
    } else {
        Box(Modifier.size(21.33.dp).clip(CircleShape).background(Color.White).border(0.66.dp, Color.Black, CircleShape))
    }
}

/** A disclose answer option — `widget_disclose_answer_option_item`: big centered label with a
 *  radio in the top-end corner; selected swaps to #EDEDFD card + #4848F0 border. */
@Composable
private fun DiscloseOptionItem(label: String, checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.66.dp)
    Box(
        modifier.fillMaxHeight().clip(shape)
            .background(if (checked) Violet100EDEDFD else Neutral100)
            .border(1.33.dp, if (checked) Violet4848F0 else Neutral300, shape)
            .clickable(onClick = onClick).designNode("qs_disclose_$label"),
    ) {
        Text(label, color = Neutral900, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) { RadioIndicator(checked) }
    }
}

/** Disclose selector — `ll_disclose_selector_area`: titled white card over a row of single-select
 *  answer options (mirrors `CSDiscloseAnswerOptionGroup`, SINGLE mode). */
@Composable
private fun DiscloseSelectorArea(options: List<String>, selected: Int?, onSelect: (Int) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).border(0.66.dp, Neutral300, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.66.dp).padding(top = 10.66.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.ic_check_white), null, Modifier.size(10.66.dp), colorFilter = ColorFilter.tint(Neutral900))
            Text("Select the correct answer", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.66.dp))
        }
        Row(Modifier.padding(top = 10.66.dp).fillMaxWidth().height(67.33.dp), horizontalArrangement = Arrangement.spacedBy(10.66.dp)) {
            options.forEachIndexed { i, label ->
                DiscloseOptionItem(label, checked = selected == i, onClick = { onSelect(i) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** A student's answering status during a live quiz (mirrors `AnsweringState`). */
enum class ResponderState { ANSWERED, NOT_SUBMITTED, ABSENT }

/** One student in the quizzing responses grid; [answer] shown only when answers are disclosed. */
data class QuizResponder(val seat: String, val name: String, val state: ResponderState, val answer: String? = null)

internal val sampleResponders: List<QuizResponder> = List(18) { i ->
    val name = sampleStudentNames[i % sampleStudentNames.size]
    val state = when {
        i == 4 -> ResponderState.ABSENT
        i % 6 == 5 -> ResponderState.NOT_SUBMITTED
        else -> ResponderState.ANSWERED
    }
    QuizResponder(seat = "%02d".format(i + 1), name = name, state = state)
}

/** One answering cell — `item_mvb_quiz_answering.xml`: number+name header over a state body
 *  (Submitted / Not submitted / Absent), colored per [QuizResponder.state]. */
@Composable
private fun AnsweringCell(r: QuizResponder, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val cardBg: Color
    val headerBg: Color
    val headerText: Color
    val bodyText: String
    val bodyColor: Color
    var border: Pair<Color, Boolean> = Color.Transparent to false
    when (r.state) {
        ResponderState.ANSWERED -> {
            cardBg = Violet100EDEDFD; headerBg = Violet4848F0; headerText = Color.White
            bodyText = r.answer ?: "Submitted"; bodyColor = Neutral900
        }
        ResponderState.NOT_SUBMITTED -> {
            cardBg = Color.White; headerBg = Color.White; headerText = Neutral900
            bodyText = "Not submitted"; bodyColor = Gray999; border = Neutral300 to true
        }
        ResponderState.ABSENT -> {
            cardBg = Neutral200; headerBg = Neutral200; headerText = Neutral500
            bodyText = "Absent"; bodyColor = Neutral500; border = Neutral300 to true
        }
    }
    Column(
        modifier.height(66.67.dp).clip(shape).background(cardBg)
            .then(if (border.second) Modifier.border(0.66.dp, border.first, shape) else Modifier)
            .designNode("qs_responder_${r.seat}"),
    ) {
        Text(
            "${r.seat}  ${r.name}", color = headerText, fontSize = 8.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().height(23.33.dp).background(headerBg).padding(horizontal = 4.dp).wrapContentHeight(Alignment.CenterVertically),
        )
        Box(Modifier.fillMaxSize().padding(horizontal = 10.66.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            Text(bodyText, color = bodyColor, fontSize = 9.33.sp, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * CMP port of the 8 `Mvb*StartWindow` quiz-start variants (service path), shared 853×480 shell +
 * `panel_mvb_quizzing`. [state] toggles the unified panel: QUIZZING (option chips + stopwatch +
 * End-and-review) / DISCLOSE (answer selector + Show-result). RESULT mode is 2b-iii.
 */
@Composable
fun MvbQuizStartScreen(
    type: MvbQuizType = MvbQuizType.MULTIPLE_CHOICE,
    state: QuizPanelState = QuizPanelState.QUIZZING,
    joined: Int = 21,
    capacity: Int = 30,
    responders: List<QuizResponder> = sampleResponders,
    onClose: () -> Unit = {},
) {
    var discloseSelected by remember { mutableStateOf<Int?>(null) }
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
                    Image(painterResource(Res.drawable.ic_check_cross_circle), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900))
                    Text(type.label, color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.66.dp).designNode("qs_type"))
                    Spacer(Modifier.weight(1f))
                    if (state == QuizPanelState.QUIZZING) {
                        Image(painterResource(Res.drawable.ic_mvb_quizzing_stopwatch), null, Modifier.size(16.dp))
                        Text("00:00", color = Neutral500, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 2.66.dp).designNode("qs_stopwatch"))
                    }
                }
                // Screenshot preview (empty capture frame)
                Box(
                    Modifier.padding(top = 10.66.dp).fillMaxWidth().height(169.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Color.White).border(0.66.dp, Neutral300, RoundedCornerShape(8.dp))
                        .designNode("qs_screenshot"),
                )
                // Options (QUIZZING) / answer selector (DISCLOSE)
                if (state == QuizPanelState.DISCLOSE) {
                    Box(Modifier.padding(top = 10.66.dp)) {
                        DiscloseSelectorArea(type.chips, discloseSelected) { discloseSelected = it }
                    }
                } else if (type.chips.isNotEmpty()) {
                    Box(Modifier.padding(top = 10.66.dp)) { SectionLabel(Res.drawable.ic_mvb_quizzing_options, "Options") }
                    Row(Modifier.padding(top = 10.66.dp).fillMaxWidth().height(67.33.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        type.chips.forEach { OptionChip(it, Modifier.weight(1f).designNode("qs_chip_$it")) }
                    }
                }
                Spacer(Modifier.weight(1f))
                // Bottom action: End-and-review (QUIZZING) / Show-result (DISCLOSE)
                if (state == QuizPanelState.DISCLOSE) {
                    val enabled = discloseSelected != null
                    Box(
                        Modifier.padding(top = 10.66.dp).fillMaxWidth().height(37.33.dp)
                            .clip(RoundedCornerShape(5.33.dp)).background(if (enabled) Violet4848F0 else Neutral200)
                            .designNode("qs_disclose_publish"),
                        contentAlignment = Alignment.Center,
                    ) { Text("Show question(s) result", color = if (enabled) Color.White else Neutral500, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                } else {
                    Box(
                        Modifier.padding(top = 10.66.dp).fillMaxWidth().height(37.33.dp)
                            .clip(RoundedCornerShape(5.33.dp)).background(Color.White).border(0.66.dp, RedDB0025, RoundedCornerShape(5.33.dp))
                            .designNode("qs_end_review"),
                        contentAlignment = Alignment.Center,
                    ) { Text("End and review question", color = RedDB0025, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                }
            }
            // Right: student responses
            Column(Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp).padding(vertical = 16.dp)) {
                Column(Modifier.fillMaxSize().clip(RoundedCornerShape(10.66.dp)).background(Neutral100).padding(10.66.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(Res.drawable.ic_mvb_quizzing_responses, "Responses")
                        if (state == QuizPanelState.QUIZZING) {
                            Row(
                                Modifier.padding(start = 10.66.dp).height(21.33.dp).clip(RoundedCornerShape(5.33.dp)).border(0.66.dp, Neutral300, RoundedCornerShape(5.33.dp)).padding(horizontal = 8.dp).designNode("qs_refresh"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(painterResource(Res.drawable.ic_arrow_clockwise_16), null, Modifier.size(10.66.dp), colorFilter = ColorFilter.tint(Neutral900))
                                Text("Refresh", color = Neutral900, fontSize = 10.sp, modifier = Modifier.padding(start = 2.66.dp))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("$joined/$capacity", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("qs_count"))
                    }
                    Column(
                        Modifier.padding(top = 10.66.dp).fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.66.dp),
                    ) {
                        responders.chunked(4).forEach { rowItems ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.66.dp)) {
                                rowItems.forEach { r -> AnsweringCell(r, Modifier.weight(1f)) }
                                repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}