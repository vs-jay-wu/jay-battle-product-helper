package com.viewsonic.classswift.ui.window.quiz.start

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.viewsonic.classswift.R
import com.viewsonic.classswift.coordinator.UploadFileHandler
import com.viewsonic.classswift.data.enum.SketchAnswerStatus
import com.viewsonic.classswift.data.info.SketchStudentCardInfo
import com.viewsonic.classswift.databinding.WindowMvbSketchResponseStartBinding
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.test.SnapshotHostActivity
import com.viewsonic.classswift.test.TestApplication
import com.viewsonic.classswift.ui.widgetmodel.quiz.SketchReviewWidgetModel
import com.viewsonic.classswift.ui.window.adapter.MvbQuizAnsweringAdapter
import com.viewsonic.classswift.ui.window.decoration.StudentAnswerResultItemDecoration
import com.viewsonic.classswift.utils.extension.toQuizAnsweringInfo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot tests for the sketch-response answering (quizzing) window.
 * Follows the infra pattern from RoborazziInfraSmokeTest:
 *   - AndroidJUnit4 + @Config(sdk = [35]) + @GraphicsMode(NATIVE)
 *   - Robolectric.buildActivity(SnapshotHostActivity) to host the inflated view
 *   - captureRoboImage(filePath = "src/test/snapshots/...") with RoborazziOptions(Screenshot)
 *
 * Baseline PNGs are committed under `app/src/test/snapshots/`.
 * Layout is inflated directly (binding) instead of via the production Window
 * class to avoid Koin/lifecycle setup; renderState equivalents are inlined.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = TestApplication::class, sdk = [35])
class MvbSketchResponseStartWindowSnapshotTest {

    @Before
    fun startKoinForWidgetInflation() {
        // window_mvb_sketch_response_start.xml inflates a hidden SketchReviewWidget
        // (result panel, visibility=gone) — the only KoinComponent in this layout. It
        // resolves SketchReviewWidgetModel on construction, so inflation needs a Koin
        // context. Provide a self-contained one here instead of depending on whether
        // another test in the same JVM fork happened to leave Koin started (the cause
        // of this test's prior intermittent InflateException).
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val uploadFileHandler = mockk<UploadFileHandler>(relaxed = true)
        val classroomManager = mockk<ClassroomManager>(relaxed = true)
        every { classroomManager.classroomDataStateFlow } returns
            MutableStateFlow(ClassroomManager.ClassroomDataState())
        startKoin {
            modules(
                module {
                    single { uploadFileHandler }
                    single { classroomManager }
                    factory { SketchReviewWidgetModel(appContext, get(), get()) }
                },
            )
        }
    }

    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    @Test
    fun initial_empty_state() {
        val view = renderAnsweringWindow(
            students = buildStudents(total = 30, submittedCount = 0, absentCount = 0),
            sketchCount = 1,
            elapsedSeconds = 1,
            submittedCount = 0,
            totalCount = 30,
        )
        view.captureRoboImage(
            filePath = "src/test/snapshots/MvbSketchResponseStartWindowSnapshotTest_initial_empty_state.png",
            roborazziOptions = RoborazziOptions(captureType = RoborazziOptions.CaptureType.Screenshot()),
        )
    }

    @Test
    fun mixed_states() {
        val view = renderAnsweringWindow(
            students = buildStudents(total = 30, submittedCount = 10, absentCount = 3),
            sketchCount = 1,
            elapsedSeconds = 42,
            submittedCount = 10,
            totalCount = 30,
        )
        view.captureRoboImage(
            filePath = "src/test/snapshots/MvbSketchResponseStartWindowSnapshotTest_mixed_states.png",
            roborazziOptions = RoborazziOptions(captureType = RoborazziOptions.CaptureType.Screenshot()),
        )
    }

    @Test
    fun all_submitted() {
        val view = renderAnsweringWindow(
            students = buildStudents(total = 30, submittedCount = 30, absentCount = 0),
            sketchCount = 1,
            elapsedSeconds = 125,
            submittedCount = 30,
            totalCount = 30,
        )
        view.captureRoboImage(
            filePath = "src/test/snapshots/MvbSketchResponseStartWindowSnapshotTest_all_submitted.png",
            roborazziOptions = RoborazziOptions(captureType = RoborazziOptions.CaptureType.Screenshot()),
        )
    }

    private fun renderAnsweringWindow(
        students: List<SketchStudentCardInfo>,
        sketchCount: Int,
        elapsedSeconds: Int,
        submittedCount: Int,
        totalCount: Int,
    ): View {
        val activity = Robolectric.buildActivity(SnapshotHostActivity::class.java)
            .create()
            .start()
            .resume()
            .get()
        val binding = WindowMvbSketchResponseStartBinding.inflate(activity.layoutInflater)

        // Sketch chrome on the shared quizzing panel.
        with(binding.panelQuizzing) {
            ivQuizTypeIcon.setImageResource(R.drawable.ic_pen_curve_line)
            tvQuizTypeSubtitle.visibility = View.GONE
            llQuizzingOptionsArea.visibility = View.GONE
            llDiscloseSelectorArea.visibility = View.GONE
            llResultOptionsArea.visibility = View.GONE
            cslbDisclosePublish.visibility = View.GONE
            rvResultStudentList.visibility = View.GONE

            tvQuizTypeLabel.text = activity.resources.getQuantityString(
                R.plurals.mvb_sketch_response_in_progress,
                sketchCount,
                sketchCount,
            )
            tvStopwatch.text = formatTimer(elapsedSeconds)
            buttonEndAndReview.setEnableText(activity.getString(R.string.mvb_sketch_response_collect_and_mark))
            tvResponseCount.text = activity.getString(
                R.string.mvb_sketch_response_counter_format,
                submittedCount,
                totalCount,
            )

            csScreenshotImage.apply {
                setMaskVisibility(false)
                setProgressbarVisibility(false)
                setCaptureAgainButtonVisibility(false)
            }

            val adapter = MvbQuizAnsweringAdapter(itemClickCallback = {})
            rvStudentList.layoutManager = GridLayoutManager(activity, GRID_SPAN)
            rvStudentList.addItemDecoration(
                StudentAnswerResultItemDecoration(
                    GRID_SPAN,
                    activity.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400),
                    activity.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400),
                ),
            )
            rvStudentList.adapter = adapter
            adapter.submitList(students.map { it.toQuizAnsweringInfo() })
        }

        activity.setContentView(binding.root)

        // Measure + lay out the inflated tree at the qualifier device size.
        val widthSpec = View.MeasureSpec.makeMeasureSpec(WINDOW_WIDTH_PX, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(WINDOW_HEIGHT_PX, View.MeasureSpec.EXACTLY)
        binding.root.measure(widthSpec, heightSpec)
        binding.root.layout(0, 0, WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX)
        return binding.root
    }

    private fun buildStudents(
        total: Int,
        submittedCount: Int,
        absentCount: Int,
    ): List<SketchStudentCardInfo> {
        require(submittedCount + absentCount <= total)
        return (0 until total).map { index ->
            val status = when {
                index < submittedCount -> SketchAnswerStatus.SUBMITTED
                index < submittedCount + absentCount -> SketchAnswerStatus.ABSENT
                else -> SketchAnswerStatus.NOT_SUBMITTED
            }
            SketchStudentCardInfo(
                studentId = "student-$index",
                serialNumber = index + 1,
                displaySeatNumber = (index + 1).toString(),
                displayName = "Student ${index + 1}",
                status = status,
            )
        }
    }

    private fun formatTimer(elapsedSeconds: Int): String {
        val safe = elapsedSeconds.coerceAtLeast(0)
        val minutes = safe / SECONDS_PER_MINUTE
        val seconds = safe % SECONDS_PER_MINUTE
        return "%02d:%02d".format(minutes, seconds)
    }

    companion object {
        private const val GRID_SPAN = 4
        private const val SECONDS_PER_MINUTE = 60
        // Figma 1280×720 → ÷1.5 → ~853×480dp, xxhdpi → ×3 px ≈ 2560×1440.
        // We render at 1280×720 px (1×) which is enough resolution to detect
        // layout regressions while keeping snapshot file size manageable.
        private const val WINDOW_WIDTH_PX = 1280
        private const val WINDOW_HEIGHT_PX = 720
    }
}
