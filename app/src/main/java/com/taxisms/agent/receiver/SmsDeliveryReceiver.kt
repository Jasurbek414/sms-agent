package com.taxisms.agent.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taxisms.agent.data.repository.SmsRepository
import com.taxisms.agent.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import com.taxisms.agent.service.SmsAgentService
import javax.inject.Inject

/**
 * SmsDeliveryReceiver - SMS yetkazib berilganlik hisobotini (Delivery Report) qabul qiluvchi.
 * Operator mijoz telefoniga xabar tushganini tasdiqlaganida ishlaydi.
 */
@AndroidEntryPoint
class SmsDeliveryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsRepository: SmsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.Actions.SMS_DELIVERED) {
            val smsId = intent.getLongExtra(Constants.Extras.SMS_ID, -1L)
            if (smsId == -1L) {
                Timber.w("SmsDeliveryReceiver: SMS_ID topilmadi")
                return
            }

            Timber.d("SMS yetkazilganlik hisoboti qabul qilindi: id=$smsId")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // SMS holatini DELIVERED ga o'zgartirish
                    smsRepository.updateSmsStatus(smsId, Constants.SmsStatus.DELIVERED)
                    Timber.i("SMS yetkazildi: id=$smsId")

                    // Serverga holatni xabar qilish
                    val serviceIntent = Intent(context, SmsAgentService::class.java).apply {
                        action = Constants.Actions.REPORT_STATUS
                        putExtra(Constants.Extras.SMS_ID, smsId)
                        putExtra(Constants.Extras.SMS_STATUS, Constants.SmsStatus.DELIVERED)
                    }
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
