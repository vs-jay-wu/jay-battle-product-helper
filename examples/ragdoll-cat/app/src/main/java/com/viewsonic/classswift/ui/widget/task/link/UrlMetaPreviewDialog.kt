package com.viewsonic.classswift.ui.widget.task.link

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import coil.load
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.task.UrlPreviewInfo
import com.viewsonic.classswift.databinding.WindowUrlMetaPreviewDialogBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widgetmodel.task.UrlMetaPreviewDialogWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.task.state.UrlMetaPreviewUiState
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import androidx.core.net.toUri
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * UrlMetaPreviewDialog
 *
 * Displays a dialog that previews the meta information of a given URL, including:
 * - Page title
 * - Description
 * - Preview image
 *
 * Test URL:
 * https://ogp.me/
 * https://www.example.com
 * https://www.wikipedia.org
 */
class UrlMetaPreviewDialog @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), KoinComponent {

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val csWindowManager: CSWindowManager by KoinJavaComponent.inject(
        CSWindowManager::class.java
    )

    private val widgetModel: UrlMetaPreviewDialogWidgetModel by KoinJavaComponent.inject(
        UrlMetaPreviewDialogWidgetModel::class.java
    )
    private var currentUrlMetaInfo: UrlPreviewInfo? = null
    private var eventListener: UrlMetaPreviewDialogEventListener? = null
    private var containerWindowTag: WindowTag = WindowTag.NONE
    private val binding: WindowUrlMetaPreviewDialogBinding = WindowUrlMetaPreviewDialogBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )
    private val textStateFlow = MutableStateFlow("")


    interface UrlMetaPreviewDialogEventListener {
        fun onConfirmClick(data: UrlPreviewInfo)
        fun onCancelClick()
    }

    init {
        //Block clicks on the semi-transparent background
        setOnClickListener { }
        initClickAction()
        observeUIState()
        initEdittext()
    }

    fun setUrlMetaPreviewDialogEventListener(listener: UrlMetaPreviewDialogEventListener) {
        eventListener = listener
    }

    fun setContainerWindowTag(tag: WindowTag) {
        containerWindowTag = tag
    }

    fun getCurrentMetaInfo(): UrlPreviewInfo? {
        return currentUrlMetaInfo
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun dismiss() {
        visibility = View.GONE
    }

    private fun observeUIState() {
        coroutineScope.launch {
            widgetModel.uiStateFlow.collect { state ->
                withContext(Dispatchers.Main) {
                    onUIStateUpdate(state = state)
                }
            }
        }
    }

    private fun initClickAction() {

        binding.acbPositive.isEnabled = false

        with(binding) {
            acbPositive.setOnClickListener {

                currentUrlMetaInfo?.let {
                    eventListener?.onConfirmClick(data = it)
                }

                clearMetaPreview()
                showMetaPreview(isShow = false)
            }

            btNegative.setOnClickListener {
                eventListener?.onCancelClick()
                clearMetaPreview()
                showMetaPreview(isShow = false)
            }

            clOpenLink.setOnClickListener {
                currentUrlMetaInfo?.let {
                    val url = it.url
                    val builder = CustomTabsIntent.Builder()
                    val customTabsIntent = builder.build()
                    customTabsIntent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    customTabsIntent.launchUrl(context, url.toUri())
                }
            }
        }
    }

    private fun onUIStateUpdate(state: UrlMetaPreviewUiState) {
        Timber.d("onUIStateUpdate: ${state.toString()}")

        when (state) {
            is UrlMetaPreviewUiState.UrlMetaPreviewUpdate -> {
                enablePositiveButton(true)
                bindMetaData(data = state.data)
            }

            is UrlMetaPreviewUiState.UrlMetaFetchFail -> {
                enablePositiveButton(false)
                bindMetaData(data = state.data)
            }

            else -> Unit
        }
    }

    @OptIn(FlowPreview::class)
    private fun initEdittext() {
        with(binding.etUrlInput) {

            setOnFocusChangeListener { _, hasFocus ->
                with(csWindowManager) {

                    /**
                     * Updates the floating window's flags based on the view's focus state.
                     *
                     * - When the view gains focus, removes FLAG_NOT_FOCUSABLE to allow the window
                     *   to receive input events (e.g., show keyboard and accept text input).
                     * - When the view loses focus, adds FLAG_NOT_FOCUSABLE back to prevent the
                     *   floating window from blocking interactions with other windows.
                     *
                     * Commonly used for EditText in floating windows to dynamically enable or
                     * disable input focus behavior.
                     */
                    getWindow(containerWindowTag)?.getLayoutParam()?.let {
                        if (hasFocus) {
                            it.flags = it.flags and android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                            getWindow(containerWindowTag)?.updateLayoutParam(it)
                        } else {
                            it.flags = it.flags or android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            getWindow(containerWindowTag)?.updateLayoutParam(it)
                        }
                    }
                }
            }

            coroutineScope.launch(Dispatchers.Main) {
                textStateFlow
                    .debounce(500)
                    .map { it.trim() }
                    .distinctUntilChanged()
                    .collect { url ->
                        Timber.d("user input url: $url")
                        if (url.trim().isNotEmpty()) {
                            widgetModel.fetchUrlMeta(url = formatUrl(url))
                        } else {
                            enablePositiveButton(false)
                            showMetaPreview(false)
                        }
                    }
            }

            doAfterTextChanged { editable ->
                enablePositiveButton(false)
                showMetaPreview(false)
                textStateFlow.value = editable?.toString() ?: ""
            }
        }
    }

    private fun bindMetaData(data: UrlPreviewInfo) {
        currentUrlMetaInfo = data
        bindUrlTitle(title = data.title)
        bindDescription(description = data.description)
        bindUrlLink(url = data.url)
        bindThumbnail(imageUrl = data.imageUrl)
        showMetaPreview(isShow = true)
    }

    private fun bindUrlTitle(title: String) {
        binding.tvLinkTitle.apply {
            isVisible = title.isNotEmpty()
            text = title
        }
    }

    private fun bindDescription(description: String) {
        binding.tvLinkDescription.apply {
            isVisible = description.isNotEmpty()
            text = description
        }
    }

    private fun bindUrlLink(url: String) {
        binding.tvLinkUrl.apply {
            isVisible = url.isNotEmpty()
            text = url
        }
    }

    private fun bindThumbnail(imageUrl: String) {
        binding.ivLinkThumbnail.apply {
            load(imageUrl) {
                placeholder(R.drawable.ic_url_preview_no_thumbnail)
                error(R.drawable.ic_url_preview_no_thumbnail)
                crossfade(true)
                allowHardware(false)
                listener(
                    onError = { _, throwable ->
                        setImageResource(R.drawable.ic_url_preview_no_thumbnail)
                        Timber.e("load url image error: ${throwable.toString()}")
                    }
                )
            }
        }
    }

    private fun clearMetaPreview() {
        currentUrlMetaInfo = null
        with(binding) {
            etUrlInput.text?.clear()
            tvLinkUrl.text = ""
            tvLinkDescription.text = ""
            tvLinkTitle.text = ""
            ivLinkThumbnail.setImageResource(R.drawable.ic_url_preview_no_thumbnail)
        }
    }

    private fun formatUrl(input: String): String {
        val trimmed = input.trim()
        val result = if (trimmed.startsWith(PREFIX_HTTP) || trimmed.startsWith(PREFIX_HTTPS)) {
            trimmed
        } else {
            "$PREFIX_HTTPS$trimmed"
        }
        Timber.d("formatted url: '$result'")
        return result
    }

    private fun showMetaPreview(isShow: Boolean) {
        binding.clMetaPreview.isVisible = isShow
    }

    private fun enablePositiveButton(isEnable: Boolean) {
        binding.acbPositive.apply {
            isEnabled = isEnable
            val textColorResId = if (isEnable) {
                R.color.url_preview_dialog_positive_enable_text_color
            } else {
                R.color.url_preview_dialog_positive_disable_text_color
            }
            setTextColor(ContextCompat.getColor(context, textColorResId))
        }
    }

    companion object {
        private const val PREFIX_HTTPS = "https://"
        private const val PREFIX_HTTP = "http://"
    }
}