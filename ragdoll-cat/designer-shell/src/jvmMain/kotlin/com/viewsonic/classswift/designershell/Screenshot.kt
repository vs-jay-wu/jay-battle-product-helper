@file:OptIn(ExperimentalComposeUiApi::class)

package com.viewsonic.classswift.designershell

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.feature.servicescreens.ui.BarStyle
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizStartScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.feature.servicescreens.ui.MvbSketchResponseScreen
import com.viewsonic.classswift.feature.servicescreens.ui.SketchPanelState
import com.viewsonic.classswift.feature.servicescreens.ui.QuizPanelState
import com.viewsonic.classswift.feature.servicescreens.ui.QuizResponder
import com.viewsonic.classswift.feature.servicescreens.ui.ResponderState
import com.viewsonic.classswift.feature.servicescreens.ui.ResultBar
import com.viewsonic.classswift.feature.servicescreens.ui.TextDiscloseOption
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Headless render of quiz-start states to PNGs (dev verification without the tablet/desktop window).
 *  Run: ./gradlew :designer-shell:screenshotQuiz -Dshot.dir=/abs/dir */
fun main() {
    val dir = System.getProperty("shot.dir") ?: "/tmp"
    File(dir).mkdirs()
    render("$dir/quiz_tf_result.png") { MvbQuizStartScreen(type = MvbQuizType.TRUE_FALSE, state = QuizPanelState.RESULT) }
    render("$dir/quiz_tf_quizzing.png") { MvbQuizStartScreen(type = MvbQuizType.TRUE_FALSE, state = QuizPanelState.QUIZZING) }
    render("$dir/quiz_tf_disclose.png") { MvbQuizStartScreen(type = MvbQuizType.TRUE_FALSE, state = QuizPanelState.DISCLOSE) }
    val pollBars = listOf(
        ResultBar("A", 8, 19, false, BarStyle.CORRECT),
        ResultBar("B", 6, 19, false, BarStyle.CORRECT),
        ResultBar("C", 3, 19, false, BarStyle.CORRECT),
        ResultBar("D", 0, 19, false, BarStyle.CORRECT),
        ResultBar("Not submitted", 2, 19, false, BarStyle.NEUTRAL),
    )
    render("$dir/quiz_poll_result.png") {
        MvbQuizStartScreen(type = MvbQuizType.POLL, state = QuizPanelState.RESULT, pollMode = true, options = listOf("A", "B", "C", "D"), resultBars = pollBars)
    }
    // Short-answer result → submission overview (Submitted / Not submitted, "Correct rate" legend).
    val saNames = listOf("Brandon Wang", "Emily Chen", "Marcus Lee", "Sophia Liu", "Daniel Wu")
    val saResponders = listOf(
        "I think the answer is photosynthesis because plants convert sunlight.",
        "Mitochondria is the powerhouse of the cell.",
        "Because the water cycle evaporates and condenses.",
        "42",
    ).mapIndexed { i, ans -> QuizResponder("%02d".format(i + 1), saNames[i], ResponderState.ANSWERED, answer = ans) } +
        QuizResponder("05", saNames[4], ResponderState.NOT_SUBMITTED)
    val saBars = listOf(
        ResultBar("Submitted", 4, 5, false, BarStyle.CORRECT),
        ResultBar("Not submitted", 1, 5, false, BarStyle.NEUTRAL),
    )
    render("$dir/quiz_sa_result.png") {
        MvbQuizStartScreen(
            type = MvbQuizType.SHORT_ANSWER, state = QuizPanelState.RESULT,
            submissionMode = true, answerPopup = true, options = emptyList(),
            joined = 4, capacity = 5, responders = saResponders, resultBars = saBars,
        )
    }
    // Audio result → submission overview with a check-icon Submitted chip + "Submitted" legend;
    // answered cells show a play/pause control + time (Student-responses tab).
    // Mixed cell states: 0 playing (pause icon), 1-3 paused/init (play icon), 4 loading (spinner), 6 not submitted.
    val audioResponders = saNames.take(5).mapIndexed { i, n ->
        QuizResponder("%02d".format(i + 1), n, ResponderState.ANSWERED, correct = true, audioTime = "0:1$i", audioPlaying = i == 0, audioLoading = i == 4)
    } + QuizResponder("06", "Olivia Yang", ResponderState.NOT_SUBMITTED)
    val audioBars = listOf(
        ResultBar("Submitted", 5, 6, false, BarStyle.CORRECT),
        ResultBar("Not submitted", 1, 6, false, BarStyle.NEUTRAL),
    )
    render("$dir/quiz_audio_result.png") {
        MvbQuizStartScreen(
            type = MvbQuizType.AUDIO, state = QuizPanelState.RESULT, audioMode = true, options = emptyList(),
            joined = 5, capacity = 6, responders = audioResponders, resultBars = audioBars,
        )
    }
    // Student-responses tab → the audio player cells (play/pause + time, spinner while loading).
    render("$dir/quiz_audio_cells.png") {
        MvbQuizStartScreen(
            type = MvbQuizType.AUDIO, state = QuizPanelState.RESULT, audioMode = true, options = emptyList(),
            startOnOverview = false, joined = 5, capacity = 6, responders = audioResponders, resultBars = audioBars,
        )
    }
    // Text True/False (step 1): neutral100 question box holding plain Compose text (slot), full-width
    // vertical True/False chips in quizzing; result reuses the TF bars (T/F/Not submitted).
    val textTfQuestion = "Which statement below is sometimes used by historians to refer to the time period of the Tang Dynasty?"
    val textTfSlot: @Composable (androidx.compose.ui.Modifier) -> Unit = { m ->
        Text(textTfQuestion, color = Color(0xFF333333), fontSize = 10.67.sp, modifier = m)
    }
    render("$dir/quiz_text_tf_quizzing.png") {
        MvbQuizStartScreen(
            type = MvbQuizType.TEXT_TRUE_FALSE, state = QuizPanelState.QUIZZING,
            options = listOf("True", "False"), screenshot = textTfSlot,
        )
    }
    render("$dir/quiz_text_tf_result.png") {
        MvbQuizStartScreen(
            type = MvbQuizType.TEXT_TRUE_FALSE, state = QuizPanelState.RESULT, screenshot = textTfSlot,
        )
    }
    // Text TF disclose (step 2): vertical option rows + "Suggested answer" reveal button. Default
    // (nothing picked / not revealed) and the revealed+selected state (pill + reason + "Applied").
    val textTfDiscloseOptions = listOf(
        TextDiscloseOption("True"),
        TextDiscloseOption(
            "False",
            reason = "The Tang Dynasty is conventionally described as a golden age, so the statement is false.",
            isSuggested = true,
        ),
    )
    render("$dir/quiz_text_tf_disclose.png") {
        MvbQuizStartScreen(
            type = MvbQuizType.TEXT_TRUE_FALSE, state = QuizPanelState.DISCLOSE,
            textDiscloseOptions = textTfDiscloseOptions, screenshot = textTfSlot,
        )
    }
    render("$dir/quiz_text_tf_disclose_revealed.png") {
        MvbQuizStartScreen(
            type = MvbQuizType.TEXT_TRUE_FALSE, state = QuizPanelState.DISCLOSE,
            textDiscloseOptions = textTfDiscloseOptions, screenshot = textTfSlot,
            startDiscloseRevealed = true, startDiscloseSelected = 1,
        )
    }
    // Text Short Answer: neutral100 text question box (slot) + submission overview + answer popup —
    // like the image short answer but the question is text/LaTeX, not a screenshot. No options/disclose.
    val textSaQuestion = "Explain in your own words why the sky appears blue during the day."
    val textSaSlot: @Composable (androidx.compose.ui.Modifier) -> Unit = { m ->
        Text(textSaQuestion, color = Color(0xFF333333), fontSize = 10.67.sp, modifier = m)
    }
    render("$dir/quiz_text_sa_quizzing.png") {
        MvbQuizStartScreen(
            type = MvbQuizType.TEXT_SHORT_ANSWER, state = QuizPanelState.QUIZZING, options = emptyList(),
            joined = 4, capacity = 5, responders = saResponders, screenshot = textSaSlot,
        )
    }
    render("$dir/quiz_text_sa_result.png") {
        MvbQuizStartScreen(
            type = MvbQuizType.TEXT_SHORT_ANSWER, state = QuizPanelState.RESULT,
            submissionMode = true, answerPopup = true, options = emptyList(),
            joined = 4, capacity = 5, responders = saResponders, resultBars = saBars, screenshot = textSaSlot,
        )
    }
    // All-not-submitted TF result → full gray crosshatch pie (apples-to-apples vs the old build).
    render("$dir/quiz_tf_result_allns.png") {
        MvbQuizStartScreen(
            type = MvbQuizType.TRUE_FALSE, state = QuizPanelState.RESULT, joined = 0, capacity = 1,
            resultBars = listOf(
                ResultBar("T", 0, 1, true, BarStyle.CORRECT),
                ResultBar("F", 0, 1, false, BarStyle.INCORRECT),
                ResultBar("Not submitted", 1, 1, false, BarStyle.NEUTRAL),
            ),
        )
    }
    // Sketch Response (step 1): ANSWERING (monitoring grid + "Collect all and mark") and RESULT
    // (question preview + Submitted/Not-submitted bars + Overview pie). Student-responses tab = step 2.
    val sketchResponders = List(14) { i ->
        val seat = "%02d".format(i + 1)
        when {
            i == 4 -> QuizResponder(seat, "Daniel Wu", ResponderState.ABSENT)
            i % 5 == 4 -> QuizResponder(seat, "Olivia Yang", ResponderState.NOT_SUBMITTED)
            else -> QuizResponder(seat, "Student $seat", ResponderState.ANSWERED, correct = true)
        }
    }
    render("$dir/quiz_sketch_answering.png") {
        MvbSketchResponseScreen(
            state = SketchPanelState.ANSWERING, inProgressLabel = "1 sketch in progress",
            stopwatch = "01:23", responders = sketchResponders,
        )
    }
    renderTall("$dir/quiz_sketch_result.png") {
        MvbSketchResponseScreen(
            state = SketchPanelState.RESULT, questionTitle = "1. Sketch response",
            submitted = 11, notSubmitted = 2, responders = sketchResponders,
        )
    }
    // Student-responses tab → status cards (Click to view / Handed back / Not submitted / Absent).
    renderTall("$dir/quiz_sketch_result_cards.png") {
        MvbSketchResponseScreen(
            state = SketchPanelState.RESULT, questionTitle = "1. Sketch response",
            submitted = 11, notSubmitted = 2, responders = sketchResponders, startOnOverview = false,
        )
    }
    println("SHOTS_DONE -> $dir")
}

/** Sketch result shell is 853×522.67dp (taller than the 480dp quiz shell) + 8dp shadow padding. */
private fun renderTall(path: String, content: @Composable () -> Unit) {
    val scene = ImageComposeScene(width = 1738, height = 1078, density = Density(2f), content = content)
    try {
        val data = scene.render().encodeToData(EncodedImageFormat.PNG) ?: error("PNG encode failed")
        File(path).writeBytes(data.bytes)
    } finally {
        scene.close()
    }
}

private fun render(path: String, content: @Composable () -> Unit) {
    // 869×496 dp shell (incl. 8dp shadow padding) at density 2.
    val scene = ImageComposeScene(width = 1738, height = 992, density = Density(2f), content = content)
    try {
        val data = scene.render().encodeToData(EncodedImageFormat.PNG) ?: error("PNG encode failed")
        File(path).writeBytes(data.bytes)
    } finally {
        scene.close()
    }
}
