package com.viewsonic.classswift.ui.helper

import android.view.View
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WindowControlButtonsUiHelper {

    /**
     * Sets up the three standard window control buttons (close, minimize, bring-to-front)
     * and adjusts minimize/bring-to-front visibility based on the MVB bound state.
     *
     * When MVB is bound:   ivMinimizeWindow=VISIBLE,  ivToolbarBringToFront=GONE
     * When MVB is unbound: ivMinimizeWindow=GONE,     ivToolbarBringToFront keeps its default visibility
     *
     * @param onCloseClick     Window-specific action to perform when the close button is tapped.
     * @param onAfterMinimize  Optional extra action to run after the window is minimized
     *                         (e.g. notifyMissionMinimizedIfNeeded). Null by default.
     */
    fun setup(
        ivClose: View,
        ivMinimizeWindow: View,
        ivToolbarBringToFront: View,
        windowTag: WindowTag,
        isMvbBound: Boolean,
        csWindowManager: CSWindowManager,
        coroutineScope: CoroutineScope,
        onCloseClick: () -> Unit,
        onAfterMinimize: (() -> Unit)? = null
    ) {
        ivClose.setOnClickListener { onCloseClick() }

        ivMinimizeWindow.setOnClickListener {
            csWindowManager.minimizeWindow(windowTag)
            onAfterMinimize?.invoke()
        }

        ivToolbarBringToFront.setOnClickListener {
            coroutineScope.launch(Dispatchers.Main) {
                csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
            }
        }

        if (isMvbBound) {
            ivMinimizeWindow.visibility = View.VISIBLE
            ivToolbarBringToFront.visibility = View.GONE
        } else {
            ivMinimizeWindow.visibility = View.GONE
        }
    }

    fun setupForMvbView(
        ivClose: View,
        ivMinimizeWindow: View,
        windowTag: WindowTag,
        csWindowManager: CSWindowManager,
        onCloseClick: () -> Unit,
        onAfterMinimize: (() -> Unit)? = null
    ) {
        ivClose.setOnClickListener { onCloseClick() }

        ivMinimizeWindow.setOnClickListener {
            csWindowManager.minimizeWindow(windowTag)
            onAfterMinimize?.invoke()
        }
    }
}
