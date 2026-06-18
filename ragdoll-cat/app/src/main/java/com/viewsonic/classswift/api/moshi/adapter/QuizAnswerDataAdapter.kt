package com.viewsonic.classswift.api.moshi.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import com.viewsonic.classswift.api.response.data.QuizAnswerData

class QuizAnswerDataAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): QuizAnswerData? {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> QuizAnswerData.Text(reader.nextString())
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = mutableListOf<Int>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(reader.nextInt())
                }
                reader.endArray()
                QuizAnswerData.Numbers(list)
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, quizAnswerData: QuizAnswerData?) {
        when (quizAnswerData) {
            is QuizAnswerData.Text -> writer.value(quizAnswerData.content)
            is QuizAnswerData.Numbers -> {
                writer.beginArray()
                quizAnswerData.list.forEach { writer.value(it) }
                writer.endArray()
            }
            null -> writer.nullValue()
        }
    }
}