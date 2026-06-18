package com.viewsonic.classswift.utils.extension

import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import java.io.Serializable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> AppCompatActivity.extraOrDefault(key: String, default: T) =
    object : ReadOnlyProperty<AppCompatActivity, T> {
        private var data: T? = null

        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: AppCompatActivity, property: KProperty<*>): T =
            data ?: intent?.extras?.run {
                (when (default) {
                    is Int -> getInt(key, default)
                    is Boolean -> getBoolean(key, default)
                    is Long -> getLong(key, default)
                    is Float -> getFloat(key, default)
                    is Double -> getDouble(key, default)
                    is String -> getString(key, default)
                    is Serializable -> getSerializableCompat(key) ?: default
                    is Parcelable -> getParcelableCompat(key) ?: default
                    else -> throw IllegalStateException("Unknown Type")
                } as T).also { data = it }
            } ?: throw IllegalStateException("No argument found")
    }

inline fun <reified T> AppCompatActivity.extraOrNull(key: String): ReadOnlyProperty<AppCompatActivity, T?> {
    return object : ReadOnlyProperty<AppCompatActivity, T?> {
        override fun getValue(thisRef: AppCompatActivity, property: KProperty<*>): T? {
            val extras = thisRef.intent?.extras ?: return null

            return when {
                T::class.java == Int::class.java -> extras.getInt(key) as T
                T::class.java == Boolean::class.java -> extras.getBoolean(key) as T
                T::class.java == Long::class.java -> extras.getLong(key) as T
                T::class.java == Float::class.java -> extras.getFloat(key) as T
                T::class.java == Double::class.java -> extras.getDouble(key) as T
                T::class.java == String::class.java -> extras.getString(key) as T
                Parcelable::class.java.isAssignableFrom(T::class.java) -> extras.getParcelableCompat(key) as? T
                Serializable::class.java.isAssignableFrom(T::class.java) -> extras.getSerializableCompat(key) as? T
                else -> throw IllegalStateException("Unsupported type: ${T::class.java}")
            }
        }
    }
}