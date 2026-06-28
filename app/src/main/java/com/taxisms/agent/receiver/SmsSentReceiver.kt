package com.taxisms.agent.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.taxisms.agent.data.repository.SmsRepository
import com.taxisms.agent.sender.SmsRateLimiter
import com.taxisms.agent.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import com.taxisms.agent.service.SmsAgentService
import javax.inject.Inject

/**
 * SmsSentReceiver - SMS yuborish natijasini qabul qiluvchi BroadcastReceiver.
 * Android OS dan kelgan yuborish javob kodiga ko'ra Room bazadagi holatni yangilaydi.
 */
@AndroidEntryPoint
class SmsSentReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsRepository: SmsRepository

    @Inject
    lateinit var rateLimiter: SmsRateLimiter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.Actions.SMS_SENT) {
            val smsId = intent.getLongExtra(Constants.Extras.SMS_ID, -1L)
            if (smsId == -1L) {
                Timber.w("SmsSentReceiver: SMS_ID topilmadi")
                return
            }

            val resultCode = resultCode
            Timber.d("SMS yuborish natijasi: id=$smsId, resultCode=$resultCode")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (resultCode == Activity.RESULT_OK) {
                        // Muvaffaqiyatli yuborildi
                        smsRepository.updateSmsStatus(smsId, Constants.SmsStatus.SENT)
                        rateLimiter.recordSmsSent() // Quota/Limit hisobi
                        Timber.i("SMS muvaffaqiyatli yuborildi: id=$smsId")

                        // Serverga holatni xabar qilish
                        val serviceIntent = Intent(context, SmsAgentService::class.java).apply {
                            action = Constants.Actions.REPORT_STATUS
                            putExtra(Constants.Extras.SMS_ID, smsId)
                            putExtra(Constants.Extras.SMS_STATUS, Constants.SmsStatus.SENT)
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        // Yuborishda xatolik yuz berdi
                        val errorCode = when (resultCode) {
                            SmsManager.RESULT_ERROR_NO_SERVICE -> Constants.ErrorCodes.SMS_NO_SERVICE
                            SmsManager.RESULT_ERROR_RADIO_OFF -> Constants.ErrorCodes.SMS_RADIO_OFF
                            SmsManager.RESULT_ERROR_NULL_PDU -> Constants.ErrorCodes.SMS_NULL_PDU
                            else -> Constants.ErrorCodes.SMS_GENERIC_FAILURE
                        }
                        val errorMessage = "Android SMS jo'natish xatolik kodi: $resultCode"
                        smsRepository.updateSmsStatus(smsId, Constants.SmsStatus.FAILED, errorCode, errorMessage)
                        Timber.e("SMS yuborishda xato: id=$smsId, kod=$errorCode, xabar=$errorMessage")

                        // Serverga holatni xabar qilish
                        val serviceIntent = Intent(context, SmsAgentService::class.java).apply {
                            action = Constants.Actions.REPORT_STATUS
                            putExtra(Constants.Extras.SMS_ID, smsId)
                            putExtra(Constants.Extras.SMS_STATUS, Constants.SmsStatus.FAILED)
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
