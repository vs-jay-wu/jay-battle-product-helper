package com.viewsonic.classswift.api.moshi.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import org.json.JSONObject


class JSONObjectMoshiAdapter {
    @ToJson
    fun toJson(value: JSONObject): String {
        return value.toString()
    }

    @FromJson
    fun fromJson(reader: JsonReader): JSONObject {
        return if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
            var encodedJson: String
            reader.nextSource().use { bufferedSource -> encodedJson = bufferedSource.readUtf8() }
            JSONObject(encodedJson)
        } else {
            throw Exception("Expected BEGIN_OBJECT but was ${reader.peek()}")
        }
    }
}