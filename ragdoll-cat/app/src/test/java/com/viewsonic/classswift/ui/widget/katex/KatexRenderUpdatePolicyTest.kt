package com.viewsonic.classswift.ui.widget.katex

import org.junit.Assert.assertEquals
import org.junit.Test

class KatexRenderUpdatePolicyTest {
    private val policy = KatexRenderUpdatePolicy()

    @Test
    fun `style or color change only should apply style without reload`() {
        val current = snapshot(
            text = "\\(x^2 + y^2 = z^2\\)",
            styleCss = "normal",
            colorCss = "#0A0A0A"
        )

        val result = policy.decide(
            currentSnapshot = current,
            nextText = "\\(x^2 + y^2 = z^2\\)",
            nextStyleCss = "bold",
            nextColorCss = "#0A0A0A"
        )

        assertEquals(KatexRenderAction.APPLY_STYLE_ONLY, result)
    }

    @Test
    fun `text change should reload content`() {
        val current = snapshot(
            text = "\\(x^2 + y^2 = z^2\\)",
            styleCss = "normal",
            colorCss = "#0A0A0A"
        )

        val result = policy.decide(
            currentSnapshot = current,
            nextText = "\\(a^2 + b^2 = c^2\\)",
            nextStyleCss = "normal",
            nextColorCss = "#0A0A0A"
        )

        assertEquals(KatexRenderAction.RELOAD_CONTENT, result)
    }

    @Test
    fun `initial render should reload content`() {
        val result = policy.decide(
            currentSnapshot = null,
            nextText = "\\(x\\)",
            nextStyleCss = "normal",
            nextColorCss = "#0A0A0A"
        )

        assertEquals(KatexRenderAction.RELOAD_CONTENT, result)
    }

    @Test
    fun `no changes should result in no-op action`() {
        val current = snapshot(
            text = "\\(x^2 + y^2 = z^2\\)",
            styleCss = "normal",
            colorCss = "#0A0A0A"
        )

        val result = policy.decide(
            currentSnapshot = current,
            nextText = "\\(x^2 + y^2 = z^2\\)",
            nextStyleCss = "normal",
            nextColorCss = "#0A0A0A"
        )

        assertEquals(KatexRenderAction.NONE, result)
    }

    @Test
    fun `repeated style toggles should keep style only updates`() {
        val initial = snapshot(
            text = "\\(x^2\\)",
            styleCss = "normal",
            colorCss = "#0A0A0A"
        )

        val firstToggle = policy.decide(
            currentSnapshot = initial,
            nextText = "\\(x^2\\)",
            nextStyleCss = "bold",
            nextColorCss = "#FFFFFF"
        )
        assertEquals(KatexRenderAction.APPLY_STYLE_ONLY, firstToggle)

        val toggledSnapshot = initial.copy(styleCss = "bold", colorCss = "#FFFFFF")
        val secondToggle = policy.decide(
            currentSnapshot = toggledSnapshot,
            nextText = "\\(x^2\\)",
            nextStyleCss = "normal",
            nextColorCss = "#0A0A0A"
        )
        assertEquals(KatexRenderAction.APPLY_STYLE_ONLY, secondToggle)
    }

    private fun snapshot(
        text: String,
        styleCss: String,
        colorCss: String
    ): KatexRenderSnapshot {
        return KatexRenderSnapshot(
            text = text,
            styleCss = styleCss,
            colorCss = colorCss
        )
    }
}
