package com.taxisms.agent.util

/**
 * SMS Agent Platform - Doimiy Qiymatlar (Constants)
 *
 * Ilova bo'ylab ishlatiluvchi barcha doimiy qiymatlar.
 * Markazlashtirilgan boshqaruv orqali xatoliklarni kamaytirish.
 */
object Constants {

    // =========================================================================
    // SMS HOLAT KONSTANTALARI
    // =========================================================================

    /**
     * SMS xabar holatlari
     * SMS hayot siklining har bir bosqichini ifodalaydi
     */
    object SmsStatus {
        /** SMS navbatda kutmoqda */
        const val PENDING = 0

        /** SMS yuborilmoqda (SmsManager ga berildi) */
        const val SENDING = 1

        /** SMS muvaffaqiyatli yuborildi (SENT callback) */
        const val SENT = 2

        /** SMS qabul qiluvchiga yetkazildi (DELIVERY callback) */
        const val DELIVERED = 3

        /** SMS yuborishda xatolik yuz berdi */
        const val FAILED = 4

        /** SMS bekor qilindi (foydalanuvchi tomonidan) */
        const val CANCELLED = 5

        /** SMS qayta yuborish uchun navbatga qo'yildi */
        const val RETRY = 6

        /** SMS muddati tugadi (timeout) */
        const val EXPIRED = 7
    }

    // =========================================================================
    // XATOLIK KODLARI
    // =========================================================================

    /**
     * SMS xatolik kodlari
     * Har bir xatolik turi uchun alohida diapazon ajratilgan
     */
    object ErrorCodes {
        // --- Tarmoq xatoliklari (100-199) ---
        /** Internetga ulanish yo'q */
        const val NO_NETWORK = 100

        /** Server bilan aloqa uzildi */
        const val CONNECTION_LOST = 101

        /** Server javob bermadi (timeout) */
        const val SERVER_TIMEOUT = 102

        /** SSL/TLS xatoligi */
        const val SSL_ERROR = 103

        // --- SMS xatoliklari (200-299) ---
        /** SMS yuborish ruxsati yo'q */
        const val SMS_PERMISSION_DENIED = 200

        /** Noto'g'ri telefon raqam */
        const val INVALID_PHONE_NUMBER = 201

        /** SMS matni bo'sh */
        const val EMPTY_MESSAGE = 202

        /** SMS matni juda uzun */
        const val MESSAGE_TOO_LONG = 203

        /** SIM karta topilmadi */
        const val NO_SIM_CARD = 204

        /** Umumiy SMS yuborish xatoligi */
        const val SMS_GENERIC_FAILURE = 205

        /** Xizmat mavjud emas */
        const val SMS_NO_SERVICE = 206

        /** Radio o'chirilgan (samolyot rejimi) */
        const val SMS_RADIO_OFF = 207

        /** Null PDU xatoligi */
        const val SMS_NULL_PDU = 208

        // --- Server xatoliklari (300-399) ---
        /** Server autentifikatsiya xatoligi */
        const val AUTH_ERROR = 300

        /** Noto'g'ri API kalit */
        const val INVALID_API_KEY = 301

        /** Server ichki xatoligi */
        const val SERVER_INTERNAL_ERROR = 302

        /** Noto'g'ri so'rov formati */
        const val BAD_REQUEST = 303

        /** Servis mavjud emas */
        const val SERVICE_UNAVAILABLE = 304

        // --- Ilova ichki xatoliklari (400-499) ---
        /** Ma'lumotlar bazasi xatoligi */
        const val DATABASE_ERROR = 400

        /** Noma'lum xatolik */
        const val UNKNOWN_ERROR = 499
    }

    // =========================================================================
    // DEFAULT KONFIGURATSIYA QIYMATLARI
    // =========================================================================

    /**
     * Standart sozlamalar qiymatlari
     */
    object Defaults {
        /** Qayta urinish uchun kutish vaqti (millisekund) */
        const val RETRY_DELAY_MS = 5000L

        /** Maksimal qayta urinishlar soni */
        const val MAX_RETRY_COUNT = 3

        /** SMS navbat hajmi limiti */
        const val MAX_QUEUE_SIZE = 100

        /** SMS orasidagi minimum interval (millisekund) */
        const val MIN_SMS_INTERVAL_MS = 1000L

        /** WebSocket qayta ulanish kutish vaqti (millisekund) */
        const val WEBSOCKET_RECONNECT_DELAY_MS = 3000L

        /** Maksimal WebSocket qayta ulanish urinishlari */
        const val MAX_WEBSOCKET_RECONNECT_ATTEMPTS = 10

        /** SMS log yozuvlarini saqlash muddati (kun) */
        const val LOG_RETENTION_DAYS = 30

        /** Foreground service bildirishnoma ID */
        const val FOREGROUND_NOTIFICATION_ID = 1001

        /** SMS holati bildirishnoma boshlang'ich ID */
        const val SMS_STATUS_NOTIFICATION_BASE_ID = 2000

        /** Avtomatik ishga tushirish (boot-da) */
        const val AUTO_START_ON_BOOT = true

        /** Vibratsiya yoqilganmi */
        const val VIBRATION_ENABLED = true
    }

    // =========================================================================
    // INTENT ACTION STRINGS
    // =========================================================================

    /**
     * Intent action-lar
     * BroadcastReceiver va Service-lar o'rtasida aloqa uchun
     */
    object Actions {
        /** Paket prefiksi */
        private const val PREFIX = "com.taxisms.agent.action"

        /** SMS yuborish so'rovi */
        const val SEND_SMS_REQUEST = "$PREFIX.SEND_SMS_REQUEST"

        /** SMS bekor qilish so'rovi */
        const val CANCEL_SMS_REQUEST = "$PREFIX.CANCEL_SMS_REQUEST"

        /** SMS holati tekshirish */
        const val CHECK_SMS_STATUS = "$PREFIX.CHECK_SMS_STATUS"

        /** SMS yuborildi callback */
        const val SMS_SENT = "$PREFIX.SMS_SENT"

        /** SMS yetkazildi callback */
        const val SMS_DELIVERED = "$PREFIX.SMS_DELIVERED"

        /** Xizmatni ishga tushirish */
        const val START_SERVICE = "$PREFIX.START_SERVICE"

        /** Xizmatni to'xtatish */
        const val STOP_SERVICE = "$PREFIX.STOP_SERVICE"

        /** Serverga ulanish */
        const val CONNECT_TO_SERVER = "$PREFIX.CONNECT_TO_SERVER"

        /** Serverdan uzilish */
        const val DISCONNECT_FROM_SERVER = "$PREFIX.DISCONNECT_FROM_SERVER"

        /** SMS holatini serverga xabar qilish */
        const val REPORT_STATUS = "$PREFIX.REPORT_STATUS"
    }

    // =========================================================================
    // INTENT EXTRA KALITLARI
    // =========================================================================

    /**
     * Intent extra kalitlari
     * Intent-lar orqali ma'lumot uzatish uchun
     */
    object Extras {
        /** Telefon raqam */
        const val PHONE_NUMBER = "extra_phone_number"

        /** SMS matni */
        const val MESSAGE_TEXT = "extra_message_text"

        /** SMS ID (local database) */
        const val SMS_ID = "extra_sms_id"

        /** SMS holat kodi */
        const val SMS_STATUS = "extra_sms_status"

        /** Xatolik kodi */
        const val ERROR_CODE = "extra_error_code"

        /** Xatolik xabari */
        const val ERROR_MESSAGE = "extra_error_message"

        /** Server URL */
        const val SERVER_URL = "extra_server_url"

        /** API kalit */
        const val API_KEY = "extra_api_key"

        /** So'rov ID (server tomondan) */
        const val REQUEST_ID = "extra_request_id"
    }

    // =========================================================================
    // BILDIRISHNOMA KANAL IDENTIFIKATORLARI
    // =========================================================================

    /**
     * Bildirishnoma kanallari
     * Android 8.0+ uchun majburiy kanal tizimi
     */
    object NotificationChannels {
        /** SMS Agent foreground xizmati kanali */
        const val SMS_AGENT_CHANNEL_ID = "sms_agent_channel"

        /** SMS holati bildirishnomalar kanali */
        const val SMS_STATUS_CHANNEL_ID = "sms_status_channel"
    }

    // =========================================================================
    // SHARED PREFERENCES KALITLARI
    // =========================================================================

    /**
     * SharedPreferences kalit nomlari
     * Legacy sozlamalar uchun
     */
    object Preferences {
        /** SharedPreferences fayl nomi */
        const val PREF_NAME = "sms_agent_prefs"

        /** Birinchi marta ishga tushirilganmi */
        const val IS_FIRST_LAUNCH = "is_first_launch"

        /** Xizmat faol holatda */
        const val IS_SERVICE_RUNNING = "is_service_running"

        /** Oxirgi ulanish vaqti */
        const val LAST_CONNECTED_TIME = "last_connected_time"

        /** Serverga ulangan */
        const val IS_CONNECTED = "is_connected"

        /** Foydalanuvchi tili */
        const val LANGUAGE = "language"

        /** Tungi rejim */
        const val DARK_MODE = "dark_mode"

        /** Agent identifikatori */
        const val AGENT_ID = "agent_id"

        /** Haydovchi telefon raqami */
        const val DRIVER_PHONE = "driver_phone"

        /** Taksi park nomi */
        const val PARK_NAME = "park_name"

        /** Server URL manzili */
        const val SERVER_URL = "server_url"

        /** API autentifikatsiya kaliti */
        const val API_KEY = "api_key"
    }

    // =========================================================================
    // DATASTORE KALITLARI
    // =========================================================================

    /**
     * DataStore kalit nomlari
     * Zamonaviy sozlamalar uchun
     */
    object DataStore {
        /** DataStore fayl nomi */
        const val PREFERENCES_NAME = "sms_agent_datastore"
    }

    // =========================================================================
    // MA'LUMOTLAR BAZASI
    // =========================================================================

    /**
     * Room Database konstantalari
     */
    object Database {
        /** Ma'lumotlar bazasi fayl nomi */
        const val DATABASE_NAME = "sms_agent_database"

        /** Joriy versiya */
        const val DATABASE_VERSION = 1
    }

    // =========================================================================
    // TARMOQ SOZLAMALARI
    // =========================================================================

    /**
     * HTTP va WebSocket tarmoq sozlamalari
     */
    object Network {
        /** Ulanish timeout (sekund) */
        const val CONNECT_TIMEOUT_SECONDS = 30L

        /** O'qish timeout (sekund) */
        const val READ_TIMEOUT_SECONDS = 30L

        /** Yozish timeout (sekund) */
        const val WRITE_TIMEOUT_SECONDS = 30L

        /** WebSocket ping interval (sekund) */
        const val PING_INTERVAL_SECONDS = 15L
    }
}
