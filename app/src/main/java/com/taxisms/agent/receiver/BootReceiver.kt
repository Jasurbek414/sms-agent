package com.taxisms.agent.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.taxisms.agent.service.SmsAgentService
import com.taxisms.agent.util.Constants
import timber.log.Timber

/**
 * BootReceiver - Telefon yoqilganda (reboot) avtomatik ishga tushib,
 * SMS Agent foreground service-ni faollashtiradi (agar sozlamalarda ruxsat berilgan bo'lsa).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.i("Qurilma yuklandi. SMS Agent xizmati ishga tushirilmoqda...")
            
            // Sozlamalarni tekshirib, xizmatni start qilish
            val prefs = context.getSharedPreferences(Constants.Preferences.PREF_NAME, Context.MODE_PRIVATE)
            val shouldAutoStart = prefs.getBoolean(Constants.Preferences.IS_SERVICE_RUNNING, true)

            if (shouldAutoStart) {
                val serviceIntent = Intent(context, SmsAgentService::class.java).apply {
                    action = Constants.Actions.START_SERVICE
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                    Timber.d("SmsAgentService foreground rejimida start qilindi")
                } catch (e: Exception) {
                    Timber.e(e, "Xizmatni fonda boshlashda xatolik")
                }
            }
        }
    }
}
