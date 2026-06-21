package com.viewsonic.classswift.ui.window.tool.mvb

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.viewinterop.AndroidView
import com.viewsonic.classswift.data.spinner.SpinnerStudentInfo
import com.viewsonic.classswift.feature.servicescreens.ui.SpinnerScreen
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.webInterface.SpinnerWebInterface
import com.viewsonic.classswift.ui.widget.NetworkDisconnectView
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.windowmodel.tool.mvb.spinner.MvbSpinnerUiEvent
import com.viewsonic.classswift.ui.windowmodel.tool.mvb.spinner.MvbSpinnerUiState
import com.viewsonic.classswift.ui.windowmodel.tool.mvb.spinner.MvbSpinnerWindowModel
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import timber.log.Timber

/**
 * MVB Spinner (service path) — faithful hybrid: the window chrome (header + divider + loading) is
 * Compose (SpinnerScreen), the spinner wheel stays a real WebView, and the offline overlay stays the
 * shared NetworkDisconnectView custom widget — both embedded via AndroidView.
 */
class MvbSpinnerWindow(val context: Context) : ComposeHostWindow(context), SpinnerWebInterface.SpinnerListener {

    private val windowModel: MvbSpinnerWindowModel by KoinJavaComponent.inject(MvbSpinnerWindowModel::class.java)
    private val csWindowManager: CSWindowManager by KoinJavaComponent.inject(CSWindowManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    override var tag: WindowTag = WindowTag.MVB_SPINNER
    override var size: SizeInPixels = SizeInPixels(570.67f.dpToPx().toInt(), 472f.dpToPx().toInt())
    override fun getCurrentSize(): SizeInPixels = size

    private val loading = MutableStateFlow(true)
    private val networkDisconnected = MutableStateFlow(false)

    @get:SuppressLint("SetJavaScriptEnabled")
    private val webView: WebView by lazy {
        WebView(context).apply {
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
            addJavascriptInterface(
                SpinnerWebInterface(context = context).apply { setSpinnerCallbackListener(this@MvbSpinnerWindow) },
                "asyncBridge",
            )
        }
    }

    // Same offline mask widget as the XML (app:cardRadius=mvb_radius_400=10.66dp); its own tap brings
    // the window + toolbar to the top, the alert's "Close" closes the window (mirrors bindCloseAction).
    private val networkDisconnectView: NetworkDisconnectView by lazy {
        NetworkDisconnectView(context).apply {
            radius = 10.66f.dpToPx()
            setCloseClickListener { csWindowManager.removeWindow(WindowTag.MVB_SPINNER) }
            setOnClickListener {
                coroutineScope.launch(Dispatchers.Main) {
                    csWindowManager.bringWindowToTop(WindowTag.MVB_SPINNER)
                    csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
                }
            }
        }
    }

    override fun onViewCreated() {
        super.onViewCreated()
        initWindowModel()
        // Load only after prefetch so web JS doesn't read an empty/stale student cache.
        coroutineScope.launch {
            windowModel.awaitPrefetchReady()
            val url = windowModel.getUrl()
            withContext(Dispatchers.Main) {
                Timber.d("Loading URL after prefetch ready: $url")
                webView.loadUrl(url)
            }
        }
    }

    @Composable
    override fun Content() {
        val isLoading by loading.collectAsState()
        val offline by networkDisconnected.collectAsState()
        SpinnerScreen(
            loading = isLoading,
            networkDisconnected = offline,
            onMinimize = { csWindowManager.minimizeWindow(WindowTag.MVB_SPINNER) },
            onClose = { csWindowManager.removeWindow(WindowTag.MVB_SPINNER) },
            web = { m -> AndroidView(factory = { webView }, modifier = m) },
            disconnectMask = { m -> AndroidView(factory = { networkDisconnectView }, modifier = m) },
        )
    }

    private fun initWindowModel() {
        coroutineScope.launch {
            windowModel.uiStateFlow.collect { state ->
                withContext(Dispatchers.Main) {
                    loading.value = state is MvbSpinnerUiState.Loading
                }
            }
        }
        coroutineScope.launch {
            windowModel.uiEventFlow.collect { event ->
                withContext(Dispatchers.Main) {
                    when (event) {
                        is MvbSpinnerUiEvent.NetworkStatusChange -> networkDisconnected.value = !event.isNetworkConnected
                    }
                }
            }
        }
    }

    // SpinnerWebInterface.SpinnerListener — JS bridge callbacks
    override fun onGetStudentList(): SpinnerStudentInfo = windowModel.getCurrentStudentList(context)

    override fun onStudentPicked(studentId: String) {
        Timber.d("onStudentPicked: $studentId")
        windowModel.sendSelectedStudentEvent(studentId = studentId)
        windowModel.sendSpinnerClickedAmplitudeEvent(studentId = studentId)
    }

    override fun onStudentRemoved(studentId: String) {
        Timber.d("onStudentRemoved: $studentId")
        windowModel.sendSpinnerRemoveClickedAmplitudeEvent(studentId = studentId)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
        windowModel.onCleared()
        CoroutineManager.cancelScope(windowModel)
        CoroutineManager.cancelScope(this)
    }
}
