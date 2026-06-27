package com.viewsonic.classswift.uimanager

import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import timber.log.Timber

class QuizUiManager(
    private val csWindowManager: CSWindowManager
) {
    // region Used to keep track of opened Quiz windows and related operations
    private val openedQuizWindowList: MutableList<WindowTag> = mutableListOf()

    @Synchronized
    fun addOpenedQuizWindowTag(windowTag: WindowTag) {
        Timber.d("add $windowTag")
        openedQuizWindowList.add(windowTag)
    }

    @Synchronized
    fun removeOpenedQuizWindowTag(windowTag: WindowTag) {
        if (openedQuizWindowList.contains(windowTag)) {
            Timber.d("remove $windowTag")
            openedQuizWindowList.remove(windowTag)
        }
    }

    fun getCurrentOpenedQuizWindowTag(): WindowTag {
        if (openedQuizWindowList.isEmpty()) {
            return WindowTag.NONE
        }
        val currentQuizWindowTag = openedQuizWindowList.last()
        return if (csWindowManager.isWindowExisted(currentQuizWindowTag)) {
            currentQuizWindowTag
        } else {
            WindowTag.NONE
        }
    }

    fun isQuizEditWindowExisted(): Boolean = WindowTag.getImageQuizEditWindowTagList().contains(getCurrentOpenedQuizWindowTag())
}