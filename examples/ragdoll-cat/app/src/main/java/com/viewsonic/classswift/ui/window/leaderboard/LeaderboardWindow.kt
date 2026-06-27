package com.viewsonic.classswift.ui.window.leaderboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.view.ContextThemeWrapper
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.google.android.material.R
import com.viewsonic.classswift.databinding.WindowLeaderboardBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.webInterface.LeaderBoardWebInterface
import com.viewsonic.classswift.ui.windowmodel.LeaderboardWindowModel
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import kotlin.getValue

class LeaderboardWindow(val context: Context) : IWindow<WindowLeaderboardBinding>, LeaderBoardWebInterface.LeaderBoardWebListener {

    override var tag: WindowTag = WindowTag.LEADERBOARD_WINDOW
    override var size: SizeInPixels = SizeInPixels(854f.dpToPx().toInt(), 520f.dpToPx().toInt())
    override val binding: WindowLeaderboardBinding = WindowLeaderboardBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                R.style.Theme_MaterialComponents
            )
        )
    )

    private val coroutineScope = CoroutineManager.getScope(this)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val windowModel: LeaderboardWindowModel by inject(LeaderboardWindowModel::class.java)

    init {
        initWebView()
        initClick()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        with(binding.wvLeaderboard) {
            val webInterface = LeaderBoardWebInterface().apply {
                setListener(listener = this@LeaderboardWindow)
            }
            addJavascriptInterface(webInterface, "asyncBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Timber.tag("Leaderboard").d("WebViewClient onPageStarted: $url")
                    // start load url, need set loadUrlHadError to false.
                    windowModel.loadUrlHadError = false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    Timber.tag("Leaderboard").d("WebViewClient onPageFinished")
                    if ( windowModel.loadUrlHadError == false) {
                        binding.cslbTryAgain.setEnable()
                        binding.wvLeaderboard.visibility = View.VISIBLE
                        binding.clErrorPage.visibility = View.GONE
                    }
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    Timber.tag("Leaderboard").d("WebViewClient onReceivedError msg: ${error?.description}")
                    //meaning error occurs is by load url.
                    if (request?.isForMainFrame == true) {
                        windowModel.loadUrlHadError = true
                        binding.cslbTryAgain.setEnable()
                        binding.clErrorPage.visibility = View.VISIBLE
                        binding.wvLeaderboard.visibility = View.INVISIBLE
                    }
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    Timber.tag("Leaderboard").d("WebViewClient onReceivedHttpError msg: ${errorResponse?.statusCode}")
                    //meaning error occurs is by load url.
                    if (request?.isForMainFrame == true) {
                        windowModel.loadUrlHadError = true
                        binding.clErrorPage.visibility = View.VISIBLE
                        binding.wvLeaderboard.visibility = View.INVISIBLE
                    }
                }
            }
            setBackgroundColor(Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                allowFileAccess = true
                allowContentAccess = true
                isVerticalScrollBarEnabled = true
                setSupportZoom(true)
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            val url = windowModel.getLeaderBoardUrl()
            Timber.tag("Leaderboard").d("Loading LeaderBoardUrl: $url")
            loadUrl(url)
        }
    }

    private fun initClick() {
        binding.apply {
            WindowControlButtonsUiHelper.setup(
                ivClose = ibClose,
                ivMinimizeWindow = ivMinimizeWindow,
                ivToolbarBringToFront = ivToolbarBringToFront,
                windowTag = tag,
                isMvbBound = windowModel.isMyViewBoardBound(),
                csWindowManager = csWindowManager,
                coroutineScope = coroutineScope,
                onCloseClick = { csWindowManager.removeWindow(tag) }
            )
            cslbTryAgain.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    cslbTryAgain.setLoading()
                    val url = windowModel.getLeaderBoardUrl()
                    Timber.tag("Leaderboard").d("Reload LeaderBoardUrl: $url")
                    wvLeaderboard.loadUrl(url)
                }
            })
        }
    }

    override fun getAccessToken(): String {
        Timber.tag("Leaderboard").d("getAccessToken")
        return windowModel.getAccessToken()
    }

    override fun viewFullRecord(lessonId: String) {
        Timber.tag("Leaderboard").d("viewFullRecord lessonId: $lessonId")
        context.let {
            val builder = CustomTabsIntent.Builder()
                .setShowTitle(true) // show title
                .setStartAnimations(it, android.R.anim.slide_in_left, android.R.anim.slide_out_right) // fade in animation
                .setExitAnimations(it, android.R.anim.slide_in_left, android.R.anim.slide_out_right) // fade out animation
            val customTabsIntent = builder.build()
            customTabsIntent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            customTabsIntent.launchUrl(it, windowModel.getFullRecordUrl(lessonId).toUri())
        }
    }

    override fun enterNextClass() {
        Timber.tag("Leaderboard").d("enterNextClass")
        csWindowManager.removeWindow(tag)
    }
}