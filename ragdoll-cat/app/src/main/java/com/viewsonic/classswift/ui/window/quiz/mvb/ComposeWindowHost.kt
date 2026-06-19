package com.viewsonic.classswift.ui.window.quiz.mvb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Makes a [ComposeView] usable inside a window-framework overlay (which is not an Activity and
 * therefore provides none of the owners Compose expects).
 *
 * Provides:
 *  - a [LifecycleOwner] + [SavedStateRegistryOwner] (driven to RESUMED while shown), and
 *  - an explicit [Recomposer] (running on the main UI dispatcher) installed as the view's parent
 *    composition context, so the view never needs to resolve a "window recomposer" — avoiding the
 *    `Cannot locate windowRecomposer ... not attached to a window` crash if the view is measured
 *    before attach.
 *
 * Note: the window must also avoid measuring the ComposeView while detached (see
 * MvbQuizCollectionWindow.getCurrentSize, which returns a fixed size instead of measuring).
 */
class ComposeWindowHost : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    private val recomposeContext = AndroidUiDispatcher.Main
    private val recomposeScope = CoroutineScope(recomposeContext)
    private val recomposer = Recomposer(recomposeContext)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun attach(view: ComposeView, content: @Composable () -> Unit) {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        recomposeScope.launch { recomposer.runRecomposeAndApplyChanges() }

        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setParentCompositionContext(recomposer)
        view.setContent(content)
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        recomposer.cancel()
        recomposeScope.cancel()
    }
}
