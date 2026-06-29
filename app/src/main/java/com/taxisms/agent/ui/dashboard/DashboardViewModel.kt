package com.taxisms.agent.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxisms.agent.data.local.entity.ConnectionEntity
import com.taxisms.agent.data.local.entity.SmsLogEntity
import com.taxisms.agent.data.repository.ConnectionRepository
import com.taxisms.agent.data.repository.SettingsRepository
import com.taxisms.agent.data.repository.SmsRepository
import com.taxisms.agent.sender.SimManager
import com.taxisms.agent.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * DashboardViewModel - Dashboard va Sozlamalar ma'lumotlarini boshqaruvchi.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val connectionRepository: ConnectionRepository,
    private val settingsRepository: SettingsRepository,
    private val simManager: SimManager
) : ViewModel() {

    // Barcha SMS loglarini kuzatish
    val smsLogs: StateFlow<List<SmsLogEntity>> = smsRepository.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Faol server ulanishini kuzatish
    val activeConnection: StateFlow<ConnectionEntity?> = connectionRepository.observeActiveConnection()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Tanlangan SIM kartani kuzatish
    val preferredSim: StateFlow<String> = settingsRepository.observeValue("preferred_sim")
        .map { it ?: "0" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "0"
        )

    // SMS shablonini kuzatish
    val smsTemplate: StateFlow<String> = settingsRepository.observeValue("finish_ride_template")
        .map { it ?: "Yo'l haqi: {price} so'm. Rahmat!" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Yo'l haqi: {price} so'm. Rahmat!"
        )

    // Bildirishnomalarni o'qish yoqilganmi
    val notificationReaderEnabled: StateFlow<Boolean> = settingsRepository.observeValue("notification_reader_enabled")
        .map { it?.toBoolean() ?: false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Bildirishnoma paketi filtri
    val notificationPackageFilter: StateFlow<String> = settingsRepository.observeValue("notification_package_filter")
        .map { it ?: "" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    // Bildirishnoma kalit so'zi filtri
    val notificationKeywordFilter: StateFlow<String> = settingsRepository.observeValue("notification_keyword_filter")
        .map { it ?: "" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    // Dinamik JSON yo'l parametrlari
    val apiPhonePath: StateFlow<String> = settingsRepository.observeValue("api_phone_path")
        .map { it ?: "phone_number" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "phone_number"
        )

    val apiPricePath: StateFlow<String> = settingsRepository.observeValue("api_price_path")
        .map { it ?: "price" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "price"
        )

    private val _simInfoList = MutableStateFlow<List<String>>(emptyList())
    val simInfoList: StateFlow<List<String>> = _simInfoList

    init {
        loadSimInfo()
        autoConnectIfNoActiveConnection()
    }

    private fun autoConnectIfNoActiveConnection() {
        viewModelScope.launch {
            val active = connectionRepository.getActiveConnection()
            if (active == null) {
                connectionRepository.connect(
                    serverUrl = "https://taxsi.ecos.uz/backend/",
                    apiKey = "7e4cd8f3-72e2-4ed3-afcf-64274679cd86",
                    parkName = "Ecos Taxi"
                )
                if (settingsRepository.getValue("notification_package_filter", "").isEmpty()) {
                    settingsRepository.setValue("notification_package_filter", "su.skat.client")
                }
                if (settingsRepository.getValue("notification_keyword_filter", "").isEmpty()) {
                    settingsRepository.setValue("notification_keyword_filter", "Стоимость")
                }
            }
        }
    }

    /**
     * Mavjud SIM kartalar haqidagi ma'lumotlarni yuklash.
     */
    fun loadSimInfo() {
        viewModelScope.launch {
            val cards = simManager.getActiveSimCards()
            val info = cards.mapIndexed { idx, sub ->
                "SIM ${idx + 1}: ${sub.carrierName} (${sub.number ?: "Raqam yashirin"})"
            }
            _simInfoList.value = if (info.isEmpty()) listOf("SIM karta aniqlanmadi") else info
        }
    }

    /**
     * Serverga ulanish parametrlarini saqlash
     */
    fun connectToServer(serverUrl: String, apiKey: String, parkName: String) {
        viewModelScope.launch {
            connectionRepository.connect(serverUrl, apiKey, parkName)
        }
    }

    /**
     * Serverdan uzilish
     */
    fun disconnectFromServer() {
        viewModelScope.launch {
            connectionRepository.disconnect()
        }
    }

    /**
     * Qayta yuborish uchun ma'lum bir SMS xabarni navbatga qaytarish (failed holatidan).
     */
    fun retrySms(smsId: Long) {
        viewModelScope.launch {
            smsRepository.updateSmsStatus(smsId, Constants.SmsStatus.PENDING)
        }
    }

    /**
     * Sinov SMS xabarni qo'lda yuborish (Diagnostika va test uchun)
     */
    fun sendTestSms(phoneNumber: String, messageText: String) {
        viewModelScope.launch {
            val requestId = "test_" + UUID.randomUUID().toString().take(8)
            smsRepository.sendSms(phoneNumber, messageText, requestId)
        }
    }

    /**
     * SIM kartani tanlash
     */
    fun setPreferredSim(simSlot: String) {
        viewModelScope.launch {
            settingsRepository.setValue("preferred_sim", simSlot)
        }
    }

    /**
     * SMS shablonini saqlash
     */
    fun setSmsTemplate(template: String) {
        viewModelScope.launch {
            settingsRepository.setValue("finish_ride_template", template)
        }
    }

    /**
     * Bildirishnomalarni o'qishni yoqish/o'chirish
     */
    fun setNotificationReaderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBoolean("notification_reader_enabled", enabled)
        }
    }

    /**
     * Bildirishnoma paketi filtri qiymatini saqlash
     */
    fun setNotificationPackageFilter(filter: String) {
        viewModelScope.launch {
            settingsRepository.setValue("notification_package_filter", filter)
        }
    }

    /**
     * Bildirishnoma kalit so'zi filtri qiymatini saqlash
     */
    fun setNotificationKeywordFilter(filter: String) {
        viewModelScope.launch {
            settingsRepository.setValue("notification_keyword_filter", filter)
        }
    }

    /**
     * Dinamik API parser sozlamalarini saqlash
     */
    fun setApiPaths(phonePath: String, pricePath: String) {
        viewModelScope.launch {
            settingsRepository.setValue("api_phone_path", phonePath)
            settingsRepository.setValue("api_price_path", pricePath)
        }
    }

    /**
     * Barcha SMS loglarini tozalash
     */
    fun clearAllLogs() {
        viewModelScope.launch {
            smsRepository.clearAllLogs()
        }
    }
}
