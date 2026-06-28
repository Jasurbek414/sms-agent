package com.taxisms.agent.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.taxisms.agent.BuildConfig
import com.taxisms.agent.data.local.AppDatabase
import com.taxisms.agent.data.local.dao.ConnectionDao
import com.taxisms.agent.data.local.dao.SmsLogDao
import com.taxisms.agent.data.local.dao.SettingsDao
import com.taxisms.agent.data.remote.api.AgentApiService
import com.taxisms.agent.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt DI - Asosiy App Moduli
 *
 * Ilova bo'ylab ishlatiluvchi singleton obyektlarni ta'minlaydi:
 * - Room Database va barcha DAO-lar
 * - Retrofit va OkHttpClient (tarmoq uchun)
 * - Gson (JSON serialization)
 * - DataStore (sozlamalar uchun)
 * - SharedPreferences (legacy sozlamalar uchun)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // =========================================================================
    // GSON - JSON Serialization/Deserialization
    // =========================================================================

    /**
     * Gson instansiyasini yaratish
     * Barcha JSON konvertatsiya operatsiyalari uchun ishlatiladi
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // ISO 8601 format
            .setPrettyPrinting() // Chiroyli JSON chiqishi (debug uchun)
            .serializeNulls() // null qiymatlarni ham serialize qilish
            .setLenient() // Qat'iy bo'lmagan parsing
            .create()
    }

    // =========================================================================
    // OKHTTP CLIENT - HTTP so'rovlar uchun
    // =========================================================================

    /**
     * OkHttpClient instansiyasini yaratish
     * Barcha HTTP so'rovlar uchun asos
     * - Timeout sozlamalari
     * - Logging interceptor (debug rejimda)
     * - Retry mexanizmi
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            // Ulanish timeout-lari
            .connectTimeout(Constants.Network.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.Network.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.Network.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Ping interval (WebSocket uchun)
            .pingInterval(Constants.Network.PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
            // Muvaffaqiyatsiz ulanishlarni qayta urinish
            .retryOnConnectionFailure(true)

        // Debug rejimda HTTP loglarni ko'rsatish
        if (BuildConfig.ENABLE_LOGGING) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Timber.tag("OkHttp").d(message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    // =========================================================================
    // RETROFIT - REST API Client
    // =========================================================================

    /**
     * Retrofit instansiyasini yaratish
     * Server bilan REST API orqali aloqa qilish uchun
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create()) // String response uchun
            .addConverterFactory(GsonConverterFactory.create(gson)) // JSON response uchun
            .build()
    }

    /**
     * Agent API Service instansiyasini yaratish
     * Taksi park serveri bilan aloqa uchun
     */
    @Provides
    @Singleton
    fun provideAgentApiService(retrofit: Retrofit): AgentApiService {
        return retrofit.create(AgentApiService::class.java)
    }

    // =========================================================================
    // ROOM DATABASE - Local ma'lumotlar bazasi
    // =========================================================================

    /**
     * Room Database instansiyasini yaratish
     * SMS loglar, ulanish ma'lumotlari va sozlamalar saqlanadi
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.Database.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // Migratsiya yo'q bo'lsa, qayta yaratish
            .build()
    }

    /**
     * SMS Log DAO - SMS xabarlar tarixini boshqarish
     */
    @Provides
    fun provideSmsLogDao(database: AppDatabase): SmsLogDao {
        return database.smsLogDao()
    }

    /**
     * Connection DAO - Server ulanish ma'lumotlarini boshqarish
     */
    @Provides
    fun provideConnectionDao(database: AppDatabase): ConnectionDao {
        return database.connectionDao()
    }

    /**
     * Settings DAO - Ilova sozlamalarini boshqarish
     */
    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }

    // =========================================================================
    // DATASTORE - Zamonaviy sozlamalar saqlash
    // =========================================================================

    /**
     * DataStore Preferences instansiyasini yaratish
     * SharedPreferences-ning zamonaviy almashtirgichi
     * Coroutine va Flow bilan ishlaydi
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = {
                context.preferencesDataStoreFile(Constants.DataStore.PREFERENCES_NAME)
            }
        )
    }

    // =========================================================================
    // SHARED PREFERENCES - Legacy sozlamalar (orqaga moslik uchun)
    // =========================================================================

    /**
     * SharedPreferences instansiyasini yaratish
     * Legacy kod va oddiy sozlamalar uchun
     * Yangi kodda DataStore ishlatish tavsiya etiladi
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(
            Constants.Preferences.PREF_NAME,
            Context.MODE_PRIVATE
        )
    }
}
