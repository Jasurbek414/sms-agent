package com.taxisms.agent.di

import com.taxisms.agent.data.repository.ConnectionRepository
import com.taxisms.agent.data.repository.ConnectionRepositoryImpl
import com.taxisms.agent.data.repository.SettingsRepository
import com.taxisms.agent.data.repository.SettingsRepositoryImpl
import com.taxisms.agent.data.repository.SmsRepository
import com.taxisms.agent.data.repository.SmsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI - Repository Moduli
 *
 * Repository interfeys va implementatsiyalarini bog'laydi.
 * Abstraksiya qatlami orqali testlash va almashtirish osonlashadi.
 *
 * Repository-lar:
 * - SmsRepository: SMS yuborish, log saqlash, holat kuzatish
 * - ConnectionRepository: Server ulanishi, WebSocket, QR skanerlash
 * - SettingsRepository: Ilova sozlamalari, foydalanuvchi parametrlari
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * SMS Repository - SMS operatsiyalari uchun
     *
     * Mas'uliyatlari:
     * - SMS xabarlarni yuborish
     * - SMS loglarini saqlash va o'qish
     * - SMS holati (yuborildi/yetkazildi/xatolik) kuzatish
     * - SMS navbatini boshqarish
     */
    @Binds
    @Singleton
    abstract fun bindSmsRepository(
        impl: SmsRepositoryImpl
    ): SmsRepository

    /**
     * Connection Repository - Server ulanishi uchun
     *
     * Mas'uliyatlari:
     * - Taksi park serveriga ulanish
     * - WebSocket orqali real-time aloqa
     * - QR kod orqali ulanish parametrlarini o'qish
     * - Ulanish holatini kuzatish
     */
    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl
    ): ConnectionRepository

    /**
     * Settings Repository - Sozlamalar uchun
     *
     * Mas'uliyatlari:
     * - Ilova sozlamalarini o'qish/yozish
     * - DataStore orqali reaktiv sozlamalar
     * - Default qiymatlarni boshqarish
     * - Sozlamalar migratsiyasi
     */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
