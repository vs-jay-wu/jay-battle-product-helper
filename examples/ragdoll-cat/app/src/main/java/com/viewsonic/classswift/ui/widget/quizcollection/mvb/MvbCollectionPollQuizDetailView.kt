package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet

/**
 * Poll detail view (single + multi vote) — VSFT-7268 AC-3.
 *
 * Detail-page rendering is identical to MC (A-F option grid, no correct-answer marker per AC-3).
 * Inherits from [MvbCollectionMultipleChoiceQuizDetailView] without overrides so the visual stays
 * in sync if MC's grid behavior evolves. The chip text differentiates Poll vs MC at the label level.
 */
class MvbCollectionPollQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MvbCollectionMultipleChoiceQuizDetailView(context, attrs, defStyleAttr)
