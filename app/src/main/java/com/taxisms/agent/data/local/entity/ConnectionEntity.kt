package com.taxisms.agent.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Connection Entity - Server ulanish ma'lumotlari
 *
 * Taksi park serveriga ulanish konfiguratsiyasini saqlaydi.
 * QR kod skanerlash orqali olingan parametrlar shu yerda saqlanadi.
 */
@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Taksi park nomi */
    @ColumnInfo(name = "park_name")
    val parkName: String,

    /** Server URL manzili */
    @ColumnInfo(name = "server_url")
    val serverUrl: String,

    /** API kaliti */
    @ColumnInfo(name = "api_key")
    val apiKey: String,

    /** Faol ulanishmi */
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    /** Yaratilgan vaqt */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** Oxirgi ulanish vaqti */
    @ColumnInfo(name = "last_connected_at")
    val lastConnectedAt: Long? = null
)
