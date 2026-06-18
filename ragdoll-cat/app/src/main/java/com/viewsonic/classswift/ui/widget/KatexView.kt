package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.viewsonic.classswift.R
import com.viewsonic.classswift.ui.widget.katex.KatexRenderAction
import com.viewsonic.classswift.ui.widget.katex.KatexRenderSnapshot
import com.viewsonic.classswift.ui.widget.katex.KatexRenderUpdatePolicy
import com.viewsonic.classswift.utils.extension.pxToDp
import timber.log.Timber
import kotlin.math.ceil

class KatexView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var webView: WebView = WebView(context)
    private val touchOverlay: View = View(context)

    // Styling
    private var textColorCss: String = "rgba(0,0,0,1)"
    private var bgColorCss: String = "transparent"
    private var textStyleCss: String = "normal"

    // Css Px = Android Dp
    private var textSizeCssPx: Float = 16f

    private var contentGravity: Int = Gravity.START or Gravity.TOP
    private var maxLines: Int = 0

    private var lastText: String? = null
    private var isPageLoaded: Boolean = false
    private var katexFontBaseUrl: String = "file:///android_asset/katex/fonts/"
    private val renderUpdatePolicy = KatexRenderUpdatePolicy()

    private val katexCssTemplate: String by lazy {
        context.assets.open("katex/katex.min.css").bufferedReader().use { it.readText() }
    }

    init {
        applyTextAppearance(attrs, defStyleAttr)
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(touchOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        applyGravityToWebView()

        // Background: keep transparent by default
        webView.setBackgroundColor(Color.TRANSPARENT)

        // No scrollbars; we size to content height
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        if (!isInEditMode) {
            // Web settings
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.loadWithOverviewMode = true
            webView.settings.useWideViewPort = true

            // Security hardening (recommended)
            webView.settings.allowFileAccess = false
            webView.settings.allowContentAccess = false
            webView.settings.javaScriptCanOpenWindowsAutomatically = false
        }

        // Block all touch events from the WebView and forward click events to the parent view.
        touchOverlay.setOnClickListener { performClick() }

        webView.webViewClient = object : WebViewClient() {}
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            webView.onResume()
            lastText?.let { text ->
                val shouldTryRender = text.isNotEmpty()
                if (shouldTryRender) {
                    post {
                        if (!isAttachedToWindow) {
                            return@post
                        }
                        if (!isPageLoaded) {
                            setText(text, true)
                            return@post
                        }
                        isRenderedSuccessful { isSuccessful ->
                            if (!isSuccessful) {
                                setText(text, true)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            webView.stopLoading()
            webView.onPause()
        }
        super.onDetachedFromWindow()
    }

    fun setText(text: String, isForced: Boolean = false) {
        val isNeedToReload = text != lastText
        if (isNeedToReload || isForced) {
            lastText = text
            loadText(text)
        }
    }

    fun isRenderedSuccessful(onResult: (Boolean) -> Unit) {
        if (!isPageLoaded || lastText.isNullOrBlank()) {
            onResult(false)
            return
        }
        val js = """
            (function() {
                if (document.readyState !== "complete") { return false; }
                var content = document.getElementById("content");
                if (!content) { return false; }
                var katexCount = content.querySelectorAll(".katex").length;
                var text = (content.textContent || "").trim();
                var rect = content.getBoundingClientRect();
                var hasHeight = rect && rect.height > 0;
                return hasHeight && (katexCount > 0 || text.length > 0);
            })()
        """.trimIndent()
        webView.evaluateJavascript(js) { value ->
            onResult(value == "true")
        }
    }

    fun setTextWithStyleAndColor(text: String, style: Int, @ColorInt textColor: Int) {
        val nextStyleCss = styleToCss(style)
        val nextColorCss = colorToCss(textColor)
        val currentSnapshot = lastText?.let {
            KatexRenderSnapshot(
                text = it,
                styleCss = textStyleCss,
                colorCss = textColorCss
            )
        }
        val renderAction = renderUpdatePolicy.decide(
            currentSnapshot = currentSnapshot,
            nextText = text,
            nextStyleCss = nextStyleCss,
            nextColorCss = nextColorCss
        )

        textStyleCss = nextStyleCss
        textColorCss = nextColorCss
        lastText = text

        when (renderAction) {
            KatexRenderAction.RELOAD_CONTENT -> loadText(text)
            KatexRenderAction.APPLY_STYLE_ONLY -> applyStyleToWebView()
            KatexRenderAction.NONE -> Unit
        }
    }

    fun refreshContentHeight() {
        if (!isPageLoaded) {
            return
        }
        post { updateHeightFromContent(webView, true) }
    }

    private fun applyTextAppearance(attrs: AttributeSet?, defStyleAttr: Int) {
        if (attrs == null) {
            return
        }

        val attributeSet = context.obtainStyledAttributes(
            attrs,
            R.styleable.KatexView,
            defStyleAttr,
            0
        )
        try {
            if (attributeSet.hasValue(R.styleable.KatexView_android_textColor)) {
                textColorCss = colorToCss(
                    attributeSet.getColor(
                        R.styleable.KatexView_android_textColor,
                        Color.BLACK
                    )
                )
            }
            if (attributeSet.hasValue(R.styleable.KatexView_android_textSize)) {
                val px = attributeSet.getDimension(
                    R.styleable.KatexView_android_textSize,
                    0f
                )
                if (px > 0f) {
                    textSizeCssPx = px.pxToDp()
                }
            }
            if (attributeSet.hasValue(R.styleable.KatexView_android_textStyle)) {
                textStyleCss = if (
                    (attributeSet.getInt(
                        R.styleable.KatexView_android_textStyle,
                        Typeface.NORMAL
                    ) and Typeface.BOLD) != 0
                ) {
                    "bold"
                } else {
                    "normal"
                }
            }
            if (attributeSet.hasValue(R.styleable.KatexView_android_gravity)) {
                contentGravity = attributeSet.getInt(
                    R.styleable.KatexView_android_gravity,
                    Gravity.START or Gravity.TOP
                )
            }
            if (attributeSet.hasValue(R.styleable.KatexView_android_maxLines)) {
                maxLines = attributeSet.getInt(R.styleable.KatexView_android_maxLines, 0)
            }
        } finally {
            attributeSet.recycle()
        }
    }

    private fun loadText(input: String) {
        isPageLoaded = false
        val normalized = normalizeBackslashesIfNeeded(input)
        val bodyHtml = plainTextToHtml(normalized)
        val html = buildHtml(bodyHtml)

        // After KaTeX renders, measure height and set WRAP_CONTENT.
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                isPageLoaded = true
                applyStyleToWebView()
                // Ensure render runs even if window load listener didn't fire or assets were late.
                view.evaluateJavascript(
                    "if (window.renderKatexNow) { window.renderKatexNow(); }",
                    null
                )
                post { updateHeightFromContent(view) }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Timber.e("[onReceivedError] : $lastText -> $error")
                super.onReceivedError(view, request, error)
            }
        }

        webView.loadDataWithBaseURL(
            "file:///android_asset/",
            html,
            "text/html",
            "utf-8",
            null
        )
    }

    /**
     * If the backend sends double-escaped backslashes ("\\sqrt"),
     * normalize to single backslashes ("\sqrt") for KaTeX.
     *
     * Heuristic: If we see many occurrences of "\\", we convert them.
     */
    private fun normalizeBackslashesIfNeeded(text: String): String {
        // Count occurrences of double backslashes. If present, convert.
        // This keeps normal strings unchanged.
        return if (text.contains("\\\\")) text.replace("\\\\", "\\") else text
    }

    /**
     * Convert plain text into safe HTML while preserving LaTeX delimiters and backslashes.
     */
    private fun plainTextToHtml(text: String): String {
        val escaped = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

        // Preserve line breaks
        return escaped.replace("\n", "<br/>")
    }

    private fun buildHtml(bodyHtml: String): String {
        val katexCss = buildKatexCss(katexFontBaseUrl)
        // NOTE: If your content can include currency like "$7",
        // you may want to remove the single-$ delimiter and rely on \( \) and \[ \] only.
        return """
            <!doctype html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <style>
                $katexCss
              </style>
              <script defer src="file:///android_asset/katex/katex.min.js"></script>
              <script defer src="file:///android_asset/katex/contrib/auto-render.min.js"></script>

              <style>
                :root {
                  --katex-color: $textColorCss;
                  --katex-weight: $textStyleCss;
                  --katex-align: ${gravityToHorizontalAlignCss(contentGravity)};
                  --katex-vertical-align: ${gravityToVerticalAlignCss(contentGravity)};
                }

                html, body {
                  margin: 0;
                  padding: 0;
                  background: $bgColorCss;
                  color: var(--katex-color);
                  font-size: ${textSizeCssPx}px;
                  font-weight: var(--katex-weight);
                  line-height: 1.35;
                  text-align: var(--katex-align);

                  /* Wrap normal text */
                  overflow-wrap: anywhere;
                  word-break: break-word;
                }

                #content {
                  width: 100%;
                }

                

                /* Ensure KaTeX inherits color */
                .katex, .katex * { color: inherit; }
                .katex { font-weight: var(--katex-weight); }

                /* Display math may overflow; allow horizontal scroll for that block */
                .katex-display {
                  overflow-x: auto;
                  overflow-y: hidden;
                  -webkit-overflow-scrolling: touch;
                }
              </style>
            </head>

            <body>
              <div id="content">$bodyHtml</div>

              <script>
                window.renderKatexNow = function() {
                  try {
                    var content = document.getElementById("content");
                    if (!content || typeof renderMathInElement !== "function") {
                      return false;
                    }
                    renderMathInElement(content, {
                      throwOnError: false,
                      delimiters: [
                        {left: "$$", right: "$$", display: true},
                        {left: "$", right: "$", display: false},
                        {left: "\\\\(", right: "\\\\)", display: false},
                        {left: "\\\\[", right: "\\\\]", display: true}
                      ]
                    });
                    return true;
                  } catch (e) {
                    return false;
                  }
                };
                window.addEventListener("load", function() {
                  window.renderKatexNow();
                });

                window.setKatexStyle = function(color, weight) {
                  document.documentElement.style.setProperty("--katex-color", color);
                  document.documentElement.style.setProperty("--katex-weight", weight);
                };

                window.setKatexGravity = function(align, verticalAlign, enableVerticalAlign) {
                  document.documentElement.style.setProperty("--katex-align", align);
                  document.documentElement.style.setProperty("--katex-vertical-align", verticalAlign);
                  if (enableVerticalAlign) {
                    document.body.style.display = "flex";
                    document.body.style.flexDirection = "column";
                    document.body.style.justifyContent = verticalAlign;
                    document.body.style.minHeight = "100%";
                  } else {
                    document.body.style.display = "block";
                    document.body.style.justifyContent = "flex-start";
                    document.body.style.minHeight = "0";
                  }
                };

                window.setKatexTruncate = function(maxLines) {
                  var content = document.getElementById("content");
                  if (!content) {
                    return;
                  }
                  if (maxLines > 0) {
                    content.style.overflow = "hidden";
                    content.style.display = "-webkit-box";
                    content.style.webkitBoxOrient = "vertical";
                    content.style.webkitLineClamp = String(maxLines);
                    content.style.whiteSpace = "normal";
                    content.style.textOverflow = "clip";
                  } else {
                    content.style.overflow = "visible";
                    content.style.display = "block";
                    content.style.webkitBoxOrient = "initial";
                    content.style.webkitLineClamp = "initial";
                    content.style.whiteSpace = "normal";
                    content.style.textOverflow = "clip";
                  }
                };
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun colorToCss(@ColorInt color: Int): String {
        val a = Color.alpha(color) / 255f
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return if (a >= 1f) String.format("#%02X%02X%02X", r, g, b) else "rgba($r,$g,$b,$a)"
    }

    private fun buildKatexCss(fontBaseUrl: String): String {
        val normalizedBaseUrl = if (fontBaseUrl.endsWith("/")) fontBaseUrl else "$fontBaseUrl/"
        return katexCssTemplate.replace("url(fonts/", "url(${normalizedBaseUrl}")
    }

    private fun styleToCss(style: Int): String {
        return if ((style and Typeface.BOLD) != 0) "bold" else "normal"
    }

    private fun applyStyleToWebView() {
        if (!isPageLoaded) {
            return
        }

        val safeColor = textColorCss.replace("'", "\\'")
        val safeWeight = textStyleCss.replace("'", "\\'")
        val safeAlign = gravityToHorizontalAlignCss(contentGravity).replace("'", "\\'")
        val safeVerticalAlign = gravityToVerticalAlignCss(contentGravity).replace("'", "\\'")
        webView.evaluateJavascript(
            "if (window.setKatexStyle) { window.setKatexStyle('$safeColor', '$safeWeight'); }",
            null
        )
        val enableVerticalAlign = layoutParams?.height != ViewGroup.LayoutParams.WRAP_CONTENT
        webView.evaluateJavascript(
            "if (window.setKatexGravity) { window.setKatexGravity('$safeAlign', '$safeVerticalAlign', $enableVerticalAlign); }",
            null
        )
        applyTruncationToWebView(true)
    }

    private fun applyTruncationToWebView(updateHeight: Boolean = false) {
        if (!isPageLoaded) {
            return
        }
        val js = "(function(){if (window.setKatexTruncate) { window.setKatexTruncate($maxLines); } return true;})()"
        webView.evaluateJavascript(js) {
            if (updateHeight) {
                updateHeightFromContent(webView)
            }
        }
    }

    private fun updateHeightFromContent(view: WebView, isForce: Boolean = false) {
        if (layoutParams?.height != ViewGroup.LayoutParams.WRAP_CONTENT && !isForce) {
            return
        }
        // Measure document height in CSS pixels; convert to device px via density.
        // Use document.documentElement for better compatibility.
        val heightJs = if (maxLines > 0) {
            "(function(){var el=document.getElementById('content'); if(!el){return 0;} var rect=el.getBoundingClientRect(); return Math.max(rect.height, 0);})()"
        } else {
            "(function(){var el=document.getElementById('content'); if(!el){return 0;} var rect=el.getBoundingClientRect(); var h=Math.max(rect.height, 0); if(h>0){return h;} return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);})()"
        }
        view.evaluateJavascript(heightJs) { value ->
            val cssPx = value.trim('"').toFloatOrNull() ?: return@evaluateJavascript
            val heightPx = ceil(cssPx * resources.displayMetrics.density).toInt().coerceAtLeast(1)
            val lp = view.layoutParams
            if (lp.height != heightPx) {
                lp.height = heightPx
                view.layoutParams = lp
            }
        }
    }

    private fun applyGravityToWebView() {
        val layoutParams = webView.layoutParams as LayoutParams
        layoutParams.gravity = contentGravity
        webView.layoutParams = layoutParams
        applyStyleToWebView()
    }

    private fun gravityToHorizontalAlignCss(gravity: Int): String {
        val absGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
        return when (absGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.CENTER_HORIZONTAL -> "center"
            Gravity.RIGHT -> "right"
            else -> "left"
        }
    }

    private fun gravityToVerticalAlignCss(gravity: Int): String {
        return when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.CENTER_VERTICAL -> "center"
            Gravity.BOTTOM -> "flex-end"
            else -> "flex-start"
        }
    }

    fun release() {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.webViewClient = WebViewClient()
        removeView(webView)
        webView.removeAllViews()
        webView.destroy()
    }
}
