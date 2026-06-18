package com.viewsonic.classswift.utils.extension

import android.content.Intent
import android.os.Build
import android.os.Bundle
import timber.log.Timber
import java.io.Serializable

inline fun <reified T : Serializable> Intent.getSerializableCompat(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T
}

fun Intent.dump(tag: String = "[B]") {
    Timber.tag(tag).d( "===== Intent Dump Start =====")
    Timber.tag(tag).d( "Action: $action")
    Timber.tag(tag).d( "Data: ${dataString}")
    Timber.tag(tag).d( "Scheme: ${data?.scheme}")
    Timber.tag(tag).d( "Host: ${data?.host}")
    Timber.tag(tag).d( "Path: ${data?.path}")
    Timber.tag(tag).d( "Query: ${data?.query}")
    Timber.tag(tag).d( "Type: $type")
    Timber.tag(tag).d( "Package: $`package`")
    Timber.tag(tag).d( "Component: $component")
    Timber.tag(tag).d( "Flags: ${flags.toHexFlags()}")
    Timber.tag(tag).d( "Categories: ${categories?.joinToString() ?: "None"}")
    dumpExtras(extras = extras)

    Timber.tag(tag).d( "===== Intent Dump End =====")
}

private fun dumpExtras(tag: String = "[B]", extras: Bundle?) {
    if (extras == null) {
        Timber.tag(tag).d( "Extras: None")
        return
    }

    Timber.tag(tag).d( "Extras:")
    for (key in extras.keySet()) {
        try {
            when (val value = extras.get(key)) {
                is Bundle -> {
                    Timber.tag(tag).d( "  [$key] -> Bundle")
                    dumpExtras(extras = value)
                }
                is Array<*> -> {
                    Timber.tag(tag).d( "  [$key] -> Array(${value.joinToString()})")
                }
                is IntArray -> {
                    Timber.tag(tag).d( "  [$key] -> IntArray(${value.joinToString()})")
                }
                is LongArray -> {
                    Timber.tag(tag).d( "  [$key] -> LongArray(${value.joinToString()})")
                }
                is BooleanArray -> {
                    Timber.tag(tag).d( "  [$key] -> BooleanArray(${value.joinToString()})")
                }
                else -> {
                    Timber.tag(tag).d( "  [$key] -> $value (${value?.javaClass?.simpleName})")
                }
            }
        } catch (e: Exception) {
            Timber.tag(tag).w(e, "  [$key] -> <unreadable extra>")
        }
    }
}

private fun Int.toHexFlags(): String =
    "0x${toString(16)}"
