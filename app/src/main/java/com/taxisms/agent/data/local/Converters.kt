package com.taxisms.agent.data.local

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room TypeConverter-lar
 * Room to'g'ridan-to'g'ri qo'llab-quvvatlamaydigan turlarni konvertatsiya qilish
 */
class Converters {

    /** Long -> Date */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /** Date -> Long */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
