package com.taxisms.agent.security

import android.os.Build
import timber.log.Timber
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EncryptionManager - Android Keystore API yordamchi moduli.
 * API Kalit va JWT Tokenlarni shifrlangan formatda saqlash va o'qish.
 */
@Singleton
class EncryptionManager @Inject constructor() {

    private val provider = "AndroidKeyStore"
    private val alias = "SmsAgentSecurityKeyAlias"

    init {
        createKeyIfNeeded()
    }

    private fun createKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(provider).apply { load(null) }
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider)
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(provider).apply { load(null) }
        return keyStore.getKey(alias, null) as SecretKey
    }

    /**
     * Matnni AES-GCM-NoPadding usuli orqali shifrlash.
     */
    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            // IV va shifrlangan ma'lumotlarni birlashtirib saqlaymiz (iv:encryptedBytes)
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            Timber.e(e, "Shifrlashda xatolik!")
            plainText // Fallback
        }
    }

    /**
     * Shifrlangan matnni asliga qaytarish (decrpyt).
     */
    fun decrypt(encryptedText: String): String {
        return try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            val ivSize = 12 // AES-GCM default IV size
            val iv = ByteArray(ivSize)
            val encryptedBytes = ByteArray(combined.size - ivSize)
            System.arraycopy(combined, 0, iv, 0, ivSize)
            System.arraycopy(combined, ivSize, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "Deshifrlashda xatolik!")
            encryptedText // Fallback
        }
    }
}
