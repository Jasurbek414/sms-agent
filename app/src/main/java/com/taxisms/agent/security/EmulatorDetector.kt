package com.taxisms.agent.security

import android.os.Build
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EmulatorDetector - Qurilmaning emulyator ekanligini aniqlash.
 * Real SMS yuborish faqat haqiqiy qurilmada ishlaydi.
 */
@Singleton
class EmulatorDetector @Inject constructor() {

    /**
     * Qurilma emulyator ekanligini aniqlash.
     */
    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.BOARD.lowercase(Locale.ROOT).contains("nox")
                || Build.BOOTLOADER.lowercase(Locale.ROOT).contains("nox")
                || Build.HARDWARE.lowercase(Locale.ROOT).contains("nox")
                || Build.PRODUCT.lowercase(Locale.ROOT).contains("nox")
    }
}
