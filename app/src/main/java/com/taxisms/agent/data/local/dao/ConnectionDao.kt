package com.taxisms.agent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taxisms.agent.data.local.entity.ConnectionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Connection DAO - Server ulanish ma'lumotlari bilan ishlash
 */
@Dao
interface ConnectionDao {

    /** Yangi ulanish qo'shish */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: ConnectionEntity): Long

    /** Ulanish yangilash */
    @Update
    suspend fun update(connection: ConnectionEntity)

    /** Faol ulanishni olish */
    @Query("SELECT * FROM connections WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveConnection(): ConnectionEntity?

    /** Faol ulanishni kuzatish (Flow) */
    @Query("SELECT * FROM connections WHERE is_active = 1 LIMIT 1")
    fun observeActiveConnection(): Flow<ConnectionEntity?>

    /** Barcha ulanishlarni olish */
    @Query("SELECT * FROM connections ORDER BY created_at DESC")
    fun getAllConnections(): Flow<List<ConnectionEntity>>

    /** Barcha ulanishlarni nofaol qilish */
    @Query("UPDATE connections SET is_active = 0")
    suspend fun deactivateAll()

    /** Ulanishni o'chirish */
    @Query("DELETE FROM connections WHERE id = :id")
    suspend fun deleteById(id: Long)
}
