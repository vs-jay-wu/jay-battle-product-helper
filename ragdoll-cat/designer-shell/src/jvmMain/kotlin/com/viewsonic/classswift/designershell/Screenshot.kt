@file:OptIn(ExperimentalComposeUiApi::class)

package com.viewsonic.classswift.designershell

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.viewsonic.classswift.feature.servicescreens.ui.BarStyle
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizStartScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.feature.servicescreens.ui.QuizPanelState
import com.viewsonic.classswift.feature.servicescreens.ui.ResultBar
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
    println("SHOTS_DONE -> $dir")
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
