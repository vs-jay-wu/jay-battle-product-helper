package com.viewsonic.classswift.feature.playground.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.viewsonic.classswift.core.designsystem.AppTheme
import com.viewsonic.classswift.core.ui.designNode

/**
 * Phase 0 placeholder screen — proves the platform-neutral UI chain
 * (:feature:ui -> :core:ui + :core:designsystem) compiles for both the JVM
 * (desktop preview) and Android targets. Real screens replace this in Phase 1.
 *
 * Pure function of state: (UiState) -> UI. No Android-only APIs, no ViewModel,
 * no NavController — so it renders in the desktop canvas and ships via ComposeView.
 */
data class PlaygroundUiState(
    val title: String = "Phase 0 scaffold",
)

@Composable
fun PlaygroundScreen(state: PlaygroundUiState, modifier: Modifier = Modifier) {
    AppTheme {
        Surface {
            Column(modifier = modifier.padding(24.dp)) {
                Text(
                    text = state.title,
                    modifier = Modifier.designNode("playground_title"),
                )
            }
        }
    }
}
