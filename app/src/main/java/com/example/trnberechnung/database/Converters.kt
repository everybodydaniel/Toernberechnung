package com.example.trnberechnung.database

import androidx.room.TypeConverter
import com.example.trnberechnung.model.ChatMessageType
import com.example.trnberechnung.model.ChatThreadType
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromChatMessageType(value: String): ChatMessageType {
        return ChatMessageType.valueOf(value)
    }

    @TypeConverter
    fun chatMessageTypeToString(type: ChatMessageType): String {
        return type.name
    }

    @TypeConverter
    fun fromChatThreadType(value: String): ChatThreadType {
        return ChatThreadType.valueOf(value)
    }

    @TypeConverter
    fun chatThreadTypeToString(type: ChatThreadType): String {
        return type.name
    }
}
