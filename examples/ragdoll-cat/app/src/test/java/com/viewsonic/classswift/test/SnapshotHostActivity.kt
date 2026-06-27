package com.viewsonic.classswift.test

import android.app.Activity
import android.os.Bundle

/**
 * Material-themed plain Activity used to host inflated views in Roborazzi
 * snapshot tests. Roborazzi's `View.captureRoboImage()` requires the view
 * to be attached to an Activity window before rendering.
 */
class SnapshotHostActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(com.google.android.material.R.style.Theme_MaterialComponents)
        super.onCreate(savedInstanceState)
    }
}
