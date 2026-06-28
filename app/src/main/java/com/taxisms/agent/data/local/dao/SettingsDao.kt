package com.taxisms.agent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.taxisms.agent.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Settings DAO - Ilova sozlamalari bilan ishlash
 */
@Dao
interface SettingsDao {

    /** Sozlama saqlash/yangilash */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: SettingsEntity)

    /** Kalit bo'yicha sozlama olish */
    @Query("SELECT setting_value FROM settings WHERE setting_key = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    /** Kalit bo'yicha sozlamani kuzatish (Flow) */
    @Query("SELECT setting_value FROM settings WHERE setting_key = :key LIMIT 1")
    fun observeValue(key: String): Flow<String?>

    /** Barcha sozlamalarni olish */
    @Query("SELECT * FROM settings ORDER BY setting_key")
    fun getAll(): Flow<List<SettingsEntity>>

    /** Sozlamani o'chirish */
    @Query("DELETE FROM settings WHERE setting_key = :key")
    suspend fun delete(key: String)

    /** Barcha sozlamalarni tozalash */
    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}
