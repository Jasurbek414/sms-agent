package com.taxisms.agent.data.repository

import com.taxisms.agent.data.local.dao.SmsLogDao
import com.taxisms.agent.data.local.entity.SmsLogEntity
import com.taxisms.agent.util.Constants
import com.taxisms.agent.util.DateUtils
import com.taxisms.agent.util.Resource
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * SMS Repository Implementatsiyasi
 *
 * SMS yuborish va log saqlash operatsiyalarini amalga oshiradi.
 * TODO: SmsManager integratsiyasi keyingi bosqichda qo'shiladi.
 */
class SmsRepositoryImpl @Inject constructor(
    private val smsLogDao: SmsLogDao
) : SmsRepository {

    override suspend fun sendSms(
        phoneNumber: String,
        message: String,
        requestId: String
    ): Resource<Long> {
        return try {
            // SMS logni yaratish
            val smsLog = SmsLogEntity(
                requestId = requestId,
                phoneNumber = phoneNumber,
                messageText = message,
                status = Constants.SmsStatus.PENDING
            )
            val id = smsLogDao.insert(smsLog)
            Timber.d("SMS log yaratildi: id=$id, phone=$phoneNumber")

            // TODO: SmsManager orqali haqiqiy SMS yuborish
            Resource.Success(id)
        } catch (e: Exception) {
            Timber.e(e, "SMS yuborishda xatolik")
            Resource.Error(e.message ?: "SMS yuborish xatoligi")
        }
    }

    override suspend fun updateSmsStatus(
        smsId: Long,
        status: Int,
        errorCode: Int?,
        errorMessage: String?
    ) {
        smsLogDao.updateStatus(smsId, status, errorCode, errorMessage)
    }

    override suspend fun getNextPendingSms(status: Int): SmsLogEntity? {
        return smsLogDao.getNextPendingSms(status)
    }

    override suspend fun getById(id: Long): SmsLogEntity? {
        return smsLogDao.getById(id)
    }

    override suspend fun getByRequestId(requestId: String): SmsLogEntity? {
        return smsLogDao.getByRequestId(requestId)
    }

    override fun getAllLogs(): Flow<List<SmsLogEntity>> {
        return smsLogDao.getAllLogs()
    }

    override fun getLogsByStatus(status: Int): Flow<List<SmsLogEntity>> {
        return smsLogDao.getLogsByStatus(status)
    }

    override suspend fun cleanOldLogs(retentionDays: Int): Int {
        val threshold = DateUtils.daysAgo(retentionDays)
        return smsLogDao.deleteOldLogs(threshold)
    }

    override suspend fun clearAllLogs() {
        smsLogDao.clearAllLogs()
    }
}
