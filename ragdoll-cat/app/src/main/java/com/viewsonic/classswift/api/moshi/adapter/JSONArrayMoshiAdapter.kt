package com.viewsonic.classswift.api.moshi.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import org.json.JSONArray
import org.json.JSONObject


class JSONArrayMoshiAdapter {
    @ToJson
    fun toJson(value: JSONObject): String {
        return value.toString()
    }

    @FromJson
    fun fromJson(reader: JsonReader): JSONArray {
        return if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
            var encodedJson: String
            reader.nextSource().use { bufferedSource -> encodedJson = bufferedSource.readUtf8() }
            JSONArray(encodedJson)
        } else {
            throw Exception("Expected BEGIN_ARRAY but was ${reader.peek()}")
        }
    }
}