package com.viewsonic.classswift.ui.widget.toolbar.factory

import android.content.Context
import androidx.viewbinding.ViewBinding
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.toolbar.CSSubordinateMenuItem
import com.viewsonic.classswift.ui.widget.toolbar.CSToolbarIconButton
import com.viewsonic.classswift.ui.window.ClassManagementMenuWindow
import com.viewsonic.classswift.ui.window.ComingSoonPromptWindow
import com.viewsonic.classswift.ui.window.MyClassWindow
import com.viewsonic.classswift.ui.window.QuizCollectionWindow
import com.viewsonic.classswift.ui.window.QuizMenuWindow
import com.viewsonic.classswift.ui.window.SettingMenuWindow
import com.viewsonic.classswift.ui.window.ToolbarWindow
import com.viewsonic.classswift.ui.window.ToolsMenuWindow
import com.viewsonic.classswift.ui.window.UpgradePromptWindow
import com.viewsonic.classswift.ui.window.quiz.mvb.MvbQuizCollectionWindow
import com.viewsonic.classswift.ui.window.task.PushRespondWindow
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

object CSToolbarIconButtonDataFactory {
    private val androidContext: Context by inject(Context::class.java)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider by inject(
        MyViewBoardConnectionStateProvider::class.java,
    )
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private fun resolvedQuizCollectionWindowTag(): WindowTag =
        if (myViewBoardConnectionStateProvider.isBound()) {
            WindowTag.WINDOW_MVB_QUIZ_COLLECTION
        } else {
            WindowTag.WINDOW_QUIZ_COLLECTION
        }

    private fun newQuizCollectionWindowInstance(): IWindow<ViewBinding> =
        if (myViewBoardConnectionStateProvider.isBound()) {
            get(MvbQuizCollectionWindow::class.java)
        } else {
            get(QuizCollectionWindow::class.java)
        }

    fun createMap(
        isPremiumUser: Boolean,
        participationState: ToolbarManager.ParticipationState
    ): Map<ToolbarWindow.CSToolbarIconButtonType, CSToolbarIconButton.CSToolbarIconButtonData> {
        val resultMap: MutableMap<ToolbarWindow.CSToolbarIconButtonType, CSToolbarIconButton.CSToolbarIconButtonData> = mutableMapOf()
        ToolbarWindow.CSToolbarIconButtonType.entries.forEach { type ->
            resultMap[type] = CSToolbarIconButton.CSToolbarIconButtonData(
                isVisible = false
            )
        }
        Timber.d("[createMap]: isPremiumUser = $isPremiumUser")
        Timber.d("[createMap]: ParticipationState = $participationState")

        when (participationState) {
            ToolbarManager.ParticipationState.NOT_JOINED -> {
                resultMap[ToolbarWindow.CSToolbarIconButtonType.CLASS_LIST] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = WindowTag.CS_SELECT_MY_CLASS
                        override fun isSubWindow(): Boolean = false
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.CS_SELECT_MY_CLASS
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> {
                            val window: MyClassWindow = get(MyClassWindow::class.java)
                            return window
                        }

                        override fun updateWindowAsNeeded() {
                            coroutineScope.launch(Dispatchers.Main) {
                                csWindowManager.bringWindowToTop(WindowTag.CS_SELECT_MY_CLASS)
                            }
                        }
                    }
                )
                resultMap[ToolbarWindow.CSToolbarIconButtonType.PRESET] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = resolvedQuizCollectionWindowTag()
                        override fun isSubWindow(): Boolean = false
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.WINDOW_QUIZ_COLLECTION,
                            WindowTag.WINDOW_MVB_QUIZ_COLLECTION,
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> = newQuizCollectionWindowInstance()

                        override fun updateWindowAsNeeded() {
                            val activeTag = resolvedQuizCollectionWindowTag()
                            if (activeTag == WindowTag.WINDOW_QUIZ_COLLECTION) {
                                csWindowManager.getWindow(WindowTag.WINDOW_QUIZ_COLLECTION)?.let { iWindowContainer ->
                                    val window: QuizCollectionWindow = iWindowContainer.customWindow as QuizCollectionWindow
                                    window.refreshStartQuizButtonState()
                                }
                            }
                            coroutineScope.launch(Dispatchers.Main) {
                                csWindowManager.bringWindowToTop(activeTag)
                            }
                        }
                    }
                )
                resultMap[ToolbarWindow.CSToolbarIconButtonType.SETTINGS] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = WindowTag.SETTING_MENU
                        override fun isSubWindow(): Boolean = true
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.SETTING_MENU,
                            WindowTag.SETTING_WINDOW
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> {
                            val window: SettingMenuWindow = get(SettingMenuWindow::class.java)
                            window.setParticipationState(participationState)
                            return window
                        }
                    }
                )
                if (isPremiumUser) {
                    resultMap[ToolbarWindow.CSToolbarIconButtonType.QUIZ] = CSToolbarIconButton.CSToolbarIconButtonData(
                        isVisible = true,
                        buttonState = CSToolbarIconButton.ButtonState.DISABLED,
                        callback = object : CSToolbarIconButton.Callback {
                            override fun getBoundWindowTag(): WindowTag = WindowTag.COMING_SOON_PROMPT
                            override fun isSubWindow(): Boolean = true
                            override fun getObservedWindowTagList(): List<WindowTag> = emptyList()
                            override fun getNewWindowInstance(): IWindow<ViewBinding> {
                                val window: ComingSoonPromptWindow = get(ComingSoonPromptWindow::class.java)
                                window.setTitle(androidContext.getString(R.string.preset_quiz_title))
                                return window
                            }

                            override fun updateWindowAsNeeded() {
                                csWindowManager.getWindow(WindowTag.COMING_SOON_PROMPT)?.let { iWindowContainer ->
                                    val window: ComingSoonPromptWindow = iWindowContainer.customWindow as ComingSoonPromptWindow
                                    window.setTitle(androidContext.getString(R.string.preset_quiz_title))
                                }
                            }
                        }
                    )
                } else {
                    resultMap[ToolbarWindow.CSToolbarIconButtonType.QUIZ] = CSToolbarIconButton.CSToolbarIconButtonData(
                        isVisible = true,
                        buttonState = CSToolbarIconButton.ButtonState.DISABLED,
                        callback = object : CSToolbarIconButton.Callback {
                            override fun getBoundWindowTag(): WindowTag = WindowTag.UPGRADE_PROMPT
                            override fun isSubWindow(): Boolean = true
                            override fun getObservedWindowTagList(): List<WindowTag> = emptyList()
                            override fun getNewWindowInstance(): IWindow<ViewBinding> {
                                val window: UpgradePromptWindow = get(UpgradePromptWindow::class.java)
                                window.setTitle(androidContext.getString(R.string.preset_quiz_title))
                                window.setIsPremiumUser(isPremiumUser)
                                return window
                            }

                            override fun updateWindowAsNeeded() {
                                csWindowManager.getWindow(WindowTag.UPGRADE_PROMPT)?.let { iWindowContainer ->
                                    val window: UpgradePromptWindow = iWindowContainer.customWindow as UpgradePromptWindow
                                    window.setTitle(androidContext.getString(R.string.preset_quiz_title))
                                    window.setIsPremiumUser(isPremiumUser)
                                }
                            }
                        }
                    )
                }
            }

            ToolbarManager.ParticipationState.JOINED -> {
                resultMap[ToolbarWindow.CSToolbarIconButtonType.CLASS] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = WindowTag.CLASS_MANAGEMENT_MENU
                        override fun isSubWindow(): Boolean = true
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.CLASS_MANAGEMENT_MENU,
                            WindowTag.STUDENT_MANAGEMENT,
                            WindowTag.LEADERBOARD_WINDOW
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> {
                            val window: ClassManagementMenuWindow = get(ClassManagementMenuWindow::class.java)
                            if (!isPremiumUser) {
                                window.setLeaderboardItemState(CSSubordinateMenuItem.ItemState.NEED_TO_UPGRADE)
                            }
                            return window
                        }
                    }
                )
                resultMap[ToolbarWindow.CSToolbarIconButtonType.SETTINGS] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = WindowTag.SETTING_MENU
                        override fun isSubWindow(): Boolean = true
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.SETTING_MENU,
                            WindowTag.SETTING_WINDOW
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> {
                            val window: SettingMenuWindow = get(SettingMenuWindow::class.java)
                            window.setParticipationState(participationState)
                            return window
                        }
                    }
                )
            }

            ToolbarManager.ParticipationState.LESSON_STARTED -> {
                resultMap[ToolbarWindow.CSToolbarIconButtonType.CLASS] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = WindowTag.CLASS_MANAGEMENT_MENU
                        override fun isSubWindow(): Boolean = true
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.CLASS_MANAGEMENT_MENU,
                            WindowTag.STUDENT_MANAGEMENT,
                            WindowTag.LEADERBOARD_WINDOW
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> {
                            val window: ClassManagementMenuWindow = get(ClassManagementMenuWindow::class.java)
                            window.setLeaderboardItemState(CSSubordinateMenuItem.ItemState.NEED_TO_UPGRADE)
                            return window
                        }
                    }
                )
                resultMap[ToolbarWindow.CSToolbarIconButtonType.QUIZ] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = WindowTag.QUIZ_MENU
                        override fun isSubWindow(): Boolean = true
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.QUIZ_MENU,
                            WindowTag.TRUE_FALSE_EDIT_QUIZ,
                            WindowTag.TRUE_FALSE_START_QUIZ,
                            WindowTag.MULTIPLE_CHOICE_EDIT_QUIZ,
                            WindowTag.MULTIPLE_CHOICE_START_QUIZ,
                            WindowTag.SHORT_ANSWER_EDIT_QUIZ,
                            WindowTag.SHORT_ANSWER_START_QUIZ,
                            WindowTag.AUDIO_EDIT_QUIZ,
                            WindowTag.AUDIO_START_QUIZ,
                            WindowTag.POLL_EDIT_QUIZ,
                            WindowTag.POLL_START_QUIZ,
                            WindowTag.WINDOW_TEXT_TRUE_FALSE_START_QUIZ,
                            WindowTag.WINDOW_TEXT_MULTIPLE_CHOICE_START_QUIZ,
                            WindowTag.WINDOW_TEXT_SHORT_ANSWER_START_QUIZ,
                            WindowTag.BATCH_START_QUIZ,
                            WindowTag.BATCH_QUIZ_RESULT
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> {
                            val window: QuizMenuWindow = get(QuizMenuWindow::class.java)
                            if (isPremiumUser) {
                                window.setQuizGeneratorItemState(CSSubordinateMenuItem.ItemState.COMING_SOON)
                            } else {
                                window.setQuizGeneratorItemState(CSSubordinateMenuItem.ItemState.NEED_TO_UPGRADE)
                            }
                            return window
                        }
                    }
                )
                resultMap[ToolbarWindow.CSToolbarIconButtonType.PRESET] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = resolvedQuizCollectionWindowTag()
                        override fun isSubWindow(): Boolean = false
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.WINDOW_QUIZ_COLLECTION,
                            WindowTag.WINDOW_MVB_QUIZ_COLLECTION,
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> = newQuizCollectionWindowInstance()

                        override fun updateWindowAsNeeded() {
                            val activeTag = resolvedQuizCollectionWindowTag()
                            if (activeTag == WindowTag.WINDOW_QUIZ_COLLECTION) {
                                csWindowManager.getWindow(WindowTag.WINDOW_QUIZ_COLLECTION)?.let { iWindowContainer ->
                                    val window: QuizCollectionWindow = iWindowContainer.customWindow as QuizCollectionWindow
                                    window.refreshStartQuizButtonState()
                                }
                            }
                            coroutineScope.launch(Dispatchers.Main) {
                                csWindowManager.bringWindowToTop(activeTag)
                            }
                        }
                    }
                )
                resultMap[ToolbarWindow.CSToolbarIconButtonType.PUSH] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = WindowTag.PUSH_RESPOND
                        override fun isSubWindow(): Boolean = false
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.PUSH_RESPOND
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> {
                            val window: PushRespondWindow = get(PushRespondWindow::class.java)
                            return window
                        }

                        override fun updateWindowAsNeeded() {
                            csWindowManager.getWindow(WindowTag.PUSH_RESPOND)?.let { iWindowContainer ->
                                val window: PushRespondWindow = iWindowContainer.customWindow as PushRespondWindow
                                window.bringToTop()
                            }
                        }
                    }
                )

                resultMap[ToolbarWindow.CSToolbarIconButtonType.TOOLS] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = WindowTag.TOOLS_MENU
                        override fun isSubWindow(): Boolean = true
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.TOOLS_MENU,
                            WindowTag.TIMER_TOOL,
                            WindowTag.RANDOM_DRAW_TOOL,
                            WindowTag.BUZZER_TOOL,
                            WindowTag.SPINNER_TOOL
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> {
                            val window: ToolsMenuWindow = get(ToolsMenuWindow::class.java)
                            return window
                        }
                    }
                )
                resultMap[ToolbarWindow.CSToolbarIconButtonType.INSTANT_PUSH] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.DISABLED,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = WindowTag.COMING_SOON_PROMPT
                        override fun isSubWindow(): Boolean = true
                        override fun getObservedWindowTagList(): List<WindowTag> = emptyList()
                        override fun getNewWindowInstance(): IWindow<ViewBinding> {
                            val window: ComingSoonPromptWindow = get(ComingSoonPromptWindow::class.java)
                            window.setTitle(androidContext.getString(R.string.instant_push_title))
                            return window
                        }

                        override fun updateWindowAsNeeded() {
                            csWindowManager.getWindow(WindowTag.COMING_SOON_PROMPT)?.let { iWindowContainer ->
                                val window: ComingSoonPromptWindow = iWindowContainer.customWindow as ComingSoonPromptWindow
                                window.setTitle(androidContext.getString(R.string.instant_push_title))
                            }
                        }
                    }
                )
                resultMap[ToolbarWindow.CSToolbarIconButtonType.SETTINGS] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE,
                    callback = object : CSToolbarIconButton.Callback {
                        override fun getBoundWindowTag(): WindowTag = WindowTag.SETTING_MENU
                        override fun isSubWindow(): Boolean = true
                        override fun getObservedWindowTagList(): List<WindowTag> = listOf(
                            WindowTag.SETTING_MENU,
                            WindowTag.SETTING_WINDOW
                        )

                        override fun getNewWindowInstance(): IWindow<ViewBinding> {
                            val window: SettingMenuWindow = get(SettingMenuWindow::class.java)
                            window.setParticipationState(participationState)
                            return window
                        }
                    }
                )
            }

            ToolbarManager.ParticipationState.NETWORK_DISCONNECT -> {
                resultMap[ToolbarWindow.CSToolbarIconButtonType.QUIT_APP] = CSToolbarIconButton.CSToolbarIconButtonData(
                    isVisible = true,
                    buttonState = CSToolbarIconButton.ButtonState.ACTIVE
                )
            }
        }
        return resultMap
    }
}