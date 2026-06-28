package com.taxisms.agent.sender

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import com.taxisms.agent.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmsSender - Android SmsManager API orqali SMS yuborish.
 * Dual SIM-ni qo'llab-quvvatlaydi.
 */
@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val simManager: SimManager
) {

    /**
     * SMS xabarni yuborish.
     * @param id SMS log yozuvining mahalliy bazadagi ID raqami.
     * @param phoneNumber Qabul qiluvchining telefon raqami.
     * @param message Xabar matni.
     * @param slotIndex Qaysi SIM slotidan yuborilishi kerak (0 yoki 1).
     */
    fun sendSms(id: Long, phoneNumber: String, message: String, slotIndex: Int): Boolean {
        return try {
            Timber.d("SMS yuborilmoqda: id=$id, raqam=$phoneNumber, SIM slot=$slotIndex")

            // SmsManager obyektini yaratish/olish
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionId = simManager.getSubscriptionIdForSlot(slotIndex)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                }
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Sent callback intent (explicit targeting to SmsSentReceiver)
            val sentIntent = Intent(context, com.taxisms.agent.receiver.SmsSentReceiver::class.java).apply {
                action = Constants.Actions.SMS_SENT
                putExtra(Constants.Extras.SMS_ID, id)
            }
            val sentPI = PendingIntent.getBroadcast(
                context,
                id.toInt(),
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Delivered callback intent (explicit targeting to SmsDeliveryReceiver)
            val deliveredIntent = Intent(context, com.taxisms.agent.receiver.SmsDeliveryReceiver::class.java).apply {
                action = Constants.Actions.SMS_DELIVERED
                putExtra(Constants.Extras.SMS_ID, id)
            }
            val deliveredPI = PendingIntent.getBroadcast(
                context,
                id.toInt(),
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Xabarni uzunligiga qarab bo'lib yuborish
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                val sentPIs = ArrayList<PendingIntent?>().apply {
                    repeat(parts.size - 1) { add(null) }
                    add(sentPI)
                }
                val deliveredPIs = ArrayList<PendingIntent?>().apply {
                    repeat(parts.size - 1) { add(null) }
                    add(deliveredPI)
                }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentPIs, deliveredPIs)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI)
            }

            Timber.i("SMS yuborish so'rovi muvaffaqiyatli topshirildi: id=$id")
            true
        } catch (e: Exception) {
            Timber.e(e, "SmsManager orqali SMS yuborishda xatolik yuz berdi: id=$id")
            false
        }
    }
}
