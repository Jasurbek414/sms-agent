package com.taxisms.agent.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Settings Entity - Ilova sozlamalari
 *
 * Kalit-qiymat juftlari sifatida sozlamalarni saqlaydi.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "setting_key")
    val key: String,

    @ColumnInfo(name = "setting_value")
    val value: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
