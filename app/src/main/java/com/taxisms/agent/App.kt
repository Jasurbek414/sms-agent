package com.taxisms.agent

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.taxisms.agent.util.Constants
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * SMS Agent Platform - Asosiy Application sinfi
 *
 * Bu sinf ilovaning hayot siklini boshqaradi:
 * - Hilt DI konteynerini ishga tushirish
 * - Timber logging tizimini sozlash
 * - WorkManager-ni manual ravishda ishga tushirish
 * - Bildirishnoma kanallarini yaratish
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider {

    // WorkManager uchun Hilt worker factory
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Logging tizimini ishga tushirish
        initTimber()

        // Bildirishnoma kanallarini yaratish
        createNotificationChannels()

        Timber.i("SMS Agent Platform ishga tushirildi")
    }

    // =========================================================================
    // WorkManager Konfiguratsiyasi
    // =========================================================================

    /**
     * WorkManager uchun maxsus konfiguratsiya
     * Hilt orqali Worker-larga dependency injection qilish imkonini beradi
     */
    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.ENABLE_LOGGING) android.util.Log.DEBUG
                else android.util.Log.ERROR
            )
            .build()
    }

    // =========================================================================
    // Timber Logging Tizimi
    // =========================================================================

    /**
     * Timber logging kutubxonasini sozlash
     * Debug rejimda - to'liq loglar
     * Release rejimda - faqat xatoliklar (crash report uchun)
     */
    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            // Debug rejim: to'liq debug tree
            Timber.plant(Timber.DebugTree())
            Timber.d("Debug logging yoqildi")
        } else {
            // Release rejim: faqat muhim xatoliklarni qayd qilish
            Timber.plant(ReleaseTree())
        }
    }

    /**
     * Release rejim uchun maxsus Timber Tree
     * Faqat WARNING, ERROR va WTF darajadagi loglarni qayd qiladi
     * Kelajakda Firebase Crashlytics bilan integratsiya qilish mumkin
     */
    private class ReleaseTree : Timber.Tree() {

        override fun isLoggable(tag: String?, priority: Int): Boolean {
            // Faqat WARNING va undan yuqori darajadagi loglarni qayd qilish
            return priority >= android.util.Log.WARN
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (!isLoggable(tag, priority)) return

            // Log xabarini qayd qilish
            when (priority) {
                android.util.Log.WARN -> {
                    android.util.Log.w(tag, message, t)
                }
                android.util.Log.ERROR -> {
                    android.util.Log.e(tag, message, t)
                    // TODO: Firebase Crashlytics ga yuborish
                    // FirebaseCrashlytics.getInstance().recordException(t ?: Exception(message))
                }
                android.util.Log.ASSERT -> {
                    android.util.Log.wtf(tag, message, t)
                    // TODO: Firebase Crashlytics ga yuborish
                    // FirebaseCrashlytics.getInstance().recordException(t ?: Exception(message))
                }
            }
        }
    }

    // =========================================================================
    // Bildirishnoma Kanallari
    // =========================================================================

    /**
     * Android 8.0+ uchun bildirishnoma kanallarini yaratish
     *
     * Kanallar:
     * 1. SMS_AGENT_CHANNEL - Foreground service bildirishnomalari (past muhimlik)
     * 2. SMS_STATUS_CHANNEL - SMS holati haqida bildirishnomalar (yuqori muhimlik)
     */
    private fun createNotificationChannels() {
        // Android 8.0 (API 26) va undan yuqori uchun
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // --- Kanal 1: SMS Agent Service ---
            // Foreground service uchun doimiy bildirishnoma
            val agentChannel = NotificationChannel(
                Constants.NotificationChannels.SMS_AGENT_CHANNEL_ID,
                "SMS Agent Xizmati",
                NotificationManager.IMPORTANCE_LOW // Past muhimlik - ovoz chiqarmaydi
            ).apply {
                description = "SMS Agent foreground xizmati uchun doimiy bildirishnoma. " +
                    "Ilova fonda SMS yuborishda ishlayotganini ko'rsatadi."
                setShowBadge(false) // Badge ko'rsatmaslik
                enableVibration(false) // Vibratsiya yo'q
                enableLights(false) // LED yo'q
            }

            // --- Kanal 2: SMS Holati ---
            // SMS yuborilganligi/yetkazilganligi haqida bildirishnomalar
            val statusChannel = NotificationChannel(
                Constants.NotificationChannels.SMS_STATUS_CHANNEL_ID,
                "SMS Holati",
                NotificationManager.IMPORTANCE_HIGH // Yuqori muhimlik - ovoz chiqaradi
            ).apply {
                description = "SMS yuborilganligi, yetkazilganligi yoki xatoliklar haqida " +
                    "bildirishnomalar."
                setShowBadge(true) // Badge ko'rsatish
                enableVibration(true) // Vibratsiya yoqish
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                enableLights(true) // LED yoqish
            }

            // Kanallarni ro'yxatdan o'tkazish
            notificationManager.createNotificationChannels(
                listOf(agentChannel, statusChannel)
            )

            Timber.d("Bildirishnoma kanallari yaratildi: %d ta", 2)
        }
    }
}
