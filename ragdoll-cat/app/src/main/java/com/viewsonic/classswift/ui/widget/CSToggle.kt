package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import com.viewsonic.classswift.R

/**
 * ClassSwift Toggle component following VSDS design system.
 * Provides a styled switch/toggle with branded colors and consistent sizing.
 *
 * Design reference: Figma - ClassSwift Toggle
 * - Track: pill shape, 24dp height, 16dp radius
 * - Thumb: 20dp circular
 * - Checked: brand blue thumb, sky_100 track
 * - Unchecked: gray thumb, semi-transparent gray track
 */
class CSToggle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.switchStyle
) : SwitchCompat(context, attrs, defStyleAttr) {

    init {
        setTrackResource(R.drawable.selector_cstv_track)
        setThumbResource(R.drawable.selector_cstv_thumb)
        switchMinWidth = context.resources.getDimensionPixelSize(R.dimen.cstv_track_min_width)
        showText = false
    }
}
