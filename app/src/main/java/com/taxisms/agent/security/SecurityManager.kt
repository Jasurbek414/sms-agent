package com.taxisms.agent.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * SecurityManager - Ilova xavfsizlik boshqaruvchisi.
 * Emulator va Root aniqlanishlarini nazorat qiladi.
 */
@Singleton
class SecurityManager @Inject constructor(
    private val rootDetector: RootDetector,
    private val emulatorDetector: EmulatorDetector,
    private val encryptionManager: EncryptionManager
) {

    /**
     * Qurilmaning xavfsizlik talablariga javob berishini tekshirish.
     * Emulator yoki Root aniqlansa, false qaytaradi.
     */
    fun isDeviceSafe(): Boolean {
        if (emulatorDetector.isEmulator()) {
            return false
        }
        if (rootDetector.isRooted()) {
            return false
        }
        return true
    }

    /**
     * Sozlama qiymatlarini shifrlash.
     */
    fun encryptData(data: String): String {
        return encryptionManager.encrypt(data)
    }

    /**
     * Shifrlangan sozlamalarni o'qish.
     */
    fun decryptData(encryptedData: String): String {
        return encryptionManager.decrypt(encryptedData)
    }
}
