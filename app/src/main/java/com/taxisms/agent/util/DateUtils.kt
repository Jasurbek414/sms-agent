package com.taxisms.agent.util

import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * SMS Agent Platform - Sana va Vaqt Yordamchi Funksiyalari
 *
 * Ilova bo'ylab ishlatiladigan sana formatlash,
 * timezone boshqarish va sana hisoblash utility-lari.
 */
object DateUtils {

    // =========================================================================
    // SANA FORMATLARI
    // =========================================================================

    /** To'liq sana va vaqt: 28.06.2026 14:30:45 */
    const val FORMAT_FULL = "dd.MM.yyyy HH:mm:ss"

    /** Qisqa sana va vaqt: 28.06.2026 14:30 */
    const val FORMAT_SHORT = "dd.MM.yyyy HH:mm"

    /** Faqat sana: 28.06.2026 */
    const val FORMAT_DATE_ONLY = "dd.MM.yyyy"

    /** Faqat vaqt: 14:30:45 */
    const val FORMAT_TIME_ONLY = "HH:mm:ss"

    /** Faqat soat va daqiqa: 14:30 */
    const val FORMAT_TIME_SHORT = "HH:mm"

    /** ISO 8601 format: 2026-06-28T14:30:45.000Z */
    const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    /** Server uchun format: 2026-06-28 14:30:45 */
    const val FORMAT_SERVER = "yyyy-MM-dd HH:mm:ss"

    /** Fayl nomi uchun format: 20260628_143045 */
    const val FORMAT_FILENAME = "yyyyMMdd_HHmmss"

    /** Log uchun format: 2026-06-28 14:30:45.123 */
    const val FORMAT_LOG = "yyyy-MM-dd HH:mm:ss.SSS"

    // =========================================================================
    // O'ZBEKISTON TIMEZONE
    // =========================================================================

    /** O'zbekiston vaqt zonasi: UTC+5 */
    val TIMEZONE_UZ: TimeZone = TimeZone.getTimeZone("Asia/Tashkent")

    /** O'zbekiston locale */
    val LOCALE_UZ: Locale = Locale("uz", "UZ")

    // =========================================================================
    // FORMATLASH FUNKSIYALARI
    // =========================================================================

    /**
     * Joriy vaqtni belgilangan formatda olish
     *
     * @param pattern Sana formati (default: FORMAT_FULL)
     * @param timeZone Vaqt zonasi (default: O'zbekiston)
     * @return Formatlangan joriy sana-vaqt string
     */
    fun now(
        pattern: String = FORMAT_FULL,
        timeZone: TimeZone = TIMEZONE_UZ
    ): String {
        return formatDate(Date(), pattern, timeZone)
    }

    /**
     * Timestamp-ni formatlangan stringga aylantirish
     *
     * @param timestamp Unix timestamp (millisekund)
     * @param pattern Sana formati
     * @param timeZone Vaqt zonasi
     * @return Formatlangan sana string
     */
    fun formatTimestamp(
        timestamp: Long,
        pattern: String = FORMAT_FULL,
        timeZone: TimeZone = TIMEZONE_UZ
    ): String {
        return formatDate(Date(timestamp), pattern, timeZone)
    }

    /**
     * Date obyektini formatlangan stringga aylantirish
     *
     * @param date Sana obyekti
     * @param pattern Sana formati
     * @param timeZone Vaqt zonasi
     * @return Formatlangan sana string
     */
    fun formatDate(
        date: Date,
        pattern: String = FORMAT_FULL,
        timeZone: TimeZone = TIMEZONE_UZ
    ): String {
        return try {
            val sdf = SimpleDateFormat(pattern, LOCALE_UZ)
            sdf.timeZone = timeZone
            sdf.format(date)
        } catch (e: Exception) {
            Timber.e(e, "Sana formatlashda xatolik: pattern=$pattern")
            ""
        }
    }

    /**
     * String sanani Date obyektiga aylantirish (parsing)
     *
     * @param dateString Sana matni
     * @param pattern Sana formati
     * @param timeZone Vaqt zonasi
     * @return Date obyekti yoki null (agar xatolik bo'lsa)
     */
    fun parseDate(
        dateString: String,
        pattern: String = FORMAT_FULL,
        timeZone: TimeZone = TIMEZONE_UZ
    ): Date? {
        return try {
            val sdf = SimpleDateFormat(pattern, LOCALE_UZ)
            sdf.timeZone = timeZone
            sdf.isLenient = false // Qat'iy parsing
            sdf.parse(dateString)
        } catch (e: Exception) {
            Timber.e(e, "Sana parsing xatoligi: input=$dateString, pattern=$pattern")
            null
        }
    }

    // =========================================================================
    // TIMEZONE KONVERTATSIYA
    // =========================================================================

    /**
     * UTC timestamp-ni mahalliy vaqtga aylantirish
     *
     * @param utcTimestamp UTC vaqtdagi timestamp
     * @return Mahalliy vaqtga formatlangan string
     */
    fun utcToLocal(utcTimestamp: Long): String {
        return formatTimestamp(utcTimestamp, FORMAT_FULL, TIMEZONE_UZ)
    }

    /**
     * ISO 8601 formatdagi stringni mahalliy vaqtga aylantirish
     *
     * @param isoString ISO 8601 formatdagi sana string
     * @return Mahalliy vaqtga formatlangan string yoki empty string
     */
    fun isoToLocal(isoString: String): String {
        val date = parseDate(isoString, FORMAT_ISO_8601, TimeZone.getTimeZone("UTC"))
        return date?.let { formatDate(it, FORMAT_FULL, TIMEZONE_UZ) } ?: ""
    }

    /**
     * Mahalliy vaqtni UTC timestamp-ga aylantirish
     *
     * @param localDate Mahalliy vaqtdagi Date obyekti
     * @return UTC timestamp (millisekund)
     */
    fun localToUtcTimestamp(localDate: Date = Date()): Long {
        return localDate.time
    }

    // =========================================================================
    // SANA HISOBLASH
    // =========================================================================

    /**
     * Bugungi kunning boshlanish vaqtini olish (00:00:00)
     */
    fun getStartOfToday(): Long {
        val calendar = Calendar.getInstance(TIMEZONE_UZ)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Bugungi kunning tugash vaqtini olish (23:59:59)
     */
    fun getEndOfToday(): Long {
        val calendar = Calendar.getInstance(TIMEZONE_UZ)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Belgilangan kun sonini orqaga hisoblash
     *
     * @param days Orqaga hisoblash uchun kun soni
     * @return Hisoblangan sananing timestamp-i
     */
    fun daysAgo(days: Int): Long {
        val calendar = Calendar.getInstance(TIMEZONE_UZ)
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return calendar.timeInMillis
    }

    /**
     * Ikki sana orasidagi farqni hisoblash
     *
     * @param start Boshlanish vaqti (timestamp)
     * @param end Tugash vaqti (timestamp)
     * @return Farq haqida ma'lumot (kun, soat, daqiqa)
     */
    fun getTimeDifference(start: Long, end: Long): TimeDifference {
        val diffMs = kotlin.math.abs(end - start)
        val days = diffMs / (24 * 60 * 60 * 1000)
        val hours = (diffMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        val minutes = (diffMs % (60 * 60 * 1000)) / (60 * 1000)
        val seconds = (diffMs % (60 * 1000)) / 1000

        return TimeDifference(
            totalMillis = diffMs,
            days = days.toInt(),
            hours = hours.toInt(),
            minutes = minutes.toInt(),
            seconds = seconds.toInt()
        )
    }

    /**
     * Vaqt farqi ma'lumotlari
     */
    data class TimeDifference(
        /** Umumiy farq (millisekund) */
        val totalMillis: Long,
        /** Kunlar soni */
        val days: Int,
        /** Soatlar soni */
        val hours: Int,
        /** Daqiqalar soni */
        val minutes: Int,
        /** Sekundlar soni */
        val seconds: Int
    ) {
        /**
         * O'zbekcha formatlangan farq matni
         * Misol: "2 kun 3 soat 15 daqiqa"
         */
        fun toDisplayString(): String {
            return buildString {
                if (days > 0) append("$days kun ")
                if (hours > 0) append("$hours soat ")
                if (minutes > 0) append("$minutes daqiqa")
                if (isEmpty()) append("1 daqiqadan kam")
            }.trim()
        }
    }

    // =========================================================================
    // YORDAMCHI FUNKSIYALAR
    // =========================================================================

    /**
     * Berilgan timestamp bugungi kunga tegishli ekanligini tekshirish
     */
    fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance(TIMEZONE_UZ)
        val check = Calendar.getInstance(TIMEZONE_UZ).apply {
            timeInMillis = timestamp
        }
        return today.get(Calendar.YEAR) == check.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == check.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Berilgan timestamp kechagi kunga tegishli ekanligini tekshirish
     */
    fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance(TIMEZONE_UZ).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val check = Calendar.getInstance(TIMEZONE_UZ).apply {
            timeInMillis = timestamp
        }
        return yesterday.get(Calendar.YEAR) == check.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == check.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Sana uchun aqlli format: "Bugun 14:30", "Kecha 10:15", "25.06.2026 09:00"
     *
     * @param timestamp Vaqt tamg'asi
     * @return Aqlli formatlangan sana string
     */
    fun smartFormat(timestamp: Long): String {
        return when {
            isToday(timestamp) -> "Bugun ${formatTimestamp(timestamp, FORMAT_TIME_SHORT)}"
            isYesterday(timestamp) -> "Kecha ${formatTimestamp(timestamp, FORMAT_TIME_SHORT)}"
            else -> formatTimestamp(timestamp, FORMAT_SHORT)
        }
    }
}
