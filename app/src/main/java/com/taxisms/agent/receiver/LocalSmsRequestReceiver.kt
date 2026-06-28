package com.taxisms.agent.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taxisms.agent.data.repository.SettingsRepository
import com.taxisms.agent.data.repository.SmsRepository
import com.taxisms.agent.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * LocalSmsRequestReceiver - Boshqa mahalliy ilovalardan (masalan, haydovchining asosiy taksi ilovasidan)
 * Intent orqali kelgan SMS jo'natish so'rovlarini qabul qiladi.
 *
 * Action: "com.taxisms.agent.action.SEND_SMS_REQUEST"
 * Qabul qiluvchi o'zgaruvchilar:
 * - phone_number: Mijoz telefon raqami
 * - message: SMS matni (agar to'liq matn yuborilsa)
 * - price: Yo'l haqi summasi (agar shablon bo'yicha yuborilmoqchi bo'lsa)
 * - request_id: Unikal so'rov identifikatori (ixtiyoriy)
 */
@AndroidEntryPoint
class LocalSmsRequestReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsRepository: SmsRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.Actions.SEND_SMS_REQUEST) {
            val phoneNumber = intent.getStringExtra(Constants.Extras.PHONE_NUMBER)
            var messageText = intent.getStringExtra(Constants.Extras.MESSAGE_TEXT)
            val price = intent.getStringExtra("price") // Haydovchi yo'nalishni yakunlagandagi summa
            var requestId = intent.getStringExtra(Constants.Extras.REQUEST_ID)

            Timber.d("Lokal SMS jo'natish so'rovi olindi: Phone=$phoneNumber, Msg=$messageText, Price=$price, RequestId=$requestId")

            if (phoneNumber.isNullOrEmpty()) {
                Timber.e("Lokal SMS xatoligi: Telefon raqam bo'sh")
                return
            }

            val finalRequestId = requestId ?: ("local_" + UUID.randomUUID().toString().take(8))

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    var finalMessageText = messageText
                    // Agar yo'l haqi summasi uzatilgan bo'lsa va to'liq matn bo'lmasa, shablondan foydalanamiz
                    if (!price.isNullOrEmpty()) {
                        val template = settingsRepository.getValue(
                            "finish_ride_template",
                            "Yo'l haqi: {price} so'm. Rahmat!"
                        )
                        finalMessageText = template.replace("{price}", price)
                    }

                    if (finalMessageText.isNullOrEmpty()) {
                        Timber.e("Lokal SMS xatoligi: SMS matni bo'sh va shablon topilmadi")
                        return@launch
                    }

                    // SMS ni Room local database-ga QUEUED statusida yozamiz
                    val result = smsRepository.sendSms(phoneNumber, finalMessageText, finalRequestId)
                    when (result) {
                        is com.taxisms.agent.util.Resource.Success -> {
                            Timber.i("Lokal SMS navbatga muvaffaqiyatli qo'shildi: id=${result.data}")
                        }
                        is com.taxisms.agent.util.Resource.Error -> {
                            Timber.e("Lokal SMS-ni navbatga qo'shishda xato: ${result.message}")
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Lokal SMS-ni qayta ishlashda xatolik")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
