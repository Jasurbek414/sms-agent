package com.taxisms.agent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.taxisms.agent.data.local.dao.ConnectionDao
import com.taxisms.agent.data.local.dao.SettingsDao
import com.taxisms.agent.data.local.dao.SmsLogDao
import com.taxisms.agent.data.local.entity.ConnectionEntity
import com.taxisms.agent.data.local.entity.SettingsEntity
import com.taxisms.agent.data.local.entity.SmsLogEntity
import com.taxisms.agent.util.Constants

/**
 * SMS Agent Platform - Room Ma'lumotlar Bazasi
 *
 * Ilovaning barcha mahalliy ma'lumotlarini saqlaydi:
 * - SMS loglar (yuborilgan xabarlar tarixi)
 * - Ulanish ma'lumotlari (server konfiguratsiyasi)
 * - Ilova sozlamalari
 */
@Database(
    entities = [
        SmsLogEntity::class,
        ConnectionEntity::class,
        SettingsEntity::class
    ],
    version = Constants.Database.DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /** SMS log yozuvlari uchun DAO */
    abstract fun smsLogDao(): SmsLogDao

    /** Server ulanish ma'lumotlari uchun DAO */
    abstract fun connectionDao(): ConnectionDao

    /** Sozlamalar uchun DAO */
    abstract fun settingsDao(): SettingsDao
}
