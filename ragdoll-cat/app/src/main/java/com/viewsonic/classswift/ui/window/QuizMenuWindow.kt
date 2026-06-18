package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.viewbinding.ViewBinding
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowQuizMenuBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.ScreenshotManager
import com.viewsonic.classswift.ui.widget.toolbar.CSSubordinateMenuItem
import com.viewsonic.classswift.ui.window.quiz.edit.AudioQuizEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MultipleChoiceEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.PollQuizEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.ShortAnswerEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.TrueFalseEditWindow
import com.viewsonic.classswift.uimanager.QuizUiManager
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnCSWindowChangedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class QuizMenuWindow(val applicationContext: Context) : IWindow<WindowQuizMenuBinding>, OnCSWindowChangedListener {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val screenshotManager: ScreenshotManager by inject(ScreenshotManager::class.java)
    private val quizManager: QuizManager by inject(QuizManager::class.java)
    private val quizUiManager: QuizUiManager by inject(QuizUiManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)

    override var tag: WindowTag = WindowTag.QUIZ_MENU
    override var size: SizeInPixels = SizeInPixels(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )

    override val binding: WindowQuizMenuBinding = WindowQuizMenuBinding.inflate(
        LayoutInflater.from(applicationContext)
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onViewCreated() {
        csWindowManager.addOnWindowChangedListener(this@QuizMenuWindow)
        checkIfStateChanged()
    }

    override fun onDestroy() {
        csWindowManager.removeOnWindowChangedListener(this@QuizMenuWindow)
        csWindowManager.getWindow(WindowTag.TOOLBAR)?.let { iWindowContainer ->
            val window: ToolbarWindow = iWindowContainer.customWindow as ToolbarWindow
            window.checkIconButtonSelectedState()
        }
    }

    override fun onCSWindowCountChanged() {
        checkIfStateChanged()
    }

    override fun onCSWindowHiddenCountChange() = Unit

    private fun checkIfStateChanged() {
        val openedQuizWindowTag = quizUiManager.getCurrentOpenedQuizWindowTag()
        Timber.d("[checkIfStateChanged] openedQuizWindowTag: $openedQuizWindowTag")

        if (openedQuizWindowTag == WindowTag.NONE) {
            // Please remember to reset the quiz button state to normal when implementing the new quiz flow.
            setTrueOrFalseItemState(CSSubordinateMenuItem.ItemState.NORMAL)
            setMultipleSelectionItemState(CSSubordinateMenuItem.ItemState.NORMAL)
            setShortAnswerItemState(CSSubordinateMenuItem.ItemState.NORMAL)
            setAudioItemState(CSSubordinateMenuItem.ItemState.NORMAL)
            setPollItemState(CSSubordinateMenuItem.ItemState.NORMAL)

            binding.apply {
                cssmiTrueOrFalse.setOnClickListener {
                    startCaptureScreenshot(WindowTag.TRUE_FALSE_EDIT_QUIZ)
                }
                cssmiMultipleSelection.setOnClickListener {
                    startCaptureScreenshot(WindowTag.MULTIPLE_CHOICE_EDIT_QUIZ)
                }
                cssmiShortAnswer.setOnClickListener {
                    startCaptureScreenshot(WindowTag.SHORT_ANSWER_EDIT_QUIZ)
                }
                cssmiAudio.setOnClickListener {
                    startCaptureScreenshot(WindowTag.AUDIO_EDIT_QUIZ)
                }
                cssmiPoll.setOnClickListener {
                    startCaptureScreenshot(WindowTag.POLL_EDIT_QUIZ)
                }
            }
        } else {
            val quizCategory = quizManager.checkQuizCategory(openedQuizWindowTag)
            with(binding) {
                when (quizCategory) {
                    QuizManager.QuizCategory.TRUE_FALSE -> {
                        setTrueOrFalseItemState(CSSubordinateMenuItem.ItemState.SELECTED)
                        setMultipleSelectionItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setShortAnswerItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setAudioItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setPollItemState(CSSubordinateMenuItem.ItemState.DISABLED)

                        cssmiMultipleSelection.setOnClickListener {}
                        cssmiShortAnswer.setOnClickListener {}

                        cssmiTrueOrFalse.setOnClickListener {
                            coroutineScope.launch(Dispatchers.Main) {
                                csWindowManager.bringWindowToTop(openedQuizWindowTag)
                                csWindowManager.removeSubWindow(tag)
                            }
                        }
                        cssmiAudio.setOnClickListener {}
                        cssmiPoll.setOnClickListener {}
                    }

                    QuizManager.QuizCategory.MULTIPLE_SELECTION -> {
                        setTrueOrFalseItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setMultipleSelectionItemState(CSSubordinateMenuItem.ItemState.SELECTED)
                        setShortAnswerItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setAudioItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setPollItemState(CSSubordinateMenuItem.ItemState.DISABLED)

                        cssmiTrueOrFalse.setOnClickListener {}
                        cssmiShortAnswer.setOnClickListener {}

                        cssmiMultipleSelection.setOnClickListener {
                            coroutineScope.launch(Dispatchers.Main) {
                                csWindowManager.bringWindowToTop(openedQuizWindowTag)
                                csWindowManager.removeSubWindow(tag)
                            }
                        }
                        cssmiAudio.setOnClickListener {}
                        cssmiPoll.setOnClickListener {}
                    }

                    QuizManager.QuizCategory.SHORT_ANSWER -> {
                        setTrueOrFalseItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setMultipleSelectionItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setShortAnswerItemState(CSSubordinateMenuItem.ItemState.SELECTED)
                        setAudioItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setPollItemState(CSSubordinateMenuItem.ItemState.DISABLED)

                        cssmiTrueOrFalse.setOnClickListener {}
                        cssmiMultipleSelection.setOnClickListener {}

                        cssmiShortAnswer.setOnClickListener {
                            coroutineScope.launch(Dispatchers.Main) {
                                csWindowManager.bringWindowToTop(openedQuizWindowTag)
                                csWindowManager.removeSubWindow(tag)
                            }
                        }
                        cssmiAudio.setOnClickListener {}
                        cssmiPoll.setOnClickListener {}
                    }
                    QuizManager.QuizCategory.AUDIO -> {
                        setTrueOrFalseItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setMultipleSelectionItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setShortAnswerItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setAudioItemState(CSSubordinateMenuItem.ItemState.SELECTED)
                        setPollItemState(CSSubordinateMenuItem.ItemState.DISABLED)

                        cssmiTrueOrFalse.setOnClickListener {}
                        cssmiMultipleSelection.setOnClickListener {}
                        cssmiShortAnswer.setOnClickListener {}
                        cssmiAudio.setOnClickListener {
                            coroutineScope.launch(Dispatchers.Main) {
                                csWindowManager.bringWindowToTop(openedQuizWindowTag)
                                csWindowManager.removeSubWindow(tag)
                            }
                        }
                        cssmiPoll.setOnClickListener {}
                    }
                    QuizManager.QuizCategory.POLL -> {
                        setTrueOrFalseItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setMultipleSelectionItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setShortAnswerItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setAudioItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setPollItemState(CSSubordinateMenuItem.ItemState.SELECTED)

                        cssmiTrueOrFalse.setOnClickListener {}
                        cssmiMultipleSelection.setOnClickListener {}
                        cssmiShortAnswer.setOnClickListener {}
                        cssmiAudio.setOnClickListener {}
                        cssmiPoll.setOnClickListener {
                            coroutineScope.launch(Dispatchers.Main) {
                                csWindowManager.bringWindowToTop(openedQuizWindowTag)
                                csWindowManager.removeSubWindow(tag)
                            }
                        }
                    }
                    QuizManager.QuizCategory.BATCH_QUIZ -> {
                        setTrueOrFalseItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setMultipleSelectionItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setShortAnswerItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setAudioItemState(CSSubordinateMenuItem.ItemState.DISABLED)
                        setPollItemState(CSSubordinateMenuItem.ItemState.DISABLED)

                        cssmiTrueOrFalse.setOnClickListener {}
                        cssmiMultipleSelection.setOnClickListener {}
                        cssmiShortAnswer.setOnClickListener {}
                        cssmiAudio.setOnClickListener {}
                        cssmiPoll.setOnClickListener {}
                    }
                    QuizManager.QuizCategory.QUIZ_GENERATOR,
                    QuizManager.QuizCategory.UNSPECIFIED -> {}
                }
            }
        }
    }

    private fun startCaptureScreenshot(destinationWindowTag: WindowTag) {
        csWindowManager.removeSubWindow(WindowTag.QUIZ_MENU)
        screenshotManager.startCaptureScreenshot(
            screenshotSource = screenshotManager.getScreenShotSource(destinationWindowTag),
            onSuccess = {
                csWindowManager.removeWindow(destinationWindowTag)
                QuizSharedUiInfo.screenshotImageUri = screenshotManager.getScreenshotImageUri()
                QuizSharedUiInfo.setQuizTypeByTag(destinationWindowTag)
                getOpenWindow(destinationWindowTag)?.let { window ->
                    csWindowManager.createWindow(window, Gravity.CENTER)
                }
            },
            onFailed = {
                ToastWindow.MakeText(
                    applicationContext,
                    applicationContext.getString(R.string.quiz_error_msg_screenshot),
                    3000
                ).build().show()
            },

            onCancel = {}
        )
    }

    private fun getOpenWindow(tag: WindowTag): IWindow<ViewBinding>? = when (tag) {
        WindowTag.TRUE_FALSE_EDIT_QUIZ -> {
            get(TrueFalseEditWindow::class.java)
        }
        WindowTag.MULTIPLE_CHOICE_EDIT_QUIZ -> {
            get(MultipleChoiceEditWindow::class.java)
        }
        WindowTag.SHORT_ANSWER_EDIT_QUIZ -> {
            get(ShortAnswerEditWindow::class.java)
        }
        WindowTag.AUDIO_EDIT_QUIZ -> {
            get(AudioQuizEditWindow::class.java)
        }
        WindowTag.POLL_EDIT_QUIZ -> {
            get(PollQuizEditWindow::class.java)
        }
        else -> {
            null
        }
    }

    private fun setTrueOrFalseItemState(itemState: CSSubordinateMenuItem.ItemState) {
        binding.cssmiTrueOrFalse.setItemState(itemState)
    }

    private fun setMultipleSelectionItemState(itemState: CSSubordinateMenuItem.ItemState) {
        binding.cssmiMultipleSelection.setItemState(itemState)
    }

    fun setPollItemState(itemState: CSSubordinateMenuItem.ItemState) {
        binding.cssmiPoll.setItemState(itemState)
    }

    private fun setShortAnswerItemState(itemState: CSSubordinateMenuItem.ItemState) {
        binding.cssmiShortAnswer.setItemState(itemState)
    }

    private fun setAudioItemState(itemState: CSSubordinateMenuItem.ItemState) {
        binding.cssmiAudio.setItemState(itemState)
    }

    fun setQuizGeneratorItemState(itemState: CSSubordinateMenuItem.ItemState) {
        binding.cssmiQuizGenerator.setItemState(itemState)
    }

}