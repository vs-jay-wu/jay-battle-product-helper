package com.viewsonic.classswift.api.moshi

import com.viewsonic.classswift.api.moshi.factory.DefaultIfNullFactory
import com.viewsonic.classswift.api.moshi.adapter.JSONArrayMoshiAdapter
import com.viewsonic.classswift.api.moshi.adapter.JSONObjectMoshiAdapter
import com.viewsonic.classswift.api.moshi.adapter.MissionTypeMoshiAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.viewsonic.classswift.api.moshi.adapter.QuizAnswerDataAdapter
import java.lang.reflect.Type

object MoshiProvider {
    private val defaultAdapterList = arrayListOf(
        JSONObjectMoshiAdapter(),
        JSONArrayMoshiAdapter(),
        QuizAnswerDataAdapter(),
        MissionTypeMoshiAdapter()
    )

    /**
     * It would throw an exception if the non-null field is set to null from json data.
     */
    val moshiNormal: Moshi = getNormalBuilder().build()

    fun getNormalBuilder(): Moshi.Builder = Moshi.Builder()
        .apply {
            defaultAdapterList.forEach { adapter ->
                add(adapter)
            }
        }

    /**
     * It would preserve default value if that field is set to null from json data.
     */
    val moshiDefaultIfNull: Moshi = getDefaultIfNullBuilder().build()

    fun getDefaultIfNullBuilder(): Moshi.Builder = Moshi.Builder()
        .add(DefaultIfNullFactory())
        .apply {
            defaultAdapterList.forEach { adapter ->
                add(adapter)
            }
        }

    /**
     * Tool Functions
     */

    fun <T> fromJson(type: Class<T>, jsonData: String, isDefaultIfNullApplied: Boolean = false): T {
        return when (isDefaultIfNullApplied) {
            true ->
                moshiDefaultIfNull.adapter(type).fromJson(jsonData)
                    ?: throw JsonDataException("Parsing Error, jsonData: $jsonData")
            false ->
                moshiNormal.adapter(type).fromJson(jsonData)
                    ?: throw JsonDataException("Parsing Error, jsonData: $jsonData")
        }
    }
    fun <T> fromJson(type: Type, jsonData: String, isDefaultIfNullApplied: Boolean = false): T {
        return when (isDefaultIfNullApplied) {
            true ->
                moshiDefaultIfNull.adapter<T>(type).fromJson(jsonData)
                    ?: throw JsonDataException("Parsing Error, jsonData: $jsonData")
            false ->
                moshiNormal.adapter<T>(type).fromJson(jsonData)
                    ?: throw JsonDataException("Parsing Error, jsonData: $jsonData")
        }
    }

    inline fun <reified T> toJson(entity: T, isDefaultIfNullApplied: Boolean = false): String {
        return when (isDefaultIfNullApplied) {
            true ->
                moshiDefaultIfNull.adapter(T::class.java).toJson(entity)
            false ->
                moshiNormal.adapter(T::class.java).toJson(entity)
        }
    }

    inline fun <reified T> toJson(type: Type, entity: T, isDefaultIfNullApplied: Boolean = false): String {
        return when (isDefaultIfNullApplied) {
            true ->
                moshiDefaultIfNull.adapter<T>(type).toJson(entity)
            false ->
                moshiNormal.adapter<T>(type).toJson(entity)
        }
    }
}
