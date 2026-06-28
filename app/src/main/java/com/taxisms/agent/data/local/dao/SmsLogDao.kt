package com.taxisms.agent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taxisms.agent.data.local.entity.SmsLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * SMS Log DAO - SMS xabarlar tarixi bilan ishlash
 */
@Dao
interface SmsLogDao {

    /** Yangi SMS log yozish */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(smsLog: SmsLogEntity): Long

    /** SMS log yangilash */
    @Update
    suspend fun update(smsLog: SmsLogEntity)

    /** SMS holatini yangilash */
    @Query("UPDATE sms_logs SET status = :status, error_code = :errorCode, error_message = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int, errorCode: Int?, errorMessage: String?)

    /** Barcha loglarni olish (oxirgisi birinchi) */
    @Query("SELECT * FROM sms_logs ORDER BY created_at DESC")
    fun getAllLogs(): Flow<List<SmsLogEntity>>

    /** Holat bo'yicha loglarni olish */
    @Query("SELECT * FROM sms_logs WHERE status = :status ORDER BY created_at DESC")
    fun getLogsByStatus(status: Int): Flow<List<SmsLogEntity>>

    /** Navbatdagi birinchi kutayotgan SMS ni olish */
    @Query("SELECT * FROM sms_logs WHERE status = :status ORDER BY created_at ASC LIMIT 1")
    suspend fun getNextPendingSms(status: Int): SmsLogEntity?

    /** Request ID bo'yicha logni topish */
    @Query("SELECT * FROM sms_logs WHERE request_id = :requestId LIMIT 1")
    suspend fun getByRequestId(requestId: String): SmsLogEntity?

    /** ID bo'yicha logni topish */
    @Query("SELECT * FROM sms_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SmsLogEntity?

    /** Eski loglarni o'chirish */
    @Query("DELETE FROM sms_logs WHERE created_at < :beforeTimestamp")
    suspend fun deleteOldLogs(beforeTimestamp: Long): Int

    /** Barcha loglarni o'chirish */
    @Query("DELETE FROM sms_logs")
    suspend fun clearAllLogs()

    /** Jami SMS sonini olish */
    @Query("SELECT COUNT(*) FROM sms_logs")
    fun getTotalCount(): Flow<Int>

    /** Holat bo'yicha SMS sonini olish */
    @Query("SELECT COUNT(*) FROM sms_logs WHERE status = :status")
    suspend fun getCountByStatus(status: Int): Int
}
