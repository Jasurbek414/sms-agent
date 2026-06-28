package com.taxisms.agent.data.repository

import com.taxisms.agent.data.local.entity.SmsLogEntity
import com.taxisms.agent.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * SMS Repository Interfeysi
 *
 * SMS yuborish, log saqlash va holat kuzatish operatsiyalari.
 */
interface SmsRepository {

    /** SMS yuborish */
    suspend fun sendSms(phoneNumber: String, message: String, requestId: String): Resource<Long>

    /** SMS holatini yangilash */
    suspend fun updateSmsStatus(smsId: Long, status: Int, errorCode: Int? = null, errorMessage: String? = null)

    /** Navbatdagi birinchi kutayotgan SMS ni olish */
    suspend fun getNextPendingSms(status: Int): SmsLogEntity?

    /** ID bo'yicha logni topish */
    suspend fun getById(id: Long): SmsLogEntity?

    /** Request ID bo'yicha logni topish */
    suspend fun getByRequestId(requestId: String): SmsLogEntity?

    /** Barcha SMS loglarni olish */
    fun getAllLogs(): Flow<List<SmsLogEntity>>

    /** Holat bo'yicha loglarni olish */
    fun getLogsByStatus(status: Int): Flow<List<SmsLogEntity>>

    /** Eski loglarni tozalash */
    suspend fun cleanOldLogs(retentionDays: Int): Int

    /** Barcha loglarni tozalash */
    suspend fun clearAllLogs()
}
