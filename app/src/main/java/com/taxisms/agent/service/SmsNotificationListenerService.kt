package com.taxisms.agent.service

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.taxisms.agent.data.repository.SettingsRepository
import com.taxisms.agent.data.repository.SmsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.regex.Pattern

/**
 * SmsNotificationListenerService - Telefon bildirishnomalarini (Notification) fonda kuzatadi.
 * Asosiy taksi dasturidan kelgan bildirishnoma matni ichidan mijoz telefon raqami
 * va yo'l haqi summasini avtomatik ajratib olib (regex), SMS yuboradi.
 */
class SmsNotificationListenerService : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceEntryPoint {
        fun smsRepository(): SmsRepository
        fun settingsRepository(): SettingsRepository
    }

    private val smsRepository by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java).smsRepository()
    }

    private val settingsRepository by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java).settingsRepository()
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    // O'zbekiston telefon raqamlari uchun Regex: +998901234567, 998901234567, 901234567
    private val phonePattern = Pattern.compile("(?:\\+?998)?\\s?[3-9]\\d\\s?\\d{3}\\s?\\d{2}\\s?\\d{2}")
    
    // Yo'l haqi summasini aniqlash: masalan 15 000, 15000, 15,000, 15.000 (ketidan so'm, som yoki sum kelishi mumkin)
    private val pricePattern = Pattern.compile("(\\d{1,3}(?:[\\s,.]\\d{3})*)\\s*(?:so'm|som|sum|сум|so`m)?", Pattern.CASE_INSENSITIVE)

    override fun onListenerConnected() {
        super.onListenerConnected()
        Timber.i("SmsNotificationListenerService ulandi")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Timber.i("SmsNotificationListenerService uzildi")
        serviceJob.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        serviceScope.launch {
            try {
                // Bildirishnomalarni o'qish yoqilganligini tekshirish
                val isEnabled = settingsRepository.getBoolean("notification_reader_enabled", false)
                if (!isEnabled) return@launch

                val packageName = sbn.packageName
                
                // Paket nomi filtri (ixtiyoriy, masalan faqat ma'lum taksi dasturi uchun)
                val packageFilter = settingsRepository.getValue("notification_package_filter", "")
                if (packageFilter.isNotEmpty() && !packageName.contains(packageFilter, ignoreCase = true)) {
                    return@launch
                }

                val notification = sbn.notification ?: return@launch
                val extras = notification.extras ?: return@launch
                val title = extras.getString("android.title", "") ?: ""
                val text = extras.getCharSequence("android.text", "").toString()
                
                val fullContent = "$title $text"
                Timber.d("Yangi bildirishnoma olindi: pkg=$packageName, content=$fullContent")

                // Kalit so'z filtri (masalan: safar, yakunlandi, yo'l haqi)
                val keywordFilter = settingsRepository.getValue("notification_keyword_filter", "")
                if (keywordFilter.isNotEmpty() && !fullContent.contains(keywordFilter, ignoreCase = true)) {
                    return@launch
                }

                // Telefon raqamini qidirish
                val phoneMatcher = phonePattern.matcher(fullContent)
                var extractedPhone: String? = null
                if (phoneMatcher.find()) {
                    extractedPhone = phoneMatcher.group().replace("\\s".toRegex(), "")
                    // Agar + belgisi bo'lmasa qo'shib qo'yamiz (Uzbekistan xalqaro formati uchun)
                    if (!extractedPhone.startsWith("+")) {
                        if (extractedPhone.startsWith("998")) {
                            extractedPhone = "+$extractedPhone"
                        } else {
                            extractedPhone = "+998$extractedPhone"
                        }
                    }
                }

                // Summani qidirish
                val priceMatcher = pricePattern.matcher(fullContent)
                var extractedPrice: String? = null
                while (priceMatcher.find()) {
                    val potentialPrice = priceMatcher.group(1)?.replace("[\\s,.]".toRegex(), "") ?: continue
                    val value = potentialPrice.toIntOrNull() ?: 0
                    // Summa mantiqiy bo'lishi kerak (masalan 2000 so'mdan 500 000 so'mgacha)
                    if (value in 2000..500000) {
                        extractedPrice = potentialPrice
                        break
                    }
                }

                Timber.d("Tahlil natijasi: Phone=$extractedPhone, Price=$extractedPrice")

                if (!extractedPhone.isNullOrEmpty() && !extractedPrice.isNullOrEmpty()) {
                    // Ikkala ma'lumot ham bitta bildirishnomada mavjud
                    sendExtractedSms(extractedPhone, extractedPrice)
                    settingsRepository.setValue("active_client_phone", "")
                } else if (!extractedPhone.isNullOrEmpty() && extractedPrice.isNullOrEmpty()) {
                    // Faqat telefon raqami bor (Safar boshlandi yoki buyurtma olindi)
                    settingsRepository.setValue("active_client_phone", extractedPhone)
                    Timber.d("Safar boshlandi: mijoz raqami keyingi yakunlov uchun saqlandi ($extractedPhone)")
                } else if (extractedPhone.isNullOrEmpty() && !extractedPrice.isNullOrEmpty()) {
                    // Faqat narx bor (Safar yakunlandi). Oldingi bosqichda saqlangan raqamni ishlatamiz
                    val storedPhone = settingsRepository.getValue("active_client_phone", "")
                    if (storedPhone.isNotEmpty()) {
                        sendExtractedSms(storedPhone, extractedPrice)
                        settingsRepository.setValue("active_client_phone", "") // Tozalash
                    } else {
                        Timber.w("Yo'l haqi aniqlandi ($extractedPrice), lekin faol saqlangan mijoz raqami topilmadi!")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Bildirishnomani tahlil qilishda xatolik")
            }
        }
    }

    private suspend fun sendExtractedSms(phone: String, price: String) {
        val template = settingsRepository.getValue(
            "finish_ride_template",
            "Yo'l haqi: {price} so'm. Rahmat!"
        )
        val messageText = template.replace("{price}", price)
        val requestId = "notif_" + UUID.randomUUID().toString().take(8)
        
        smsRepository.sendSms(phone, messageText, requestId)
        Timber.i("Bildirishnomadan avtomatik SMS navbatga olindi: Phone=$phone, Msg=$messageText")
    }
}
