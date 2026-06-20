package com.viewsonic.classswift.ui.window.compose

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import com.viewsonic.classswift.databinding.WindowComposeHostBinding
import com.viewsonic.classswift.ui.window.quiz.mvb.ComposeWindowHost
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow

/**
 * Base for windows whose body is a CMP screen (rendered via [ComposeWindowHost] inside a single
 * ComposeView). Subclasses provide [tag], [size], optional [getCurrentSize] override, and the
 * [Content] composable — wired from their WindowModel so the shipped UI and the Designer Shell
 * preview are one codebase.
 *
 * Sizing: `size` feeds the OS window LayoutParams (use WRAP_CONTENT to wrap the ComposeView
 * exactly); [getCurrentSize] feeds centering math and must NOT measure the (detached) ComposeView
 * — return a fixed value.
 */
abstract class ComposeHostWindow(context: Context) : IWindow<WindowComposeHostBinding> {

    private val composeHost = ComposeWindowHost()

    override val binding: WindowComposeHostBinding =
        WindowComposeHostBinding.inflate(LayoutInflater.from(context))

    override fun onViewCreated() {
        composeHost.attach(binding.cvComposeHost) { Content() }
    }

    override fun onDestroy() {
        composeHost.destroy()
    }

    /** The CMP screen for this window. Feed real runtime data + wire callbacks here. */
    @Composable
    protected abstract fun Content()
}
