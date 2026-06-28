package com.taxisms.agent.security

import android.os.Build
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RootDetector - Qurilmaning root qilinganligini aniqlash.
 * Xavfsizlik maqsadida root qilingan telefonda SMS yuborish bloklanishi mumkin.
 */
@Singleton
class RootDetector @Inject constructor() {

    /**
     * Qurilma root qilinganligini tekshirish.
     */
    fun isRooted(): Boolean {
        return checkBuildTags() || checkSuPaths() || checkSuCommand()
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuPaths(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            try {
                process.inputStream.use { it.read() != -1 }
            } finally {
                process.destroy()
            }
        } catch (e: Exception) {
            false
        }
    }
}
