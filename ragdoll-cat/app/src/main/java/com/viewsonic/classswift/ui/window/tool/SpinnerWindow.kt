package com.viewsonic.classswift.ui.window.tool

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import com.google.android.material.R
import com.viewsonic.classswift.data.spinner.SpinnerStudentInfo
import com.viewsonic.classswift.databinding.WindowSpinnerBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.webInterface.SpinnerWebInterface
import com.viewsonic.classswift.ui.windowmodel.tool.spinner.SpinnerUiEvent
import com.viewsonic.classswift.ui.windowmodel.tool.spinner.SpinnerWindowModel
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

class SpinnerWindow(
    val context: Context
) : IWindow<WindowSpinnerBinding>, SpinnerWebInterface.SpinnerListener {

    private val windowModel: SpinnerWindowModel by KoinJavaComponent.inject(SpinnerWindowModel::class.java)
    private val csWindowManager: CSWindowManager by KoinJavaComponent.inject(CSWindowManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    override var tag: WindowTag = WindowTag.SPINNER_TOOL
    override var size: SizeInPixels = SizeInPixels(738f.dpToPx().toInt(), 597.66f.dpToPx().toInt())
    override val binding: WindowSpinnerBinding = WindowSpinnerBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                R.style.Theme_MaterialComponents
            )
        )
    )

    init {
        initWindowModel()
        initClickedAction()
        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        with(binding.wvWebView) {
            setBackgroundColor(Color.TRANSPARENT)

            visibility = View.GONE

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
                    Timber.Forest.d("WebViewFragment WebView page loaded: $url")
                    visibility = View.VISIBLE
                }
            }

            webChromeClient = WebChromeClient()

            val spinnerWebInterface = SpinnerWebInterface(context = context).apply {
                setSpinnerCallbackListener(listener = this@SpinnerWindow)
            }

            addJavascriptInterface(spinnerWebInterface, "asyncBridge")

            val url = windowModel.getUrl()

            Timber.Forest.d("Loading URL: $url")
            loadUrl(url)
        }

    }

    private fun initClickedAction() {
        with(binding) {
            ibClose.setOnClickListener {
                csWindowManager.removeWindow(WindowTag.SPINNER_TOOL)
            }

            ndvNetworkDisconnectMask.setOnClickListener {
                coroutineScope.launch(Dispatchers.Main) {
                    csWindowManager.let {
                        it.bringWindowToTop(tag)
                        it.bringWindowToTop(WindowTag.TOOLBAR)
                    }
                }
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
    }

    private fun handleUiEventUpdate(event: SpinnerUiEvent) {
        when (event) {
            is SpinnerUiEvent.NetworkStatusChange -> {
                handleNetworkStatusChange(
                    isNetworkConnected = event.isNetworkConnected
                )
            }

            else -> Unit
        }
    }

    private fun handleNetworkStatusChange(isNetworkConnected: Boolean) {
        binding.ndvNetworkDisconnectMask.isVisible = !isNetworkConnected
    }

    //SpinnerWebInterface SpinnerListener
    override fun onGetStudentList(): SpinnerStudentInfo {
        return windowModel.getCurrentStudentList()
    }

    //SpinnerWebInterface SpinnerListener
    override fun onStudentPicked(studentId: String) {
        Timber.Forest.d("onStudentPicked: $studentId")
        windowModel.sendSelectedStudentEvent(studentId = studentId)
        windowModel.sendSpinnerClickedAmplitudeEvent()
    }

    //SpinnerWebInterface SpinnerListener
    override fun onStudentRemoved(studentId: String) {
        Timber.Forest.d("onStudentRemoved: $studentId")
        windowModel.sendSpinnerRemoveClickedAmplitudeEvent()
    }
}