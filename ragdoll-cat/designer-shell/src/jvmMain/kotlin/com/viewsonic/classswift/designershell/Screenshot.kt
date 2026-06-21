@file:OptIn(ExperimentalComposeUiApi::class)

package com.viewsonic.classswift.designershell

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizStartScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.feature.servicescreens.ui.QuizPanelState
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
