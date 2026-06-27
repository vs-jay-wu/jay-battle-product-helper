package com.viewsonic.classswift.api.moshi.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import com.viewsonic.classswift.data.enum.MissionType

class MissionTypeMoshiAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): MissionType {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> MissionType.valueOfServerValue(reader.nextString())
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                MissionType.NONE
            }
            else -> {
                reader.skipValue()
                MissionType.NONE
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: MissionType?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.value(value.serverValue)
    }
}
