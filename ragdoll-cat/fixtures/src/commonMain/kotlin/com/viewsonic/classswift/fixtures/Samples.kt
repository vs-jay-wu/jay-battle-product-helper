package com.viewsonic.classswift.fixtures

import com.viewsonic.classswift.feature.playground.ui.PlaygroundUiState

/**
 * Single source of mock data for previews / screenshot tests / (future) demo flavor.
 *
 * Phase 0: one sample for the playground screen. Phase 1 adds loading / empty / error /
 * populated variants per real screen.
 */
object Samples {
    val playgroundPopulated: PlaygroundUiState =
        PlaygroundUiState(title = "Designer Shell scaffold ✅ (from :fixtures)")
}
