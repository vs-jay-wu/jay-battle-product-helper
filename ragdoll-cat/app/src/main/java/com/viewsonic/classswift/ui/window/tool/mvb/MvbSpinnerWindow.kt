package com.viewsonic.classswift.ui.window.tool.mvb

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import com.google.android.material.R
import com.viewsonic.classswift.data.spinner.SpinnerStudentInfo
import com.viewsonic.classswift.databinding.WindowMvbSpinnerBinding
import com.viewsonic.classswift.feature.servicescreens.ui.SpinnerHeader
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.webInterface.SpinnerWebInterface
import com.viewsonic.classswift.ui.window.quiz.mvb.ComposeWindowHost
import com.viewsonic.classswift.ui.windowmodel.tool.mvb.spinner.MvbSpinnerUiEvent
import com.viewsonic.classswift.ui.windowmodel.tool.mvb.spinner.MvbSpinnerUiState
import com.viewsonic.classswift.ui.windowmodel.tool.mvb.spinner.MvbSpinnerWindowModel
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import timber.log.Timber

/**
 * MVB Spinner (service path) — faithful hybrid: the window chrome (header icon/title/minimize/close
 * + divider) is the Compose [SpinnerHeader] hosted in `cv_header`; the spinner wheel (WebView), the
 * Loading label and the offline NetworkDisconnectView stay native children so they render exactly as
 * the original (this overlay window is not hardware-accelerated, so a WebView only paints when it is
 * a direct native child — not when nested inside a ComposeView).
 */
class MvbSpinnerWindow(
    val context: Context
) : IWindow<WindowMvbSpinnerBinding>, SpinnerWebInterface.SpinnerListener {

    private val windowModel: MvbSpinnerWindowModel by KoinJavaComponent.inject(MvbSpinnerWindowModel::class.java)
    private val csWindowManager: CSWindowManager by KoinJavaComponent.inject(CSWindowManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val composeHost = ComposeWindowHost()

    override var tag: WindowTag = WindowTag.MVB_SPINNER
    override var size: SizeInPixels = SizeInPixels(570.67f.dpToPx().toInt(), 472f.dpToPx().toInt())
    override val binding: WindowMvbSpinnerBinding = WindowMvbSpinnerBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                R.style.Theme_MaterialComponents
            )
        )
    )

    init {
        initWindowModel()
        initHeader()
        initClickedAction()
        initWebView()
    }

    private fun initHeader() {
        composeHost.attach(binding.cvHeader) {
            SpinnerHeader(
                onMinimize = { csWindowManager.minimizeWindow(WindowTag.MVB_SPINNER) },
                onClose = { csWindowManager.removeWindow(WindowTag.MVB_SPINNER) },
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        with(binding.wvWebView) {
            setBackgroundColor(Color.TRANSPARENT)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                allowFileAccess = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
                setSupportZoom(true)
                cacheMode = WebSettings.LOAD_NO_CACHE
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Timber.d("MvbSpinnerWindow WebView page loaded: $url")
                    windowModel.onWebPageFinished()
                }
            }

            webChromeClient = WebChromeClient()

            val spinnerWebInterface = SpinnerWebInterface(context = context).apply {
                setSpinnerCallbackListener(listener = this@MvbSpinnerWindow)
            }

            addJavascriptInterface(spinnerWebInterface, "asyncBridge")
        }

        // 等 prefetch 完成才 loadUrl，避免 web JS 啟動時 getStudentList()
        // 早於 fetch 完成而拿到 manager 的空 / 舊 cache。
        coroutineScope.launch {
            windowModel.awaitPrefetchReady()
            val url = windowModel.getUrl()
            withContext(Dispatchers.Main) {
                Timber.d("Loading URL after prefetch ready: $url")
                binding.wvWebView.loadUrl(url)
            }
        }
    }

    private fun initClickedAction() {
        with(binding) {
            ndvNetworkDisconnectMask.setOnClickListener {
                coroutineScope.launch(Dispatchers.Main) {
                    csWindowManager.let {
                        it.bringWindowToTop(tag)
                        it.bringWindowToTop(WindowTag.TOOLBAR)
                    }
                }
            }

            // VSFT-8799: the disconnect mask overlays the (now Compose) header close button, so the
            // alert's own "Close" closes the window directly — same effect as the old bindCloseAction.
            ndvNetworkDisconnectMask.setCloseClickListener {
                csWindowManager.removeWindow(WindowTag.MVB_SPINNER)
            }
        }
    }

    private fun initWindowModel() {
        coroutineScope.launch {
            windowModel.uiEventFlow.collect { event ->
                withContext(Dispatchers.Main) {
                    handleUiEventUpdate(event = event)
                }
            }
        }
        coroutineScope.launch {
            windowModel.uiStateFlow.collect { state ->
                withContext(Dispatchers.Main) {
                    handleUiStateUpdate(state = state)
                }
            }
        }
    }

    private fun handleUiEventUpdate(event: MvbSpinnerUiEvent) {
        when (event) {
            is MvbSpinnerUiEvent.NetworkStatusChange -> {
                handleNetworkStatusChange(isNetworkConnected = event.isNetworkConnected)
            }
        }
    }

    private fun handleUiStateUpdate(state: MvbSpinnerUiState) {
        when (state) {
            is MvbSpinnerUiState.Loading -> {
                binding.clLoading.isVisible = true
                binding.wvWebView.isVisible = false
            }
            is MvbSpinnerUiState.Ready -> {
                binding.clLoading.isVisible = false
                binding.wvWebView.isVisible = true
            }
        }
    }

    private fun handleNetworkStatusChange(isNetworkConnected: Boolean) {
        binding.ndvNetworkDisconnectMask.isVisible = !isNetworkConnected
    }

    //SpinnerWebInterface SpinnerListener
    override fun onGetStudentList(): SpinnerStudentInfo {
        return windowModel.getCurrentStudentList(context)
    }

    //SpinnerWebInterface SpinnerListener
    override fun onStudentPicked(studentId: String) {
        Timber.d("onStudentPicked: $studentId")
        windowModel.sendSelectedStudentEvent(studentId = studentId)
        windowModel.sendSpinnerClickedAmplitudeEvent(studentId = studentId)
    }

    //SpinnerWebInterface SpinnerListener
    override fun onStudentRemoved(studentId: String) {
        Timber.d("onStudentRemoved: $studentId")
        windowModel.sendSpinnerRemoveClickedAmplitudeEvent(studentId = studentId)
    }

    override fun onDestroy() {
        composeHost.destroy()
        windowModel.onCleared()
        CoroutineManager.cancelScope(windowModel)
        CoroutineManager.cancelScope(this)
    }
}
