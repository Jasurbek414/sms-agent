package com.taxisms.agent.data.repository

import com.taxisms.agent.data.local.dao.SettingsDao
import com.taxisms.agent.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Settings Repository Implementatsiyasi
 *
 * Room Database orqali sozlamalarni saqlash va o'qish.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {

    override suspend fun getValue(key: String, defaultValue: String): String {
        return settingsDao.getValue(key) ?: defaultValue
    }

    override suspend fun setValue(key: String, value: String) {
        val entity = SettingsEntity(key = key, value = value)
        settingsDao.upsert(entity)
        Timber.d("Sozlama saqlandi: $key = $value")
    }

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return settingsDao.getValue(key)?.toBooleanStrictOrNull() ?: defaultValue
    }

    override suspend fun setBoolean(key: String, value: Boolean) {
        setValue(key, value.toString())
    }

    override fun observeValue(key: String): Flow<String?> {
        return settingsDao.observeValue(key)
    }

    override suspend fun remove(key: String) {
        settingsDao.delete(key)
    }

    override suspend fun clearAll() {
        settingsDao.deleteAll()
        Timber.w("Barcha sozlamalar tozalandi")
    }
}
