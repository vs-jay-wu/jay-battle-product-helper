package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel.UiState
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel.UploadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure data-class invariants for `MvbSketchResponseEditWindowModel`.
 *
 * State-machine tests (upload, dispatch, retry) live in `MvbSketchResponseEditWindowModelMockkTest`.
 */
class MvbSketchResponseEditWindowModelTest {

    @Test
    fun `UiState defaults are Idle and not in-flight`() {
        val state = UiState()
        assertEquals(UploadState.Idle, state.uploadState)
        assertFalse(state.isDispatchInFlight)
    }

    @Test
    fun `UploadState Success carries previewImageUrl`() {
        val state = UploadState.Success("https://cdn/path.png")
        assertEquals("https://cdn/path.png", state.previewImageUrl)
    }

    @Test
    fun `UploadState Loading carries the source URI`() {
        val state = UploadState.Loading("content://capture/1")
        assertEquals("content://capture/1", state.uri)
    }

    @Test
    fun `UploadState Failed exposes nullable cause`() {
        val withCause = UploadState.Failed(IllegalStateException("boom"))
        val withoutCause = UploadState.Failed()
        assertNotNull(withCause.cause)
        assertTrue(withoutCause.cause == null)
    }

    @Test
    fun `UiState copy preserves uploadState while toggling isDispatchInFlight`() {
        val base = UiState(uploadState = UploadState.Success("u"))
        val inFlight = base.copy(isDispatchInFlight = true)
        val idle = inFlight.copy(isDispatchInFlight = false)
        assertEquals(UploadState.Success("u"), inFlight.uploadState)
        assertTrue(inFlight.isDispatchInFlight)
        assertEquals(UploadState.Success("u"), idle.uploadState)
        assertFalse(idle.isDispatchInFlight)
    }
}
