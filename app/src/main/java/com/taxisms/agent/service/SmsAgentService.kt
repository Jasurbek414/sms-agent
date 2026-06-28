package com.taxisms.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.taxisms.agent.data.local.entity.SmsLogEntity
import com.taxisms.agent.data.remote.api.AgentApiService
import com.taxisms.agent.data.repository.ConnectionRepository
import com.taxisms.agent.data.repository.SmsRepository
import com.taxisms.agent.data.repository.SettingsRepository
import com.taxisms.agent.sender.SmsRateLimiter
import com.taxisms.agent.sender.SmsSender
import com.taxisms.agent.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import javax.inject.Inject

/**
 * SmsAgentService - Fondagi doimiy ishlovchi xizmat (Foreground Service).
 * - Mahalliy database navbatini tekshiradi va SMS yuboradi.
 * - Serverdan yangi SMS buyruqlarini oladi (REST Polling).
 * - SMS yuborilganlik/yetkazilganlik holatlarini serverga hisobot qiladi.
 */
@AndroidEntryPoint
class SmsAgentService : Service() {

    @Inject
    lateinit var smsRepository: SmsRepository

    @Inject
    lateinit var smsSender: SmsSender

    @Inject
    lateinit var rateLimiter: SmsRateLimiter

    @Inject
    lateinit var connectionRepository: ConnectionRepository

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var queueJob: Job? = null
    private var pollingJob: Job? = null
    private val serviceJob = kotlinx.coroutines.SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    // SMS hisoblagichlari (statistika)
    private var totalSentCount = 0
    private var totalDeliveredCount = 0
    private var totalFailedCount = 0

    override fun onCreate() {
        super.onCreate()
        Timber.i("SmsAgentService yaratildi")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: Constants.Actions.START_SERVICE
        Timber.d("onStartCommand: action=$action")

        when (action) {
            Constants.Actions.START_SERVICE -> {
                startForegroundService()
                startQueueProcessor()
                startServerPolling()
            }
            Constants.Actions.STOP_SERVICE -> {
                stopQueueProcessor()
                stopServerPolling()
                stopSelf()
            }
            Constants.Actions.REPORT_STATUS -> {
                val smsId = intent?.getLongExtra(Constants.Extras.SMS_ID, -1L) ?: -1L
                val status = intent?.getIntExtra(Constants.Extras.SMS_STATUS, -1) ?: -1
                if (smsId != -1L && status != -1) {
                    reportStatusToServer(smsId, status)
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification("SMS Agent faol", "SMS navbati tekshirilmoqda...")
        startForeground(Constants.Defaults.FOREGROUND_NOTIFICATION_ID, notification)
        
        val prefs = getSharedPreferences(Constants.Preferences.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.Preferences.IS_SERVICE_RUNNING, true).apply()
    }

    // =========================================================================
    // LOKAL SMS NAVBATINI QAYTA ISHLASH (ROOM DB -> SMS SENDER)
    // =========================================================================
    private fun startQueueProcessor() {
        if (queueJob != null && queueJob?.isActive == true) return

        queueJob = serviceScope.launch {
            Timber.i("SMS Navbat protsessori ishga tushdi")
            while (isActive) {
                try {
                    val pendingSms = smsRepository.getNextPendingSms(Constants.SmsStatus.PENDING)
                    if (pendingSms != null) {
                        if (rateLimiter.isLimitExceeded()) {
                            delay(Constants.Defaults.RETRY_DELAY_MS)
                            continue
                        }

                        if (rateLimiter.isSpam(pendingSms.phoneNumber)) {
                            smsRepository.updateSmsStatus(
                                pendingSms.id,
                                Constants.SmsStatus.CANCELLED,
                                Constants.ErrorCodes.UNKNOWN_ERROR,
                                "Anti-Spam filter blocked this number temporarily"
                            )
                            updateNotificationStats()
                            continue
                        }

                        smsRepository.updateSmsStatus(pendingSms.id, Constants.SmsStatus.SENDING)
                        
                        // SIM slotini sozlamalardan o'qish (default 0 - SIM 1)
                        val preferredSimStr = settingsRepository.getValue("preferred_sim", "0")
                        val slot = preferredSimStr.toIntOrNull() ?: 0

                        val success = smsSender.sendSms(
                            id = pendingSms.id,
                            phoneNumber = pendingSms.phoneNumber,
                            message = pendingSms.messageText,
                            slotIndex = slot
                        )

                        if (!success) {
                            smsRepository.updateSmsStatus(
                                pendingSms.id,
                                Constants.SmsStatus.FAILED,
                                Constants.ErrorCodes.SMS_GENERIC_FAILURE,
                                "Failed to trigger SmsManager API"
                            )
                            totalFailedCount++
                            updateNotificationStats()
                            // Serverga darhol xabar berish
                            reportStatusToServer(pendingSms.id, Constants.SmsStatus.FAILED)
                        } else {
                            rateLimiter.recordSpamCheck(pendingSms.phoneNumber)
                            totalSentCount++
                            updateNotificationStats()
                        }
                        delay(Constants.Defaults.MIN_SMS_INTERVAL_MS)
                    } else {
                        delay(3000L)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Queue processor loopda kutilmagan xatolik")
                    delay(Constants.Defaults.RETRY_DELAY_MS)
                }
            }
        }
    }

    private fun stopQueueProcessor() {
        queueJob?.cancel()
        queueJob = null
        Timber.i("SMS Navbat protsessori to'xtatildi")

        val prefs = getSharedPreferences(Constants.Preferences.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.Preferences.IS_SERVICE_RUNNING, false).apply()
    }

    // =========================================================================
    // SERVER POLING MEXANIZMI (SERVER -> ROOM DB)
    // =========================================================================
    private fun startServerPolling() {
        if (pollingJob != null && pollingJob?.isActive == true) return

        pollingJob = serviceScope.launch {
            Timber.i("Server REST polling ishga tushdi")
            while (isActive) {
                try {
                    val connection = connectionRepository.getActiveConnection()
                    if (connection != null && connection.isActive) {
                        val apiService = getApiService(connection.serverUrl)
                        
                        // Kutilayotgan SMS buyruqlarini serverdan so'rash
                        val response = apiService.getPendingSmsRequests(
                            apiKey = "Bearer ${connection.apiKey}",
                            agentId = settingsRepository.getValue(Constants.Preferences.AGENT_ID, "agent_device")
                        )

                        if (response.isSuccessful) {
                            val phoneKey = settingsRepository.getValue("api_phone_path", "phone_number")
                            val priceKey = settingsRepository.getValue("api_price_path", "price")

                            val pendingRequests = response.body() ?: emptyList()
                            for (req in pendingRequests) {
                                val requestId = (req["external_id"] ?: req["id"]) as? String ?: continue
                                val phoneNumber = req[phoneKey] as? String ?: continue
                                var messageText = req["message"] as? String ?: ""
                                val price = req[priceKey] as? String

                                // Agar serverdan yo'l haqi summasi kelsa, shablon bo'yicha formatlaymiz
                                if (!price.isNullOrEmpty()) {
                                    val template = settingsRepository.getValue(
                                        "finish_ride_template",
                                        "Yo'l haqi: {price} so'm. Rahmat!"
                                    )
                                    messageText = template.replace("{price}", price)
                                }

                                if (messageText.isEmpty()) {
                                    Timber.w("Serverdan bo'sh SMS matni keldi, o'tkazib yuboramiz")
                                    continue
                                }

                                 // Takrorlanishni oldini olish (duplicate check)
                                 val exists = smsRepository.getByRequestId(requestId)
                                 if (exists == null) {
                                     val result = smsRepository.sendSms(phoneNumber, messageText, requestId)
                                     if (result is com.taxisms.agent.util.Resource.Success) {
                                         Timber.d("Serverdan yangi SMS navbatga qo'shildi: RequestId=$requestId")
                                     } else if (result is com.taxisms.agent.util.Resource.Error) {
                                         Timber.e("Serverdan kelgan SMSni navbatga qo'shishda xato: ${result.message}")
                                     }
                                 }
                            }
                        } else {
                            Timber.w("Serverdan SMS olishda xatolik: ${response.code()} ${response.message()}")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Server polling xatoligi")
                }
                delay(10000L) // Har 10 soniyada tekshirish
            }
        }
    }

    private fun stopServerPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Timber.i("Server REST polling to'xtatildi")
    }

    // =========================================================================
    // SMS STATUSINI SERVERGA HISOBOT QILISH (ROOM DB -> SERVER)
    // =========================================================================
    private fun reportStatusToServer(smsId: Long, status: Int) {
        serviceScope.launch {
            try {
                val connection = connectionRepository.getActiveConnection() ?: return@launch
                val smsLog = smsRepository.getById(smsId) ?: return@launch
                
                // Faqat serverdan kelgan (external_id ga ega) SMSlarni serverga hisobot qilamiz
                if (smsLog.requestId.startsWith("local_")) return@launch

                val apiService = getApiService(connection.serverUrl)
                val statusText = when (status) {
                    Constants.SmsStatus.PENDING -> "PENDING"
                    Constants.SmsStatus.SENDING -> "SENDING"
                    Constants.SmsStatus.SENT -> "SENT"
                    Constants.SmsStatus.DELIVERED -> "DELIVERED"
                    Constants.SmsStatus.FAILED -> "FAILED"
                    Constants.SmsStatus.CANCELLED -> "CANCELLED"
                    else -> "UNKNOWN"
                }

                val statusReport = mapOf(
                    "status" to statusText,
                    "error_code" to (smsLog.errorCode ?: 0),
                    "error_message" to (smsLog.errorMessage ?: "")
                )

                val response = apiService.reportSmsStatus(
                    apiKey = "Bearer ${connection.apiKey}",
                    requestId = smsLog.requestId,
                    statusReport = statusReport
                )

                if (response.isSuccessful) {
                    Timber.d("SMS holati serverga yuborildi: id=$smsId, status=$statusText")
                } else {
                    Timber.w("Serverga holat hisobotini yuborishda xatolik: ${response.code()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Serverga SMS holat hisoboti yuborish xatoligi: smsId=$smsId")
            }
        }
    }

    private var cachedApiService: AgentApiService? = null
    private var cachedBaseUrl: String? = null

    private fun getApiService(baseUrl: String): AgentApiService {
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (cachedBaseUrl == formattedUrl && cachedApiService != null) {
            return cachedApiService!!
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val service = retrofit.create(AgentApiService::class.java)
        cachedApiService = service
        cachedBaseUrl = formattedUrl
        return service
    }

    private fun updateNotificationStats() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val text = "Yuborildi: $totalSentCount | Yetkazildi: $totalDeliveredCount | Xato: $totalFailedCount"
        val notification = createNotification("SMS Agent faol", text)
        manager.notify(Constants.Defaults.FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun createNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, Constants.NotificationChannels.SMS_AGENT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SMS Agent Service"
            val descriptionText = "Fonda SMS yuborish va monitoring xizmati"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(Constants.NotificationChannels.SMS_AGENT_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        stopQueueProcessor()
        stopServerPolling()
        Timber.i("SmsAgentService yo'q qilindi")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
