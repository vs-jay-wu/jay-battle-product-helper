package com.viewsonic.classswift.infra

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Smoke test that proves the Roborazzi infra is wired correctly. Renders a
 * trivial TextView attached to a Robolectric Activity, captures it, and
 * writes the baseline (`recordRoborazziStagDebug`) or compares against the
 * baseline (`verifyRoborazziStagDebug`).
 *
 * Use this file as the reference pattern for new Mvb* UI snapshot tests
 * (see .claude/rules/test-with-feature.md Type B). Key points:
 *   - `@RunWith(AndroidJUnit4::class)` + `@Config(sdk = [35])` matches project targetSdk
 *   - View MUST be attached to an Activity (Roborazzi enforces this) — use
 *     Robolectric.buildActivity(...) instead of plain Context.
 *   - `captureRoboImage()` without args auto-derives baseline filename from
 *     the test method's FQN.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)  // Required so Robolectric actually renders text glyphs (otherwise TextView draws as an empty box → text changes don't affect pixels → snapshot diffs fail to detect regressions).
class RoborazziInfraSmokeTest {

    @Test
    fun smoke() {
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val activity = controller.get()
        val view = TextView(activity).apply {
            text = "Hello Roborazzi"
            textSize = 18f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
        }
        activity.setContentView(
            view,
            ViewGroup.LayoutParams(400, 200)
        )
        // Robolectric activity controller doesn't trigger a window-layout pass,
        // so we measure + layout explicitly before Roborazzi calls drawToBitmap().
        view.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 400, 200)
        view.captureRoboImage(
            filePath = "src/test/snapshots/RoborazziInfraSmokeTest_smoke.png",
            // Default options include a "dump" mode that requires a ComposeView / hierarchy,
            // which a plain TextView doesn't satisfy → set captureType = Screenshot only.
            roborazziOptions = RoborazziOptions(
                captureType = RoborazziOptions.CaptureType.Screenshot()
            )
        )
    }
}
