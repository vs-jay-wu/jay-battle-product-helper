package com.viewsonic.classswift.test

import android.app.Application

/**
 * Empty Application for Robolectric snapshot tests. Bypasses
 * ClassSwiftApplication's Koin startup so multiple tests in the same JVM
 * don't blow up with KoinAppAlreadyStartedException. Snapshot tests inflate
 * layouts directly without needing the DI graph.
 */
class TestApplication : Application()
