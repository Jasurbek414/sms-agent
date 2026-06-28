package com.taxisms.agent.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taxisms.agent.data.repository.ConnectionRepository
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
 * Intent orqali kelgan SMS jo'natish va konfiguratsiya so'rovlarini qabul qiladi.
 *
 * Actions:
 * - "com.taxisms.agent.action.SEND_SMS_REQUEST"
 * - "com.taxisms.agent.action.CONNECT_TO_SERVER"
 * - "com.taxisms.agent.action.DISCONNECT_FROM_SERVER"
 */
@AndroidEntryPoint
class LocalSmsRequestReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsRepository: SmsRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var connectionRepository: ConnectionRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Timber.d("LocalSmsRequestReceiver: action=$action")

        if (action == Constants.Actions.SEND_SMS_REQUEST) {
            val phoneNumber = intent.getStringExtra(Constants.Extras.PHONE_NUMBER)
            var messageText = intent.getStringExtra(Constants.Extras.MESSAGE_TEXT)
            val price = intent.getStringExtra("price") // Haydovchi yo'nalishni yakunlagandagi summa
            val requestId = intent.getStringExtra(Constants.Extras.REQUEST_ID)

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
        } else if (action == Constants.Actions.CONNECT_TO_SERVER) {
            val serverUrl = intent.getStringExtra(Constants.Extras.SERVER_URL)
            val apiKey = intent.getStringExtra(Constants.Extras.API_KEY)
            val parkName = intent.getStringExtra("park_name") ?: "Lokal Ulanish"

            // Qo'shimcha sozlamalar (ixtiyoriy)
            val smsTemplate = intent.getStringExtra("sms_template")
            val packageFilter = intent.getStringExtra("package_filter")
            val keywordFilter = intent.getStringExtra("keyword_filter")
            val readerEnabled = intent.getStringExtra("reader_enabled") // "true" yoki "false"

            Timber.i("Lokal buyruq orqali server sozlamalarini o'rnatish so'raldi: URL=$serverUrl, Park=$parkName")

            if (serverUrl.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
                Timber.e("Lokal ulanish xatoligi: serverUrl yoki apiKey bo'sh")
                return
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Serverga ulanish parametrlarini saqlash
                    connectionRepository.connect(serverUrl, apiKey, parkName)

                    // Qo'shimcha sozlamalarni yangilash
                    if (!smsTemplate.isNullOrEmpty()) {
                        settingsRepository.setValue("finish_ride_template", smsTemplate)
                    }
                    if (!packageFilter.isNullOrEmpty()) {
                        settingsRepository.setValue("notification_package_filter", packageFilter)
                    }
                    if (!keywordFilter.isNullOrEmpty()) {
                        settingsRepository.setValue("notification_keyword_filter", keywordFilter)
                    }
                    if (!readerEnabled.isNullOrEmpty()) {
                        settingsRepository.setValue("notification_reader_enabled", readerEnabled)
                    }

                    Timber.i("Lokal sozlash muvaffaqiyatli yakunlandi. Sozlamalar faollashtirildi.")
                } catch (e: Exception) {
                    Timber.e(e, "Lokal sozlash jarayonida xatolik")
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == Constants.Actions.DISCONNECT_FROM_SERVER) {
            Timber.i("Lokal buyruq orqali serverdan uzilish so'raldi")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    connectionRepository.disconnect()
                    Timber.i("Lokal buyruq orqali serverdan uzildi")
                } catch (e: Exception) {
                    Timber.e(e, "Lokal serverdan uzish jarayonida xatolik")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
