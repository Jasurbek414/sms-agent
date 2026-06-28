package com.taxisms.agent.data.repository

import com.taxisms.agent.data.local.dao.ConnectionDao
import com.taxisms.agent.data.local.entity.ConnectionEntity
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Connection Repository Implementatsiyasi
 *
 * Server ulanishi va ma'lumotlarni boshqarish.
 * TODO: WebSocket va real-time ulanish keyingi bosqichda qo'shiladi.
 */
class ConnectionRepositoryImpl @Inject constructor(
    private val connectionDao: ConnectionDao
) : ConnectionRepository {

    override suspend fun connect(
        serverUrl: String,
        apiKey: String,
        parkName: String
    ): Result<Unit> {
        return try {
            // Avvalgi faol ulanishlarni nofaol qilish
            connectionDao.deactivateAll()

            // Yangi ulanishni saqlash
            val connection = ConnectionEntity(
                parkName = parkName,
                serverUrl = serverUrl,
                apiKey = apiKey,
                isActive = true,
                lastConnectedAt = System.currentTimeMillis()
            )
            connectionDao.insert(connection)
            Timber.i("Serverga ulandi: $parkName ($serverUrl)")

            // TODO: WebSocket ulanishni boshlash
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Serverga ulanishda xatolik")
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        connectionDao.deactivateAll()
        Timber.i("Serverdan uzildi")
        // TODO: WebSocket ulanishni yopish
    }

    override suspend fun getActiveConnection(): ConnectionEntity? {
        return connectionDao.getActiveConnection()
    }

    override fun observeActiveConnection(): Flow<ConnectionEntity?> {
        return connectionDao.observeActiveConnection()
    }

    override fun getAllConnections(): Flow<List<ConnectionEntity>> {
        return connectionDao.getAllConnections()
    }

    override suspend fun deleteConnection(id: Long) {
        connectionDao.deleteById(id)
    }
}
