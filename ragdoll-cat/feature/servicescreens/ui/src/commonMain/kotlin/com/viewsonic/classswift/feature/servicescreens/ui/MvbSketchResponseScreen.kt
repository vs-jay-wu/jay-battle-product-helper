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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_arrow_clockwise_16
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_check_green_small
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_chevron_left_16
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_chevron_right_16
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_minus_32dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_header
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_options
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_responses
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_quizzing_stopwatch
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_pen_curve_line
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_pen_straight_line
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

/** Sketch Response has two mutually-exclusive panels (mirrors [com.viewsonic.classswift.ui.windowmodel.quiz.enums.SketchState]). */
enum class SketchPanelState { ANSWERING, RESULT }

/** The sketch question preview / answering screenshot frame: 169dp white box, neutral300 hairline. */
@Composable
private fun QuestionFrame(modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    val shape = RoundedCornerShape(10.66.dp)
    Box(
        modifier.fillMaxWidth().height(169.33.dp).clip(shape).background(Color.White)
            .border(1.33.dp, Neutral300, shape).padding(1.33.dp).designNode("sketch_question"),
    ) { content(Modifier.fillMaxSize().clip(shape)) }
}

/** Sketch result Overview big-number box — two columns (Submitted / Not submitted) each a chip + a
 *  32sp count + "Students", split by a hairline (`bg_neutral100_radius1200_line_neutral300_border400`). */
@Composable
private fun OverviewCountBox(submitted: Int, notSubmitted: Int) {
    @Composable
    fun Col(chipIcon: Boolean, label: String, count: Int) {
        Column(Modifier.padding(top = 10.66.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).border(1.33.dp, Neutral300, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 1.33.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (chipIcon) Image(painterResource(Res.drawable.ic_check_green_small), null, Modifier.padding(end = 2.66.dp).size(10.dp))
                Text(label, color = Neutral900, fontSize = 8.sp, fontWeight = FontWeight.Medium)
            }
            Text("$count", color = Neutral900, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.33.dp))
            Text("Students", color = Neutral900, fontSize = 9.33.sp, modifier = Modifier.padding(top = 5.33.dp))
        }
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Neutral100)
            .border(1.33.dp, Neutral300, RoundedCornerShape(16.dp)).padding(vertical = 5.33.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Col(chipIcon = true, "Submitted", submitted) }
        Box(Modifier.width(1.33.dp).height(45.33.dp).background(Neutral300))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Col(chipIcon = false, "Not submitted", notSubmitted) }
    }
}

/** Result Overview / Student-responses tab bar (`ll_result_tabs`). */
@Composable
private fun SketchTabs(overviewActive: Boolean, onSelect: (Boolean) -> Unit) {
    @Composable
    fun Tab(label: String, active: Boolean, node: String, onClick: () -> Unit) {
        // width(IntrinsicSize.Max) so the column wraps to the text — else the underline's fillMaxWidth
        // expands the tab to the whole row and pushes the other tab off-screen.
        Column(Modifier.width(IntrinsicSize.Max).clickable(onClick = onClick).padding(horizontal = 8.dp).padding(vertical = 5.33.dp).designNode(node)) {
            Text(label, color = if (active) Violet4848F0 else Neutral900, fontSize = 9.33.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Box(Modifier.padding(top = 5.33.dp).fillMaxWidth().height(1.33.dp).background(if (active) Violet4848F0 else Color.Transparent))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        Tab("Overview", overviewActive, "sketch_tab_overview") { onSelect(true) }
        Tab("Student responses", !overviewActive, "sketch_tab_student") { onSelect(false) }
    }
}

/**
 * CMP port of `MvbSketchResponseStartWindow` (hybrid: the marking review canvas + downloaded images
 * stay native — the window injects the [questionScreenshot]/[questionPreview] image slots and the
 * [studentResponses] thumbnail grid). [state] toggles ANSWERING (student monitoring + "Collect all
 * and mark") and RESULT (question preview + Submitted/Not-submitted bars + Overview pie / responses).
 */
@Composable
fun MvbSketchResponseScreen(
    state: SketchPanelState = SketchPanelState.ANSWERING,
    inProgressLabel: String = "1 sketch in progress",
    questionTitle: String = "1. Sketch response",
    stopwatch: String = "00:00",
    submitted: Int = 0,
    notSubmitted: Int = 0,
    startOnOverview: Boolean = true,
    responders: List<QuizResponder> = sampleResponders,
    questionScreenshot: @Composable (Modifier) -> Unit = {},
    questionPreview: @Composable (Modifier) -> Unit = {},
    studentResponses: @Composable () -> Unit = {},
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onCollectAndMark: () -> Unit = {},
) {
    var overview by remember { mutableStateOf(startOnOverview) }
    val cardHeight = if (state == SketchPanelState.RESULT) 522.67.dp else 480.dp
    Box(Modifier.padding(8.dp)) {
        Column(
            Modifier.width(853.dp).height(cardHeight).shadow(5.33.dp, RoundedCornerShape(10.66.dp)).background(Color.White).designNode("mvb_sketch"),
        ) {
            // ---- Header ----
            Row(Modifier.fillMaxWidth().height(32.dp).background(Color.White).padding(horizontal = 10.66.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(Res.drawable.ic_mvb_quizzing_header), null, Modifier.size(21.33.dp))
                Text("Question", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.33.dp).weight(1f).designNode("sketch_title"))
                Image(painterResource(Res.drawable.ic_minus_32dp), "Minimize", Modifier.size(16.dp).clickable(onClick = onMinimize).designNode("sketch_minimize"), colorFilter = ColorFilter.tint(Neutral900))
                Spacer(Modifier.width(8.dp))
                Image(painterResource(Res.drawable.ic_close), "Close", Modifier.size(16.dp).clickable(onClick = onClose).designNode("sketch_close"))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE6E6E6)))
            when (state) {
                SketchPanelState.ANSWERING -> AnsweringPanel(Modifier.weight(1f).fillMaxWidth(), inProgressLabel, stopwatch, responders, questionScreenshot, onRefresh, onCollectAndMark)
                SketchPanelState.RESULT -> ResultPanel(Modifier.weight(1f).fillMaxWidth(), questionTitle, submitted, notSubmitted, overview, { overview = it }, questionPreview, studentResponses, onRefresh)
            }
        }
    }
}

@Composable
private fun AnsweringPanel(
    modifier: Modifier,
    inProgressLabel: String,
    stopwatch: String,
    responders: List<QuizResponder>,
    questionScreenshot: @Composable (Modifier) -> Unit,
    onRefresh: () -> Unit,
    onCollectAndMark: () -> Unit,
) {
    Row(modifier.background(Color.White)) {
        Column(Modifier.width(333.33.dp).fillMaxHeight().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(Res.drawable.ic_pen_curve_line), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900))
                Text(inProgressLabel, color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.66.dp).designNode("sketch_label"))
                Spacer(Modifier.weight(1f))
                Image(painterResource(Res.drawable.ic_mvb_quizzing_stopwatch), null, Modifier.size(16.dp))
                Text(stopwatch, color = Neutral500, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 2.66.dp).designNode("sketch_stopwatch"))
            }
            Box(Modifier.padding(top = 10.66.dp)) { QuestionFrame { m -> questionScreenshot(m) } }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.padding(top = 10.66.dp).fillMaxWidth().height(37.33.dp).clip(RoundedCornerShape(5.33.dp))
                    .background(Color.White).border(0.66.dp, RedDB0025, RoundedCornerShape(5.33.dp))
                    .clickable(onClick = onCollectAndMark).designNode("sketch_collect"),
                contentAlignment = Alignment.Center,
            ) { Text("Collect all and mark", color = RedDB0025, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
        }
        ResponsesWrapper(Modifier.weight(1f).fillMaxHeight(), responders = responders, onRefresh = onRefresh)
    }
}

/** ANSWERING right wrapper — Responses header (+ refresh + count) over the student monitoring grid. */
@Composable
private fun ResponsesWrapper(modifier: Modifier, responders: List<QuizResponder>, onRefresh: () -> Unit) {
    val submitted = responders.count { it.state == ResponderState.ANSWERED }
    val total = responders.count { it.state != ResponderState.ABSENT }
    Column(modifier.padding(end = 16.dp).padding(vertical = 16.dp)) {
        Column(Modifier.fillMaxSize().clip(RoundedCornerShape(10.66.dp)).background(Neutral100).padding(10.66.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(Res.drawable.ic_mvb_quizzing_responses, "Responses")
                Row(
                    Modifier.padding(start = 8.dp).clip(RoundedCornerShape(5.33.dp)).border(0.66.dp, Neutral300, RoundedCornerShape(5.33.dp))
                        .clickable(onClick = onRefresh).padding(horizontal = 5.33.dp, vertical = 2.66.dp).designNode("sketch_refresh"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(Res.drawable.ic_arrow_clockwise_16), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900))
                    Text("Refresh", color = Neutral900, fontSize = 9.33.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 2.66.dp))
                }
                Spacer(Modifier.weight(1f))
                Text("$submitted / $total", color = Neutral900, fontSize = 10.67.sp, modifier = Modifier.designNode("sketch_count"))
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

@Composable
private fun ResultPanel(
    modifier: Modifier,
    questionTitle: String,
    submitted: Int,
    notSubmitted: Int,
    overview: Boolean,
    onSelectTab: (Boolean) -> Unit,
    questionPreview: @Composable (Modifier) -> Unit,
    studentResponses: @Composable () -> Unit,
    onRefresh: () -> Unit,
) {
    val total = submitted + notSubmitted
    Column(modifier) {
        Row(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
            // Left: question section (white)
            Column(Modifier.width(333.33.dp).fillMaxHeight().background(Color.White).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(Res.drawable.ic_pen_straight_line), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900))
                    Text(questionTitle, color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.66.dp).designNode("sketch_result_title"))
                }
                Box(Modifier.padding(top = 10.66.dp)) { QuestionFrame { m -> questionPreview(m) } }
                Row(Modifier.padding(top = 10.66.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(Res.drawable.ic_mvb_quizzing_options), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900))
                    Text("Options", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.66.dp))
                    Text("Click an option to highlight students", color = Neutral900, fontSize = 8.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 5.33.dp))
                }
                Column(Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    ResultOptionBar(ResultBar("Submitted", submitted, total, isCorrect = false, BarStyle.CORRECT), highlighted = true) {}
                    ResultOptionBar(ResultBar("Not submitted", notSubmitted, total, isCorrect = false, BarStyle.NEUTRAL), highlighted = true) {}
                }
            }
            // Right: response wrapper (neutral100)
            Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.66.dp)).background(Neutral100).padding(vertical = 10.66.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.66.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(Res.drawable.ic_mvb_quizzing_responses, "Responses")
                    Row(
                        Modifier.padding(start = 8.dp).clip(RoundedCornerShape(5.33.dp)).border(0.66.dp, Neutral300, RoundedCornerShape(5.33.dp))
                            .clickable(onClick = onRefresh).padding(horizontal = 5.33.dp, vertical = 2.66.dp).designNode("sketch_result_refresh"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(painterResource(Res.drawable.ic_arrow_clockwise_16), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900))
                        Text("Refresh", color = Neutral900, fontSize = 9.33.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 2.66.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("$submitted / $total", color = Neutral900, fontSize = 10.67.sp, modifier = Modifier.designNode("sketch_result_count"))
                }
                Box(Modifier.padding(top = 10.66.dp).padding(horizontal = 10.66.dp)) { SketchTabs(overview, onSelectTab) }
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 5.33.dp).padding(horizontal = 10.66.dp)) {
                    if (overview) {
                        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            OverviewCountBox(submitted, notSubmitted)
                            PieChart(submitted, 0, notSubmitted, Modifier.padding(top = 26.66.dp).size(160.dp).align(Alignment.CenterHorizontally))
                            Row(Modifier.padding(top = 16.dp, bottom = 10.66.dp).align(Alignment.CenterHorizontally)) {
                                fun pct(n: Int) = if (total <= 0) 0 else (n * 100f / total).roundToInt()
                                LegendItem(BarStyle.CORRECT, Green48720F, "Submitted ${pct(submitted)}%")
                                Spacer(Modifier.width(10.66.dp))
                                LegendItem(BarStyle.NEUTRAL, Neutral500, "Not submitted ${pct(notSubmitted)}%")
                            }
                        }
                    } else {
                        studentResponses()
                    }
                }
            }
        }
        // Pagination footer (< 1 >, disabled in Sprint 20 single-question)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE6E6E6)))
        Row(Modifier.fillMaxWidth().padding(vertical = 10.66.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.ic_chevron_left_16), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900.copy(alpha = 0.3f)))
            Box(
                Modifier.padding(horizontal = 5.33.dp).size(16.dp).clip(RoundedCornerShape(5.33.dp)).background(Neutral100),
                contentAlignment = Alignment.Center,
            ) { Text("1", color = Neutral900, fontSize = 9.33.sp, fontWeight = FontWeight.Medium) }
            Image(painterResource(Res.drawable.ic_chevron_right_16), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900.copy(alpha = 0.3f)))
        }
    }
}
