package com.taxisms.agent.sender

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SIM Manager - Dual SIM qurilmalarida SIM kartalarni boshqarish.
 * Android 5.0 (API 22) va undan yuqori versiyalarda SubscriptionManager orqali ishlaydi.
 */
@Singleton
class SimManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val subscriptionManager: SubscriptionManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        } else {
            null
        }
    }

    /**
     * Qurilmada mavjud bo'lgan barcha faol SIM kartalar ro'yxatini olish.
     */
    fun getActiveSimCards(): List<SubscriptionInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("READ_PHONE_STATE ruxsati berilmagan")
            return emptyList()
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Mavjud SIM kartalarni aniqlashda xatolik yuz berdi")
            emptyList()
        }
    }

    /**
     * Ma'lum bir slot indeksiga (0 yoki 1) tegishli bo'lgan SIM karta ma'lumotlarini olish.
     */
    fun getSubscriptionInfoForSlot(slotIndex: Int): SubscriptionInfo? {
        val simList = getActiveSimCards()
        return simList.find { it.simSlotIndex == slotIndex }
    }

    /**
     * Qurilmada Dual SIM qo'llab-quvvatlanishini tekshirish.
     */
    fun isDualSimSupported(): Boolean {
        return getActiveSimCards().size > 1
    }

    /**
     * SMS yuborish uchun tavsiya etilgan SIM kartaning subscriptionId sini aniqlash.
     * Sozlamalarda tanlangan slotIndex asosida ishlaydi (default = 0).
     */
    fun getSubscriptionIdForSlot(slotIndex: Int): Int {
        val info = getSubscriptionInfoForSlot(slotIndex)
        return info?.subscriptionId ?: SubscriptionManager.DEFAULT_SUBSCRIPTION_ID
    }
}
