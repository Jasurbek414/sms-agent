package com.taxisms.agent.data.repository

import com.taxisms.agent.data.local.entity.ConnectionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Connection Repository Interfeysi
 *
 * Server ulanishi va WebSocket boshqaruvi.
 */
interface ConnectionRepository {

    /** Serverga ulanish */
    suspend fun connect(serverUrl: String, apiKey: String, parkName: String): Result<Unit>

    /** Serverdan uzilish */
    suspend fun disconnect()

    /** Faol ulanishni olish */
    suspend fun getActiveConnection(): ConnectionEntity?

    /** Faol ulanishni kuzatish */
    fun observeActiveConnection(): Flow<ConnectionEntity?>

    /** Barcha ulanishlarni olish */
    fun getAllConnections(): Flow<List<ConnectionEntity>>

    /** Ulanishni o'chirish */
    suspend fun deleteConnection(id: Long)
}
