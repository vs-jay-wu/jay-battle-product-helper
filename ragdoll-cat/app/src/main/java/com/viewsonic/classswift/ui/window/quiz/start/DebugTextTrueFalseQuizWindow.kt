package com.viewsonic.classswift.ui.window.quiz.start

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.viewsonic.classswift.databinding.WindowMvbTextQuizBinding
import com.viewsonic.classswift.feature.servicescreens.ui.BarStyle
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizStartScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.feature.servicescreens.ui.QuizPanelState
import com.viewsonic.classswift.feature.servicescreens.ui.QuizResponder
import com.viewsonic.classswift.feature.servicescreens.ui.ResponderState
import com.viewsonic.classswift.feature.servicescreens.ui.ResultBar
import com.viewsonic.classswift.feature.servicescreens.ui.TextDiscloseOption
import com.viewsonic.classswift.ui.window.quiz.mvb.ComposeWindowHost
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * DEBUG ONLY — opened via the debug broadcast in `ClassSwiftService` (never in release). An isolated
 * copy of the [MvbTextTrueFalseStartWindow] hybrid (same `window_mvb_text_quiz` layout + same native
 * [MvbTextQuizKatexOverlay]) fed STATIC sample data — including a LaTeX question and a LaTeX AI reason
 * — so the rendering can be eyeballed without dispatching a real quiz. Tapping the minimize button
 * cycles QUIZZING → DISCLOSE → RESULT; close removes it.
 */
class DebugTextTrueFalseQuizWindow(val context: Context) : IWindow<WindowMvbTextQuizBinding> {

    private val composeHost = ComposeWindowHost()
    private val katexOverlay by lazy { MvbTextQuizKatexOverlay(binding) }

    // LaTeX question + a LaTeX AI reason to exercise both KatexView overlays (not verifiable headless).
    private val latexQuestion = "Is the following equation correct?  \$\\sqrt{16} = 4\$"
    private val sampleDisclose = listOf(
        TextDiscloseOption("True"),
        TextDiscloseOption(
            content = "False",
            reason = "16 has two square roots (\$\\pm 4\$); the principal root is \$\\sqrt{16} = 4\$, so the statement is true.",
            isSuggested = true,
            reasonIsLatex = true,
        ),
    )

    private val sampleResponders: List<QuizResponder> = List(14) { i ->
        val seat = "%02d".format(i + 1)
        when {
            i == 4 -> QuizResponder(seat, "Daniel Wu", ResponderState.ABSENT)
            i % 5 == 4 -> QuizResponder(seat, "Olivia Yang", ResponderState.NOT_SUBMITTED)
            else -> {
                val isTrue = i % 3 != 0
                QuizResponder(seat, "Student $seat", ResponderState.ANSWERED, answer = if (isTrue) "T" else "F", correct = isTrue)
            }
        }
    }
    private val sampleBars = listOf(
        ResultBar("T", 8, 13, isCorrect = true, BarStyle.CORRECT),
        ResultBar("F", 3, 13, isCorrect = false, BarStyle.INCORRECT),
        ResultBar("Not submitted", 2, 13, isCorrect = false, BarStyle.NEUTRAL),
    )

    private val state = MutableStateFlow(QuizPanelState.QUIZZING)

    override var tag: WindowTag = WindowTag.MVB_TEXT_TRUE_FALSE_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(869f.dpToPx().toInt(), 496f.dpToPx().toInt())
    override val binding: WindowMvbTextQuizBinding = WindowMvbTextQuizBinding.inflate(LayoutInflater.from(context))
    override fun getCurrentSize(): SizeInPixels = size

    override fun onViewCreated() {
        composeHost.attach(binding.cvBody) { Content() }
    }

    @Composable
    private fun Content() {
        val s by state.collectAsState()
        Box(Modifier.fillMaxSize()) {
            MvbQuizStartScreen(
                type = MvbQuizType.TEXT_TRUE_FALSE,
                state = s,
                joined = 11,
                capacity = 13,
                stopwatch = "01:23",
                options = listOf("True", "False"),
                responders = sampleResponders,
                resultBars = sampleBars,
                textDiscloseOptions = sampleDisclose,
                latexHeight = { key -> katexOverlay.heights[key] ?: 0.dp },
                onLatexBounds = { key, text, pos, sizePx -> katexOverlay.position(key, text, pos, sizePx.width, fixedHeightPx = null) },
                onLatexHidden = { key -> katexOverlay.hide(key) },
                screenshot = { m -> QuestionContent(m) },
                onClose = { CSWindowManager.removeWindow(tag) },
                // Debug: minimize cycles the panel state so all three can be eyeballed.
                onMinimize = {
                    state.value = when (state.value) {
                        QuizPanelState.QUIZZING -> QuizPanelState.DISCLOSE
                        QuizPanelState.DISCLOSE -> QuizPanelState.RESULT
                        QuizPanelState.RESULT -> QuizPanelState.QUIZZING
                    }
                },
                onEndAndReview = { state.value = QuizPanelState.DISCLOSE },
                onPublishDisclose = { state.value = QuizPanelState.RESULT },
            )
        }
    }

    @Composable
    private fun QuestionContent(modifier: Modifier) {
        Box(modifier.onGloballyPositioned { katexOverlay.position("question", latexQuestion, it.positionInRoot(), it.size.width, fixedHeightPx = it.size.height) })
    }

    override fun onDestroy() {
        composeHost.destroy()
        katexOverlay.release()
    }
}
