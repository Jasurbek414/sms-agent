package com.taxisms.agent.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Settings Repository Interfeysi
 *
 * Ilova sozlamalarini o'qish va yozish.
 */
interface SettingsRepository {

    /** Sozlama qiymatini olish */
    suspend fun getValue(key: String, defaultValue: String = ""): String

    /** Sozlama qiymatini saqlash */
    suspend fun setValue(key: String, value: String)

    /** Boolean sozlama olish */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    /** Boolean sozlama saqlash */
    suspend fun setBoolean(key: String, value: Boolean)

    /** Sozlama qiymatini kuzatish (Flow) */
    fun observeValue(key: String): Flow<String?>

    /** Sozlamani o'chirish */
    suspend fun remove(key: String)

    /** Barcha sozlamalarni tozalash */
    suspend fun clearAll()
}
