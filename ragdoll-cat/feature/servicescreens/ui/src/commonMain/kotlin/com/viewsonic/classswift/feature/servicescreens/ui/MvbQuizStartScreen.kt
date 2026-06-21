package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_arrow_clockwise_16
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_audio_pause_outline
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_audio_play_outline
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_check_cross_circle
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_check_white
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_checkmark
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_cross
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_sparkles
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_minus_32dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_previous_arrow
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_header
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_options
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_responses
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_stopwatch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

/** The 8 service-path quiz-start variants — label + option-chip layout. (The quizzing panel's
 *  type icon is the shared `ic_check_cross_circle`, per panel_mvb_quizzing.xml, not per-type.) */
enum class MvbQuizType(val label: String, val chips: List<String>) {
    MULTIPLE_CHOICE("Multiple choice", listOf("A", "B", "C", "D")),
    TRUE_FALSE("True or false", listOf("T", "F")),
    SHORT_ANSWER("Short answer", emptyList()),
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

/** Text-variant quizzing option — `bg_neutral100_radius800_line_neutral300_border200`: a full-width
 *  38.66dp neutral100 chip (radius 10.66, 0.66 neutral300 border) with its label left-aligned. */
@Composable
private fun VerticalOptionChip(label: String) {
    val shape = RoundedCornerShape(10.66.dp)
    Box(
        Modifier.fillMaxWidth().height(38.66.dp).clip(shape).background(Neutral100).border(0.66.dp, Neutral300, shape)
            .padding(horizontal = 10.66.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(label, color = Neutral900, fontSize = 10.67.sp, modifier = Modifier.designNode("qs_voption_$label")) }
}

/** Radio indicator — `bg_disclose_radio_*`: a circle, unchecked white+black ring, checked #4848F0
 *  fill with a white center dot. [size]/[dot] default to the disclose-square radio (21.33/9.33);
 *  the text-variant option radio (`bg_mvb_text_quiz_option_radio_*`) is 24/9.78. */
@Composable
private fun RadioIndicator(checked: Boolean, size: Dp = 21.33.dp, dot: Dp = 9.33.dp) {
    if (checked) {
        Box(Modifier.size(size).clip(CircleShape).background(Violet4848F0), contentAlignment = Alignment.Center) {
            Box(Modifier.size(dot).clip(CircleShape).background(Color.White))
        }
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(Color.White).border(0.66.dp, Color.Black, CircleShape))
    }
}

/** Checkbox indicator — `bg_disclose_checkbox_*`: 21.33dp rounded square; checked = #4848F0 fill
 *  with a white check, unchecked = white + black border. */
@Composable
private fun CheckboxIndicator(checked: Boolean) {
    val shape = RoundedCornerShape(5.33.dp)
    if (checked) {
        Box(Modifier.size(21.33.dp).clip(shape).background(Violet4848F0), contentAlignment = Alignment.Center) {
            Image(painterResource(Res.drawable.ic_check_white), null, Modifier.size(12.dp))
        }
    } else {
        Box(Modifier.size(21.33.dp).clip(shape).background(Color.White).border(0.66.dp, Color.Black, shape))
    }
}

/** A disclose answer option — `widget_disclose_answer_option_item`: big centered label with a
 *  radio (single) / checkbox (multi) in the top-end corner; selected swaps to #EDEDFD + #4848F0 border. */
@Composable
private fun DiscloseOptionItem(label: String, checked: Boolean, multi: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.66.dp)
    Box(
        modifier.fillMaxHeight().clip(shape)
            .background(if (checked) Violet100EDEDFD else Neutral100)
            .border(1.33.dp, if (checked) Violet4848F0 else Neutral300, shape)
            .clickable(onClick = onClick).designNode("qs_disclose_$label"),
    ) {
        Text(label, color = Neutral900, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) { if (multi) CheckboxIndicator(checked) else RadioIndicator(checked) }
    }
}

/** Disclose selector — `ll_disclose_selector_area`: titled white card over a row of answer options
 *  (mirrors `CSDiscloseAnswerOptionGroup`; [multi] = MULTIPLE selection mode). */
@Composable
private fun DiscloseSelectorArea(options: List<String>, selected: Set<Int>, multi: Boolean, onToggle: (Int) -> Unit) {
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
                DiscloseOptionItem(label, checked = i in selected, multi = multi, onClick = { onToggle(i) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Text-variant disclose option (`view_mvb_text_quiz_disclose_option`). [content]/[reason] are the
 *  option text (plain here; native renders LaTeX via a KatexView), [isSuggested] marks the AI/correct
 *  answer whose reason + "Suggested answer" pill appear once the reveal button is tapped. */
data class TextDiscloseOption(val content: String, val reason: String = "", val isSuggested: Boolean = false)

/** One text-disclose option row: tappable card (selected = violet50 + 1.33 violet500 border, else
 *  neutral100 + 0.66 neutral300 border) with a 24dp radio in the top-end corner, the content, and —
 *  when [revealed] and this is the suggested answer — a "Suggested answer" pill + neutral650 reason. */
@Composable
private fun TextDiscloseOptionItem(option: TextDiscloseOption, selected: Boolean, revealed: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.66.dp)
    val showSuggested = option.isSuggested && revealed
    Box(
        Modifier.fillMaxWidth().heightIn(min = 41.32.dp).clip(shape)
            .background(if (selected) Violet100EDEDFD else Neutral100)
            .border(if (selected) 1.33.dp else 0.66.dp, if (selected) Violet4848F0 else Neutral300, shape)
            .clickable(onClick = onClick).designNode("qs_text_disclose_${option.content}"),
    ) {
        Box(Modifier.align(Alignment.TopEnd).padding(top = 8.66.dp, end = 10.66.dp)) { RadioIndicator(selected, 24.dp, 9.78.dp) }
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.66.dp).padding(bottom = 10.66.dp)) {
            if (showSuggested) {
                Row(
                    Modifier.padding(top = 10.66.dp).clip(RoundedCornerShape(50)).background(Violet100DADAFC)
                        .padding(horizontal = 5.33.dp, vertical = 2.66.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(Res.drawable.ic_sparkles), null, Modifier.size(10.66.dp), colorFilter = ColorFilter.tint(Neutral900))
                    Text("Suggested answer", color = Neutral900, fontSize = 8.sp, modifier = Modifier.padding(start = 2.66.dp))
                }
            }
            Text(
                option.content, color = Neutral900, fontSize = 10.67.sp,
                modifier = Modifier.padding(top = if (showSuggested) 5.33.dp else 10.66.dp).designNode("qs_text_disclose_content_${option.content}"),
            )
            if (showSuggested && option.reason.isNotEmpty()) {
                Text(option.reason, color = Neutral650, fontSize = 10.67.sp, modifier = Modifier.padding(top = 5.33.dp).designNode("qs_text_disclose_reason_${option.content}"))
            }
        }
    }
}

/** Text-variant disclose area (`view_mvb_text_quiz_disclose_options_panel`): a header (checkmark +
 *  "Select the correct answer") with a "Suggested answer"/"Applied" reveal button on the right, over
 *  the vertical option rows. Tapping reveal shows every suggested option's reason + pill. */
@Composable
private fun TextDiscloseArea(options: List<TextDiscloseOption>, selected: Int?, revealed: Boolean, onSelect: (Int) -> Unit, onReveal: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.ic_checkmark), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900))
            Text("Select the correct answer", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.66.dp))
            Spacer(Modifier.weight(1f))
            // Reveal button — enabled: white + violet500 outline/text; applied: white + neutral200/neutral500.
            val tint = if (revealed) Neutral500 else Violet4848F0
            Row(
                Modifier.height(24.dp).clip(RoundedCornerShape(5.33.dp)).background(Color.White)
                    .border(1.dp, if (revealed) Neutral200 else Violet4848F0, RoundedCornerShape(5.33.dp))
                    .then(if (revealed) Modifier else Modifier.clickable(onClick = onReveal))
                    .padding(horizontal = 8.dp).designNode("qs_text_disclose_reveal"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(Res.drawable.ic_sparkles), null, Modifier.size(10.66.dp), colorFilter = ColorFilter.tint(tint))
                Text(if (revealed) "Applied" else "Suggested answer", color = tint, fontSize = 9.33.sp, modifier = Modifier.padding(start = 2.66.dp))
            }
        }
        Column(Modifier.padding(top = 10.66.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.66.dp)) {
            options.forEachIndexed { i, opt -> TextDiscloseOptionItem(opt, selected == i, revealed) { onSelect(i) } }
        }
    }
}

/** A student's answering status during a live quiz (mirrors `AnsweringState`). */
enum class ResponderState { ANSWERED, NOT_SUBMITTED, ABSENT }

/** One student in the responses grid. [answer] = their submitted answer (shown in result mode);
 *  [correct] = whether it matches the disclosed answer (drives green/red in result mode). */
data class QuizResponder(
    val seat: String,
    val name: String,
    val state: ResponderState,
    val answer: String? = null,
    val correct: Boolean? = null,
    // Audio quiz (result mode): an answered cell shows a play/pause control + time instead of text.
    val audioPlaying: Boolean = false,
    val audioTime: String? = null,
    val audioLoading: Boolean = false,
)

internal val sampleResponders: List<QuizResponder> = List(18) { i ->
    val name = sampleStudentNames[i % sampleStudentNames.size]
    val seat = "%02d".format(i + 1)
    when {
        i == 4 -> QuizResponder(seat, name, ResponderState.ABSENT)
        i % 6 == 5 -> QuizResponder(seat, name, ResponderState.NOT_SUBMITTED)
        else -> {
            val isTrue = i % 3 != 0
            QuizResponder(seat, name, ResponderState.ANSWERED, answer = if (isTrue) "T" else "F", correct = isTrue)
        }
    }
}

/** Audio answer control inside a result cell — play/pause icon + elapsed time, or an indeterminate
 *  spinner while the recording loads (`item_mvb_quiz_audio_answering`: ll_audio / cpi_progress_indicator). */
@Composable
private fun AudioControl(r: QuizResponder) {
    if (r.audioLoading) {
        CircularProgressIndicator(Modifier.size(21.dp), color = Neutral900, strokeWidth = 1.dp)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(if (r.audioPlaying) Res.drawable.ic_audio_pause_outline else Res.drawable.ic_audio_play_outline),
                null, Modifier.size(13.dp), colorFilter = ColorFilter.tint(Neutral900),
            )
            Text(r.audioTime ?: "0:00", color = Neutral900, fontSize = 9.33.sp, modifier = Modifier.padding(start = 2.dp))
        }
    }
}

/** One answering cell — `item_mvb_quiz_answering.xml`: number+name header over a state body
 *  (Submitted / Not submitted / Absent), colored per [QuizResponder.state]. */
@Composable
private fun AnsweringCell(r: QuizResponder, modifier: Modifier = Modifier, resultMode: Boolean = false, showName: Boolean = true, audio: Boolean = false, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(8.dp)
    val cardBg: Color
    val headerBg: Color
    val headerText: Color
    val bodyText: String
    val bodyColor: Color
    var border: Pair<Color, Boolean> = Color.Transparent to false
    when (r.state) {
        ResponderState.ANSWERED -> {
            // Quizzing: violet "Submitted". Result: green/red by correctness, showing the answer.
            // Audio has no correctness → answered maps to correct=true (submitted = green, per the
            // audio ViewHolder which paints the answered result card green / violet in quizzing).
            val ok = r.correct == true
            cardBg = if (!resultMode) Violet100EDEDFD else if (ok) GreenE7F7D0 else RedFFECEF
            headerBg = if (!resultMode) Violet4848F0 else if (ok) Green48720F else RedDB0025
            headerText = Color.White
            bodyText = if (resultMode) r.answer ?: "Submitted" else "Submitted"; bodyColor = Neutral900
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .designNode("qs_responder_${r.seat}"),
    ) {
        Text(
            if (showName) "${r.seat}  ${r.name}" else r.seat, color = headerText, fontSize = 8.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().height(23.33.dp).background(headerBg).padding(horizontal = 4.dp).wrapContentHeight(Alignment.CenterVertically),
        )
        Box(Modifier.fillMaxSize().padding(horizontal = 10.66.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            if (audio && resultMode && r.state == ResponderState.ANSWERED) {
                AudioControl(r)
            } else {
                Text(bodyText, color = bodyColor, fontSize = 9.33.sp, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Bar / pie segment styling for a result option (mirrors `CSResultOptionBarItem.BarStyle`). */
enum class BarStyle { CORRECT, INCORRECT, NEUTRAL }

/** One result option bar: [count] of [outOf] responders chose it. */
data class ResultBar(val label: String, val count: Int, val outOf: Int, val isCorrect: Boolean, val style: BarStyle)

internal val sampleResultBars = listOf(
    ResultBar("T", 12, 19, isCorrect = true, BarStyle.CORRECT),
    ResultBar("F", 5, 19, isCorrect = false, BarStyle.INCORRECT),
    ResultBar("Not submitted", 2, 19, isCorrect = false, BarStyle.NEUTRAL),
)

private fun barColor(style: BarStyle) = when (style) {
    BarStyle.CORRECT -> Green48720F
    BarStyle.INCORRECT -> RedDB0025
    BarStyle.NEUTRAL -> Neutral500
}

/** WCAG accessibility overlay (`WcagPatternTiles`): correct=dots, incorrect=slashes, no-answer=cross-hatch,
 *  drawn in 8dp tiles at ~8% black so the bar/segment color stays vivid. Shared by bars + pie chart. */
private fun DrawScope.drawWcag(style: BarStyle) {
    val tile = 8.dp.toPx()
    val c = Color(0x14000000)
    val sw = 1.dp.toPx().coerceAtLeast(2f)
    fun slashesForward() { var o = -size.height; while (o < size.width) { drawLine(c, Offset(o, size.height), Offset(o + size.height, 0f), sw); o += tile } }
    fun slashesBack() { var o = -size.height; while (o < size.width) { drawLine(c, Offset(o, 0f), Offset(o + size.height, size.height), sw); o += tile } }
    when (style) {
        BarStyle.CORRECT -> {
            val r = 1.dp.toPx().coerceAtLeast(2f)
            var y = 0f
            while (y < size.height) { var x = 0f; while (x < size.width) { drawCircle(c, r, Offset(x + tile * 0.25f, y + tile * 0.25f)); drawCircle(c, r, Offset(x + tile * 0.75f, y + tile * 0.75f)); x += tile }; y += tile }
        }
        BarStyle.INCORRECT -> slashesForward()
        BarStyle.NEUTRAL -> { slashesForward(); slashesBack() }
    }
}

private fun Modifier.wcagPattern(style: BarStyle): Modifier = drawBehind { drawWcag(style) }

@Composable
private fun ResultBarChip(text: String, bg: Color, textColor: Color, border: Color? = null, circle: Boolean = false) {
    val shape = RoundedCornerShape(50)
    var m = Modifier.height(13.33.dp).clip(shape).background(bg)
    if (border != null) m = m.border(1.33.dp, border, shape)
    m = if (circle) m.width(13.33.dp) else m.padding(horizontal = 5.33.dp)
    Box(m, contentAlignment = Alignment.Center) {
        Text(text, color = textColor, fontSize = 8.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** A result option bar — `CSResultOptionBarItem`: chips row (label + "Correct answer" + responses
 *  count) over a ratio track whose fill is colored + WCAG-patterned; non-highlighted bars dim to 0.2. */
@Composable
private fun ResultOptionBar(bar: ResultBar, highlighted: Boolean, onClick: () -> Unit) {
    val fillShape = RoundedCornerShape(50)
    val fraction = if (bar.outOf <= 0) 0f else bar.count.toFloat() / bar.outOf
    Column(
        Modifier.fillMaxWidth().alpha(if (highlighted) 1f else 0.2f).clickable(onClick = onClick)
            .padding(vertical = 5.33.dp).designNode("qs_result_bar_${bar.label}"),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ResultBarChip(bar.label, Neutral100, Neutral900, circle = bar.label.length <= 1)
            if (bar.isCorrect) {
                Spacer(Modifier.width(5.33.dp))
                ResultBarChip("Correct answer", Violet4848F0, Color.White)
            }
            Spacer(Modifier.weight(1f))
            ResultBarChip(if (bar.count == 1) "1 response" else "${bar.count} responses", Color.Transparent, Neutral900, border = Neutral300)
        }
        Box(Modifier.padding(top = 5.33.dp).fillMaxWidth().height(10.66.dp).clip(fillShape).background(Neutral100)) {
            Box(
                (if (bar.count <= 0) Modifier.width(5.33.dp) else Modifier.fillMaxWidth(fraction.coerceAtLeast(0.02f)))
                    .fillMaxHeight().clip(fillShape).background(barColor(bar.style)).wcagPattern(bar.style),
            )
        }
    }
}

/** Result Overview / Student-responses tab bar (`ll_result_tabs`). */
@Composable
private fun ResultTabs(overviewActive: Boolean, onSelect: (Boolean) -> Unit) {
    @Composable
    fun Tab(label: String, active: Boolean, node: String, onClick: () -> Unit) {
        // width(IntrinsicSize.Max) so the column wraps to the text — the underline fills the text
        // width (not the whole row, which would push the other tab off-screen).
        Column(Modifier.width(IntrinsicSize.Max).clickable(onClick = onClick).padding(horizontal = 10.66.dp).padding(top = 5.33.dp).designNode(node)) {
            Text(label, color = if (active) Violet4848F0 else Neutral900, fontSize = 9.33.sp, maxLines = 1)
            Box(Modifier.padding(top = 5.33.dp).fillMaxWidth().height(1.33.dp).background(if (active) Violet4848F0 else Color.Transparent))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        Tab("Overview", overviewActive, "qs_tab_overview") { onSelect(true) }
        Spacer(Modifier.width(10.66.dp))
        Tab("Student responses", !overviewActive, "qs_tab_student") { onSelect(false) }
    }
}

/** Left result options area — `ll_result_options_area`: title + hint + the option bars
 *  (no bar selected → all bars full-alpha; selecting one dims the rest). */
@Composable
private fun ResultOptionsArea(bars: List<ResultBar>, highlighted: Int?, onBarClick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.ic_mvb_quizzing_options), null, Modifier.size(16.dp))
            Text("Options", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.66.dp))
            Text("Click an option to highlight students", color = Neutral900, fontSize = 8.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 5.33.dp))
        }
        Column(Modifier.padding(top = 5.33.dp).fillMaxWidth()) {
            bars.forEachIndexed { i, bar ->
                ResultOptionBar(bar, highlighted == null || highlighted == i) { onBarClick(i) }
            }
        }
    }
}

/** Correct-answer badge — `CSResultCorrectAnswerBadge`: a 48dp white CIRCLE (oval) with neutral_300
 *  border and the big answer letter centered. */
@Composable
private fun CorrectAnswerBadge(label: String) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color.White).border(0.66.dp, Neutral300, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Neutral900, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
}

/** One analytic chip — `CSResultAnalyticChip`: header pill ((icon +) label in a `bg_result_chip_outline`
 *  pill — transparent fill, neutral_300 hairline, full radius), big count, "students"; all centered. */
@Composable
private fun AnalyticChip(icon: DrawableResource?, label: String, count: Int, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(50)
    Column(modifier.padding(vertical = 5.33.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.height(13.33.dp).clip(pill).border(1.33.dp, Neutral300, pill).padding(horizontal = 5.33.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Image(painterResource(icon), null, Modifier.padding(end = 2.66.dp).size(8.dp), colorFilter = ColorFilter.tint(Neutral900))
            Text(label, color = Neutral900, fontSize = 8.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("$count", color = Neutral900, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.33.dp))
        Text("students", color = Neutral900, fontSize = 8.sp)
    }
}

/** Answer-distribution pie — `CSAnswerPieChart`: correct/incorrect/no-answer arcs from top, each with
 *  its WCAG pattern + a neutral_300 wedge border. */
@Composable
private fun PieChart(correct: Int, incorrect: Int, noAnswer: Int, modifier: Modifier = Modifier) {
    val total = (correct + incorrect + noAnswer).coerceAtLeast(1)
    val segs = listOf(Triple(correct, BarStyle.CORRECT, Green48720F), Triple(incorrect, BarStyle.INCORRECT, RedDB0025), Triple(noAnswer, BarStyle.NEUTRAL, Neutral500))
    Canvas(modifier) {
        val border = 4.dp.toPx()
        val tl = Offset(border / 2f, border / 2f)
        val sz = Size(size.width - border, size.height - border)
        val arcRect = Rect(tl, sz)
        var start = -90f
        segs.forEach { (count, style, color) ->
            if (count <= 0) return@forEach
            val sweep = count.toFloat() / total * 360f
            drawArc(color, start, sweep, useCenter = true, topLeft = tl, size = sz)
            val slice = Path().apply { moveTo(center.x, center.y); arcTo(arcRect, start, sweep, false); close() }
            // A 360° slice path (arc start == end) is degenerate → clipPath collapses and the WCAG
            // fill vanishes (single 100% segment, e.g. all-not-submitted). Clip to an oval instead;
            // the slice still draws the border so the top seam line is kept, matching the old build.
            val clip = if (sweep >= 359.99f) Path().apply { addOval(arcRect) } else slice
            clipPath(clip) { drawWcag(style) }
            drawPath(slice, Neutral300, style = Stroke(width = border))
            start += sweep
        }
    }
}

/** Pie legend item — WCAG swatch + "<rate> %". */
@Composable
private fun LegendItem(style: BarStyle, color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).clip(RoundedCornerShape(1.33.dp)).background(color).wcagPattern(style))
        Text(text, color = Neutral900, fontSize = 9.33.sp, modifier = Modifier.padding(start = 5.33.dp))
    }
}

/** Overview flavor (all no-correct-answer ones share the 2-chip + 2-segment-pie layout, differing only
 *  in the submitted chip icon / legend text):
 *  - GRADED (TF/MC): correct-answer badge + correct/incorrect/no-answer.
 *  - POLL: "Submitted"/"Not submitted" legend, no chip icon.
 *  - SUBMISSION (short answer): submitted legend reads "Correct rate" (per old MvbShortAnswerStartWindow).
 *  - AUDIO: like POLL ("Submitted" legend) but the Submitted chip carries a check icon. */
enum class OverviewMode { GRADED, POLL, SUBMISSION, AUDIO }

/** Result Overview tab content — `sv_result_overview_content`: correct-answer card (badges +
 *  3 analytic chips) + pie chart + WCAG legend. */
@Composable
private fun ResultOverview(correctLabels: List<String>, correct: Int, incorrect: Int, noAnswer: Int, mode: OverviewMode) {
    val attendance = correct + incorrect + noAnswer
    fun rate(n: Int) = if (attendance <= 0) 0 else (n * 100f / attendance).roundToInt()
    val noCorrect = mode != OverviewMode.GRADED
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.66.dp)).background(Neutral100)
                .border(1.33.dp, Neutral300, RoundedCornerShape(10.66.dp)).padding(horizontal = 16.dp, vertical = 10.66.dp),
        ) {
            if (!noCorrect) {
                // No correct answer in a poll / submission quiz → skip the correct-answer label + badges.
                Text("Correct answer", color = Neutral900, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.padding(top = 5.33.dp), horizontalArrangement = Arrangement.spacedBy(5.33.dp)) {
                    correctLabels.forEach { CorrectAnswerBadge(it) }
                }
                Box(Modifier.padding(top = 10.66.dp).fillMaxWidth().height(0.66.dp).background(Neutral300))
            }
            Row(Modifier.padding(top = 5.33.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (noCorrect) {
                    AnalyticChip(if (mode == OverviewMode.AUDIO) Res.drawable.ic_check_white else null, "Submitted", correct, Modifier.weight(1f))
                    Box(Modifier.width(0.66.dp).height(45.dp).background(Neutral300))
                    AnalyticChip(null, "Not submitted", noAnswer, Modifier.weight(1f))
                } else {
                    AnalyticChip(Res.drawable.ic_check_white, "Answered correctly", correct, Modifier.weight(1f))
                    Box(Modifier.width(0.66.dp).height(45.dp).background(Neutral300))
                    AnalyticChip(Res.drawable.ic_cross, "Answered incorrectly", incorrect, Modifier.weight(1f))
                    Box(Modifier.width(0.66.dp).height(45.dp).background(Neutral300))
                    AnalyticChip(null, "Not submitted", noAnswer, Modifier.weight(1f))
                }
            }
        }
        PieChart(correct, incorrect, noAnswer, Modifier.padding(top = 16.dp).size(160.dp).align(Alignment.CenterHorizontally))
        Row(Modifier.padding(top = 16.dp, bottom = 10.66.dp).align(Alignment.CenterHorizontally)) {
            when (mode) {
                OverviewMode.POLL, OverviewMode.AUDIO -> {
                    LegendItem(BarStyle.CORRECT, Green48720F, "Submitted ${rate(correct)}%")
                    Spacer(Modifier.width(10.66.dp))
                    LegendItem(BarStyle.NEUTRAL, Neutral500, "Not submitted ${rate(noAnswer)}%")
                }
                OverviewMode.SUBMISSION -> {
                    LegendItem(BarStyle.CORRECT, Green48720F, "Correct rate ${rate(correct)}%")
                    Spacer(Modifier.width(10.66.dp))
                    LegendItem(BarStyle.NEUTRAL, Neutral500, "Not submitted ${rate(noAnswer)}%")
                }
                OverviewMode.GRADED -> {
                    LegendItem(BarStyle.CORRECT, Green48720F, "Correct rate ${rate(correct)}%")
                    Spacer(Modifier.width(10.66.dp))
                    LegendItem(BarStyle.INCORRECT, RedDB0025, "Incorrect rate ${rate(incorrect)}%")
                    Spacer(Modifier.width(10.66.dp))
                    LegendItem(BarStyle.NEUTRAL, Neutral500, "Not submitted ${rate(noAnswer)}%")
                }
            }
        }
    }
}

/** Show-students'-name toggle row (`ll_show_students_name`) — label + a small violet switch. */
@Composable
private fun ShowNamesToggle(checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Show students' name", color = Neutral900, fontSize = 9.33.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(5.33.dp))
        // Compact track+thumb switch (mirrors selector_mvb_audio_switch_*: violet on, neutral off).
        val track = if (checked) Violet4848F0 else Neutral300
        Box(
            Modifier.width(34.dp).height(20.dp).clip(RoundedCornerShape(50)).background(track)
                .clickable { onToggle(!checked) }.padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) { Box(Modifier.size(16.dp).clip(CircleShape).background(Color.White)) }
    }
}

/** Short-answer student-answer popup (`fl_student_answer_popup`): a 480×288 white card over a 35%-black
 *  scrim — scrollable answer in a violet-bordered box, a show-names toggle, prev/name-chip/next, close. */
@Composable
private fun StudentAnswerPopup(
    responder: QuizResponder,
    showNames: Boolean,
    hasPrev: Boolean,
    hasNext: Boolean,
    onToggleNames: (Boolean) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0x59000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(480.dp).height(288.dp).shadow(8.dp, RoundedCornerShape(10.66.dp)).background(Color.White)
                .padding(top = 10.66.dp, bottom = 16.dp).padding(horizontal = 10.66.dp)
                .designNode("qs_answer_popup"),
        ) {
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.66.dp))
                    .border(1.33.dp, Violet4848F0, RoundedCornerShape(10.66.dp)),
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 21.33.dp, vertical = 16.dp)) {
                    Text(responder.answer.orEmpty(), color = Neutral900, fontSize = 28.sp)
                }
            }
            Row(Modifier.padding(top = 16.dp).fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                ShowNamesToggle(showNames, onToggleNames)
                Spacer(Modifier.weight(1f))
                Image(
                    painterResource(Res.drawable.ic_previous_arrow), "Previous",
                    Modifier.size(32.dp).alpha(if (hasPrev) 1f else 0.3f).then(if (hasPrev) Modifier.clickable(onClick = onPrev) else Modifier).padding(8.dp).designNode("qs_popup_prev"),
                    colorFilter = ColorFilter.tint(Neutral900),
                )
                Box(
                    Modifier.padding(horizontal = 5.33.dp).height(32.dp).clip(RoundedCornerShape(8.dp)).background(Neutral100).padding(horizontal = 13.33.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (showNames) responder.name else responder.seat, color = Neutral900, fontSize = 13.33.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
                Image(
                    painterResource(Res.drawable.ic_previous_arrow), "Next",
                    Modifier.size(32.dp).rotate(180f).alpha(if (hasNext) 1f else 0.3f).then(if (hasNext) Modifier.clickable(onClick = onNext) else Modifier).padding(8.dp).designNode("qs_popup_next"),
                    colorFilter = ColorFilter.tint(Neutral900),
                )
                Spacer(Modifier.weight(1f))
                Image(
                    painterResource(Res.drawable.ic_close), "Close",
                    Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClose).padding(6.66.dp).designNode("qs_popup_close"),
                    colorFilter = ColorFilter.tint(Neutral900),
                )
            }
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
    typeSubtitle: String? = null,
    state: QuizPanelState = QuizPanelState.QUIZZING,
    joined: Int = 21,
    capacity: Int = 30,
    stopwatch: String = "00:00",
    options: List<String> = type.chips,
    multiSelectDisclose: Boolean = false,
    pollMode: Boolean = false,
    submissionMode: Boolean = false,
    answerPopup: Boolean = false,
    audioMode: Boolean = false,
    startOnOverview: Boolean = true, // result: start on Overview tab (false = Student-responses; for previews)
    textDiscloseOptions: List<TextDiscloseOption> = emptyList(), // TEXT_TRUE_FALSE disclose rows
    startDiscloseRevealed: Boolean = false, // preview hook: start with the AI reason/pill revealed
    startDiscloseSelected: Int? = null, // preview hook: pre-select a disclose option index
    responders: List<QuizResponder> = sampleResponders,
    resultBars: List<ResultBar> = sampleResultBars,
    screenshot: @Composable (Modifier) -> Unit = {},
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onEndAndReview: () -> Unit = {},
    onPublishDisclose: (List<Int>) -> Unit = {},
    onAudioToggle: (QuizResponder) -> Unit = {},
) {
    // Text variants (TEXT_*) render the question as Compose text / a KatexView hole in a neutral100
    // box, vs the screenshot types' white framed bitmap. TEXT_TRUE_FALSE also uses vertical chips.
    val isTextType = type == MvbQuizType.TEXT_TRUE_FALSE || type == MvbQuizType.TEXT_SHORT_ANSWER
    val useTextDisclose = type == MvbQuizType.TEXT_TRUE_FALSE && textDiscloseOptions.isNotEmpty()
    var discloseSelected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var textDiscloseSelected by remember { mutableStateOf(startDiscloseSelected) }
    var answersRevealed by remember { mutableStateOf(startDiscloseRevealed) }
    var resultOverview by remember { mutableStateOf(startOnOverview) } // result defaults to the Overview tab (pie), as in the original
    var highlightedBar by remember { mutableStateOf<Int?>(null) }
    var showNames by remember { mutableStateOf(true) }
    var popupResponder by remember { mutableStateOf<QuizResponder?>(null) }
    // 8dp outer padding gives the elevation shadow room (mirrors the window shell's outer padding).
    Box(Modifier.padding(8.dp)) {
      Column(
        Modifier.size(853.dp, 480.dp).shadow(5.33.dp, RoundedCornerShape(10.66.dp)).background(Color.White).designNode("mvb_quiz_start"),
      ) {
        // ---- Header ----
        Row(Modifier.fillMaxWidth().height(32.dp).background(Color.White).padding(horizontal = 10.66.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.ic_mvb_quizzing_header), null, Modifier.size(21.33.dp))
            Text("Question", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.33.dp).weight(1f).designNode("qs_title"))
            Image(painterResource(Res.drawable.ic_minus_32dp), "Minimize", Modifier.size(16.dp).clickable(onClick = onMinimize).designNode("qs_minimize"), colorFilter = ColorFilter.tint(Neutral900))
            Spacer(Modifier.width(8.dp))
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
                    if (typeSubtitle != null) {
                        Text(typeSubtitle, color = Gray999, fontSize = 8.sp, modifier = Modifier.padding(start = 5.33.dp).designNode("qs_type_subtitle"))
                    }
                    Spacer(Modifier.weight(1f))
                    if (state == QuizPanelState.QUIZZING) {
                        Image(painterResource(Res.drawable.ic_mvb_quizzing_stopwatch), null, Modifier.size(16.dp))
                        Text(stopwatch, color = Neutral500, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 2.66.dp).designNode("qs_stopwatch"))
                    }
                }
                // Question area (slot, injected by the window). Screenshot types: white framed box holding
                // the captured bitmap. Text types: a neutral100 box holding Compose text / a hole for the
                // native KatexView (`fl_text_preview` → bg_mvb_neutral100_radius300, 10.66dp inner padding).
                val boxShape = RoundedCornerShape(8.dp)
                Box(
                    Modifier.padding(top = 10.66.dp).fillMaxWidth().height(169.dp).clip(boxShape)
                        .background(if (isTextType) Neutral100 else Color.White)
                        .then(if (isTextType) Modifier else Modifier.border(0.66.dp, Neutral300, boxShape))
                        .designNode("qs_screenshot"),
                ) {
                    screenshot(
                        if (isTextType) Modifier.fillMaxSize().padding(10.66.dp)
                        else Modifier.fillMaxSize().clip(boxShape),
                    )
                }
                // Mid area: options (QUIZZING) / answer selector (DISCLOSE) / result bars (RESULT)
                when (state) {
                    QuizPanelState.DISCLOSE -> Box(Modifier.padding(top = 10.66.dp)) {
                        if (useTextDisclose) {
                            TextDiscloseArea(
                                textDiscloseOptions, textDiscloseSelected, answersRevealed,
                                onSelect = { textDiscloseSelected = it }, onReveal = { answersRevealed = true },
                            )
                        } else {
                            DiscloseSelectorArea(options, discloseSelected, multiSelectDisclose) { i ->
                                discloseSelected = when {
                                    !multiSelectDisclose -> setOf(i)
                                    i in discloseSelected -> discloseSelected - i
                                    else -> discloseSelected + i
                                }
                            }
                        }
                    }
                    QuizPanelState.RESULT -> Box(Modifier.padding(top = 10.66.dp)) {
                        ResultOptionsArea(resultBars, highlightedBar) { i -> highlightedBar = if (highlightedBar == i) null else i }
                    }
                    QuizPanelState.QUIZZING -> if (options.isNotEmpty()) {
                        Box(Modifier.padding(top = 10.66.dp)) { SectionLabel(Res.drawable.ic_mvb_quizzing_options, "Options") }
                        if (type == MvbQuizType.TEXT_TRUE_FALSE) {
                            // Text TF: full-width vertical chips (True / False), not the side-by-side squares.
                            Column(Modifier.padding(top = 10.66.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.66.dp)) {
                                options.forEach { VerticalOptionChip(it) }
                            }
                        } else {
                            Row(Modifier.padding(top = 10.66.dp).fillMaxWidth().height(67.33.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options.forEach { OptionChip(it, Modifier.weight(1f).designNode("qs_chip_$it")) }
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                // Bottom action: End-and-review (QUIZZING) / Show-result (DISCLOSE) / none (RESULT)
                when (state) {
                    QuizPanelState.DISCLOSE -> {
                        val enabled = if (useTextDisclose) textDiscloseSelected != null else discloseSelected.isNotEmpty()
                        Box(
                            Modifier.padding(top = 10.66.dp).fillMaxWidth().height(37.33.dp)
                                .clip(RoundedCornerShape(5.33.dp)).background(if (enabled) Violet4848F0 else Neutral200)
                                .clickable(enabled = enabled) {
                                    if (useTextDisclose) onPublishDisclose(listOfNotNull(textDiscloseSelected)) else onPublishDisclose(discloseSelected.sorted())
                                }
                                .designNode("qs_disclose_publish"),
                            contentAlignment = Alignment.Center,
                        ) { Text("Show question(s) result", color = if (enabled) Color.White else Neutral500, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    }
                    QuizPanelState.QUIZZING -> Box(
                        Modifier.padding(top = 10.66.dp).fillMaxWidth().height(37.33.dp)
                            .clip(RoundedCornerShape(5.33.dp)).background(Color.White).border(0.66.dp, RedDB0025, RoundedCornerShape(5.33.dp))
                            .clickable(onClick = onEndAndReview)
                            .designNode("qs_end_review"),
                        contentAlignment = Alignment.Center,
                    ) { Text("End and review question", color = RedDB0025, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    QuizPanelState.RESULT -> Unit
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
                    val result = state == QuizPanelState.RESULT
                    if (result) {
                        Box(Modifier.padding(top = 10.66.dp)) { ResultTabs(resultOverview) { resultOverview = it } }
                    }
                    if (result && resultOverview) {
                        val correct = resultBars.filter { it.style == BarStyle.CORRECT }.sumOf { it.count }
                        val incorrect = resultBars.filter { it.style == BarStyle.INCORRECT }.sumOf { it.count }
                        val noAns = resultBars.filter { it.style == BarStyle.NEUTRAL }.sumOf { it.count }
                        val correctLabels = resultBars.filter { it.isCorrect }.map { it.label }
                        val overviewMode = when {
                            pollMode -> OverviewMode.POLL
                            audioMode -> OverviewMode.AUDIO
                            submissionMode -> OverviewMode.SUBMISSION
                            else -> OverviewMode.GRADED
                        }
                        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 10.66.dp)) {
                            ResultOverview(correctLabels, correct, incorrect, noAns, overviewMode)
                        }
                    } else {
                        val hlBar = highlightedBar?.let { resultBars.getOrNull(it) }
                        Column(Modifier.padding(top = 10.66.dp).fillMaxWidth().weight(1f)) {
                            // Show-students'-name toggle — result + popup/audio mode (short-answer/audio) only.
                            if (result && (answerPopup || audioMode)) {
                                Box(Modifier.fillMaxWidth().padding(bottom = 10.66.dp)) { ShowNamesToggle(showNames) { showNames = it } }
                            }
                            Column(
                                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.66.dp),
                            ) {
                                responders.chunked(4).forEach { rowItems ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.66.dp)) {
                                        rowItems.forEach { r ->
                                            val match = hlBar == null || if (hlBar.style == BarStyle.NEUTRAL) r.state != ResponderState.ANSWERED else r.answer == hlBar.label
                                            val click: (() -> Unit)? = when {
                                                result && answerPopup && r.state == ResponderState.ANSWERED -> ({ popupResponder = r })
                                                result && audioMode && r.state == ResponderState.ANSWERED -> ({ onAudioToggle(r) })
                                                else -> null
                                            }
                                            AnsweringCell(r, Modifier.weight(1f).alpha(if (match) 1f else 0.2f), resultMode = result, showName = showNames, audio = audioMode, onClick = click)
                                        }
                                        repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
      }
      // Student-answer popup overlays the whole window (short-answer / audio result mode).
      popupResponder?.let { r ->
          val answered = responders.filter { it.state == ResponderState.ANSWERED }
          val idx = answered.indexOfFirst { it.seat == r.seat }
          StudentAnswerPopup(
              responder = r, showNames = showNames,
              hasPrev = idx > 0, hasNext = idx in 0 until answered.lastIndex,
              onToggleNames = { showNames = it },
              onPrev = { if (idx > 0) popupResponder = answered[idx - 1] },
              onNext = { if (idx in 0 until answered.lastIndex) popupResponder = answered[idx + 1] },
              onClose = { popupResponder = null },
          )
      }
    }
}