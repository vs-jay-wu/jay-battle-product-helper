package com.viewsonic.classswift.ui.widget.katex

data class KatexRenderSnapshot(
    val text: String,
    val styleCss: String,
    val colorCss: String
)

enum class KatexRenderAction {
    RELOAD_CONTENT,
    APPLY_STYLE_ONLY,
    NONE
}

class KatexRenderUpdatePolicy {
    fun decide(
        currentSnapshot: KatexRenderSnapshot?,
        nextText: String,
        nextStyleCss: String,
        nextColorCss: String
    ): KatexRenderAction {
        if (currentSnapshot == null) {
            return KatexRenderAction.RELOAD_CONTENT
        }
        if (currentSnapshot.text != nextText) {
            return KatexRenderAction.RELOAD_CONTENT
        }
        val isStyleChanged = currentSnapshot.styleCss != nextStyleCss
        val isColorChanged = currentSnapshot.colorCss != nextColorCss
        if (isStyleChanged || isColorChanged) {
            return KatexRenderAction.APPLY_STYLE_ONLY
        }
        return KatexRenderAction.NONE
    }
}
