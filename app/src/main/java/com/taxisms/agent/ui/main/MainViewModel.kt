package com.taxisms.agent.ui.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taxisms.agent.service.SmsAgentService
import com.taxisms.agent.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainViewModel - Bosh sahifa va umumiy ilova holatini boshqarish.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private val _isNotificationListenerGranted = MutableStateFlow(false)
    val isNotificationListenerGranted: StateFlow<Boolean> = _isNotificationListenerGranted

    init {
        checkServiceStatus()
        checkNotificationListenerPermission()
    }

    /**
     * Fondagi SMS Agent xizmati joriy holatini tekshirish.
     */
    fun checkServiceStatus() {
        val prefs = context.getSharedPreferences(Constants.Preferences.PREF_NAME, Context.MODE_PRIVATE)
        _isServiceRunning.value = prefs.getBoolean(Constants.Preferences.IS_SERVICE_RUNNING, false)
    }

    /**
     * Fondagi xizmatni ishga tushirish yoki to'xtatish.
     */
    fun toggleService() {
        viewModelScope.launch {
            val intent = Intent(context, SmsAgentService::class.java)
            if (_isServiceRunning.value) {
                // Xizmatni to'xtatish
                intent.action = Constants.Actions.STOP_SERVICE
                context.startService(intent)
                _isServiceRunning.value = false
            } else {
                // Xizmatni boshlash
                intent.action = Constants.Actions.START_SERVICE
                ContextCompat.startForegroundService(context, intent)
                _isServiceRunning.value = true
            }
        }
    }

    /**
     * Bildirishnoma tinglovchisi ruxsati berilganligini tekshirish
     */
    fun checkNotificationListenerPermission() {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        var enabled = false
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    enabled = true
                    break
                }
            }
        }
        _isNotificationListenerGranted.value = enabled
    }
}
