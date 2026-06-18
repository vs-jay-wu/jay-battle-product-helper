package com.viewsonic.classswift.utils.extension

import android.content.Context
import android.content.Intent

inline fun <reified T : Any> Context.startActivity(block: Intent.() -> Unit = {}) {
    val intent = Intent(this, T::class.java).apply(block)
    startActivity(intent)
}