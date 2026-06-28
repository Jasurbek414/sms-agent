package com.taxisms.agent.sender

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SMS Rate Limiter - SMS yuborish tezligi va limitlarini nazorat qiladi.
 * Anti-spam qoidalarini ham amalga oshiradi (BR-1, BR-2).
 */
@Singleton
class SmsRateLimiter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("sms_rate_limiter_prefs", Context.MODE_PRIVATE)

    // Tarixiy yozuvlarni tozalash (masalan, 24 soatdan oshganlarni)
    private fun cleanupSpamHistory() {
        val now = System.currentTimeMillis()
        val allEntries = prefs.all
        val editor = prefs.edit()
        for ((key, value) in allEntries) {
            if (key.startsWith("spam_") && value is Long) {
                if (now - value > TimeUnit.MINUTES.toMillis(5)) {
                    editor.remove(key)
                }
            }
        }
        editor.apply()
    }

    @Synchronized
    fun isSpam(phoneNumber: String): Boolean {
        cleanupSpamHistory()
        val now = System.currentTimeMillis()
        val key = "spam_$phoneNumber"
        val lastSent = prefs.getLong(key, 0L)
        if (lastSent > 0 && now - lastSent < TimeUnit.MINUTES.toMillis(3)) {
            return true
        }
        return false
    }

    /**
     * SMS muvaffaqiyatli yuborilganda anti-spam vaqtini yangilash.
     */
    @Synchronized
    fun recordSpamCheck(phoneNumber: String) {
        val now = System.currentTimeMillis()
        val key = "spam_$phoneNumber"
        prefs.edit().putLong(key, now).apply()
    }

    /**
     * Kunlik/Soatlik limitlarni tekshirish.
     * BR-1: Kunlik 250 ta, Soatlik 50 ta SMS limiti.
     */
    @Synchronized
    fun isLimitExceeded(): Boolean {
        val now = System.currentTimeMillis()
        val hourStart = now - TimeUnit.HOURS.toMillis(1)
        val dayStart = now - TimeUnit.DAYS.toMillis(1)

        // Local dynamic logs of timestamps
        val timestamps = getSmsTimestamps()
        
        // Soatlik tekshiruv (oxirgi 1 soatdagi SMS-lar)
        val inLastHour = timestamps.count { it > hourStart }
        if (inLastHour >= 50) return true

        // Kunlik tekshiruv (oxirgi 24 soatdagi SMS-lar)
        val inLastDay = timestamps.count { it > dayStart }
        if (inLastDay >= 250) return true

        return false
    }

    /**
     * Yangi SMS muvaffaqiyatli yuborilganda uni limit ro'yxatiga qayd qilish.
     */
    @Synchronized
    fun recordSmsSent() {
        val now = System.currentTimeMillis()
        val timestamps = getSmsTimestamps().toMutableList()
        timestamps.add(now)
        
        // 24 soatdan oshganlarni o'chirib tashlaymiz
        val dayStart = now - TimeUnit.DAYS.toMillis(1)
        val cleanList = timestamps.filter { it > dayStart }

        saveSmsTimestamps(cleanList)
    }

    private fun getSmsTimestamps(): List<Long> {
        val dataStr = prefs.getString("sent_timestamps", "") ?: ""
        if (dataStr.isEmpty()) return emptyList()
        return dataStr.split(",").mapNotNull { it.toLongOrNull() }
    }

    private fun saveSmsTimestamps(list: List<Long>) {
        val dataStr = list.joinToString(",")
        prefs.edit().putString("sent_timestamps", dataStr).apply()
    }
}
