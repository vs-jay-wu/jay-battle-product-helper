package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WindowInAppTutorialBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.inapptutorial.CSVideoGuideIndicator
import com.viewsonic.classswift.ui.window.adapter.InAppTutorialAdapter
import com.viewsonic.classswift.ui.windowmodel.InAppTutorialWindowModel
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.windowframework.core.utils.LocationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject

class InAppTutorialWindow(
    private val applicationContext: Context,
    private val isFromSettingsWindow: Boolean
) : IWindow<WindowInAppTutorialBinding>, InAppTutorialAdapter.OnItemInteractionListener, InAppTutorialAdapter.OnItemCallback {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val toolbarManager: ToolbarManager by inject(ToolbarManager::class.java)
    private val inAppTutorialWindowModel: InAppTutorialWindowModel by inject(InAppTutorialWindowModel::class.java)
    private val inAppTutorialAdapter: InAppTutorialAdapter = InAppTutorialAdapter(this, this)
    private val linearLayoutManager: LinearLayoutManager = object : LinearLayoutManager(applicationContext, HORIZONTAL, false) {
        override fun canScrollHorizontally(): Boolean {
            return false
        }
    }
    private val skipOnClickListener = View.OnClickListener {
        csSystemDialogWindow = CSSystemDialogWindow.Builder(applicationContext)
            .setTitle(applicationContext.getString(R.string.tutorial_skip_dialog_title))
            .setMessage(applicationContext.getString(R.string.tutorial_skip_dialog_content))
            .setNegativeButton(
                applicationContext.getString(R.string.tutorial_skip_dialog_skip_for_now),
                applicationContext.getColor(R.color.neutral_900)
            ) {
                csSystemDialogWindow?.startNegativeButtonLoading()
                inAppTutorialWindowModel.sendIsInAppTutorialShown(true)
            }
            .setPositiveButton(
                applicationContext.getString(R.string.tutorial_skip_dialog_keep_going),
                applicationContext.getColor(R.color.color_0A8CF0)
            ) {
                csWindowManager.removeWindow(WindowTag.CS_NORMAL_DIALOG)
            }
            .build()
        csSystemDialogWindow?.show()
    }
    private var csSystemDialogWindow: CSSystemDialogWindow? = null

    override var tag: WindowTag = WindowTag.WINDOW_IN_APP_TUTORIAL
    override var size: SizeInPixels =
        SizeInPixels(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    override val binding: WindowInAppTutorialBinding = WindowInAppTutorialBinding.inflate(
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
        initCollection()
        initView()
        initRecycleView()
    }

    //region InAppTutorialAdapter.OnItemInteractionListener
    override fun onWelcomeItemShown() {
        with(binding) {
            mbLeftCta.text = applicationContext.getString(R.string.tutorial_actions_skip)
            mbLeftCta.setOnClickListener(skipOnClickListener)
            mbRightCtaNegative.visibility = View.GONE
            mbRightCtaPositive.text = applicationContext.getString(R.string.tutorial_actions_get_started)
            mbRightCtaPositive.setOnClickListener {
                val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()
                rvContent.scrollToPosition(firstVisibleItemPosition + 1)
            }
            csvgiVideoGuideIndicator.visibility = View.GONE
            csvgiVideoGuideIndicator.setState(CSVideoGuideIndicator.State.CLEAR)
        }
    }

    override fun onVideoGuideItemShown(index: Int) {
        with(binding) {
            mbLeftCta.text = applicationContext.getString(R.string.tutorial_actions_skip)
            mbLeftCta.setOnClickListener(skipOnClickListener)
            mbRightCtaNegative.text = applicationContext.getString(R.string.tutorial_actions_back)
            mbRightCtaNegative.visibility = View.VISIBLE
            mbRightCtaNegative.setOnClickListener {
                val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()
                rvContent.scrollToPosition(firstVisibleItemPosition - 1)
            }
            mbRightCtaPositive.text = applicationContext.getString(R.string.tutorial_actions_next)
            mbRightCtaPositive.setOnClickListener {
                val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()
                rvContent.scrollToPosition(firstVisibleItemPosition + 1)
            }
            csvgiVideoGuideIndicator.visibility = View.VISIBLE
            csvgiVideoGuideIndicator.setState(CSVideoGuideIndicator.State.entries[index])
        }
    }

    override fun onCompeteItemShown() {
        with(binding) {
            mbLeftCta.text = applicationContext.getString(R.string.tutorial_actions_watch_again)
            mbLeftCta.setOnClickListener {
                rvContent.scrollToPosition( inAppTutorialAdapter.getFirstVideoGuidePageIndex() )
            }
            mbRightCtaNegative.visibility = View.GONE
            mbRightCtaPositive.text = applicationContext.getString(R.string.tutorial_actions_ready)
            mbRightCtaPositive.setOnClickListener {
                inAppTutorialWindowModel.sendIsInAppTutorialShown(true)
            }
            csvgiVideoGuideIndicator.visibility = View.GONE
            csvgiVideoGuideIndicator.setState(CSVideoGuideIndicator.State.CLEAR)
        }
    }

    override fun onThumbUpClicked() {
        inAppTutorialWindowModel.sendFeedback(true)
    }

    override fun onThumbDownClicked() {
        inAppTutorialWindowModel.sendFeedback(false)
    }
    //endregion

    //region InAppTutorialAdapter.OnItemCallback
    override fun isNeedToShowFeedbackUI(): Boolean = inAppTutorialWindowModel.isNeedToShowFeedbackUI
    //endregion

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            inAppTutorialWindowModel.uiEventSharedFlow.collect { event ->
                withContext(Dispatchers.Main) {
                    when (event) {
                        InAppTutorialWindowModel.InAppTutorialUiEvent.SendIsInAppTutorialShownFailed -> {
                            csSystemDialogWindow?.dismiss()
                            showErrorToast()
                        }
                        InAppTutorialWindowModel.InAppTutorialUiEvent.SendIsInAppTutorialShownSuccessful -> {
                            csSystemDialogWindow?.dismiss()
                            navigateToNextDestination()
                        }
                        InAppTutorialWindowModel.InAppTutorialUiEvent.SendFeedbackFailed -> {
                            showErrorToast()
                        }
                        InAppTutorialWindowModel.InAppTutorialUiEvent.SendFeedbackSuccessful -> {}
                    }
                }
            }
        }
    }

    private fun initView() {
        with(binding) {
            cstErrorToast.setActionOnClickListener {
                inAppTutorialWindowModel.forceQuitApp()
            }
        }
    }

    private fun initRecycleView() {
        val pagerSnapHelper = PagerSnapHelper()
        binding.rvContent.apply {
            pagerSnapHelper.attachToRecyclerView(this)
            layoutManager = linearLayoutManager
            adapter = inAppTutorialAdapter
        }
    }

    private fun navigateToNextDestination() {
        csWindowManager.removeWindow(WindowTag.WINDOW_IN_APP_TUTORIAL)
        if (!isFromSettingsWindow) {
            inAppTutorialWindowModel.connectSocket()
            inAppTutorialWindowModel.checkMultipleLogin()

            val myClassWindow: MyClassWindow = get(MyClassWindow::class.java)
            csWindowManager.createWindow(myClassWindow, Gravity.CENTER)

            val toolbarWindow: ToolbarWindow = get(ToolbarWindow::class.java)
            val location = LocationUtil.gravityToLocation(Gravity.CENTER_BOTTOM, toolbarWindow.getCurrentSize())
            csWindowManager.createWindow(
                toolbarWindow,
                location.apply { coordinateY -= 23.dpToPx().toInt()}
            )
            toolbarManager.setIsExpanded(true)
            toolbarManager.setParticipationState(ToolbarManager.ParticipationState.NOT_JOINED)
        }
    }

    private fun showErrorToast() {
        binding.cstErrorToast.show(coroutineScope)
    }
}