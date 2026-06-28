package com.taxisms.agent.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/**
 * SMS Agent Platform - Tarmoq Yordamchi Funksiyalari
 *
 * Internet ulanishi holatini tekshirish va kuzatish.
 * SMS Agent uchun server bilan aloqa mavjudligini aniqlash muhim.
 */
object NetworkUtils {

    // =========================================================================
    // TARMOQ TURI
    // =========================================================================

    /**
     * Tarmoq ulanish turi
     */
    enum class NetworkType {
        /** Wi-Fi orqali ulangan */
        WIFI,

        /** Mobil tarmoq orqali ulangan (3G/4G/5G) */
        MOBILE,

        /** Ethernet orqali ulangan */
        ETHERNET,

        /** Boshqa turdagi ulanish */
        OTHER,

        /** Internet yo'q */
        NONE
    }

    /**
     * Tarmoq holati ma'lumotlari
     */
    data class NetworkState(
        /** Internetga ulanganmi */
        val isConnected: Boolean,

        /** Tarmoq turi */
        val type: NetworkType,

        /** Signal kuchi (0-100, agar mavjud bo'lsa) */
        val signalStrength: Int? = null
    ) {
        companion object {
            /** Internet yo'q holati */
            val Disconnected = NetworkState(
                isConnected = false,
                type = NetworkType.NONE
            )
        }
    }

    // =========================================================================
    // ULANISH TEKSHIRISH
    // =========================================================================

    /**
     * Internetga ulanish mavjudligini tekshirish
     *
     * @param context Android kontekst
     * @return true - internet mavjud, false - yo'q
     */
    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Joriy tarmoq turini aniqlash
     *
     * @param context Android kontekst
     * @return NetworkType - Wi-Fi, Mobile, Ethernet, Other yoki None
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return NetworkType.NONE

        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkType.NONE

        // Internet mavjudligini tekshirish
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkType.NONE
        }

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }

    /**
     * To'liq tarmoq holati ma'lumotlarini olish
     *
     * @param context Android kontekst
     * @return NetworkState - ulanish holati va turi
     */
    fun getNetworkState(context: Context): NetworkState {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return NetworkState.Disconnected

        val network = connectivityManager.activeNetwork ?: return NetworkState.Disconnected
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkState.Disconnected

        val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        if (!isConnected) return NetworkState.Disconnected

        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }

        // Signal kuchini olishga urinish
        val signalStrength = try {
            capabilities.signalStrength.takeIf { it != Int.MIN_VALUE }?.let {
                // Signal kuchini 0-100 oralig'iga normalizatsiya qilish
                ((it + 120).coerceIn(0, 120) * 100 / 120)
            }
        } catch (e: Exception) {
            null
        }

        return NetworkState(
            isConnected = true,
            type = type,
            signalStrength = signalStrength
        )
    }

    // =========================================================================
    // REAL-TIME TARMOQ KUZATISH
    // =========================================================================

    /**
     * Tarmoq holatini real-time kuzatish (Flow)
     * Tarmoq o'zgarganda avtomatik yangilanadi
     *
     * Foydalanish:
     * ```kotlin
     * NetworkUtils.observeNetworkState(context)
     *     .collect { state ->
     *         when (state.type) {
     *             NetworkType.WIFI -> "Wi-Fi ulangan"
     *             NetworkType.MOBILE -> "Mobil tarmoq"
     *             NetworkType.NONE -> "Internet yo'q"
     *             else -> "Boshqa"
     *         }
     *     }
     * ```
     *
     * @param context Android kontekst
     * @return Flow<NetworkState> - tarmoq holati oqimi
     */
    fun observeNetworkState(context: Context): Flow<NetworkState> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

        // Dastlabki holatni yuborish
        trySend(getNetworkState(context))

        // Tarmoq o'zgarishlarini kuzatuvchi callback
        val networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                Timber.d("Tarmoq ulanishi mavjud: $network")
                trySend(getNetworkState(context))
            }

            override fun onLost(network: Network) {
                Timber.d("Tarmoq ulanishi uzildi: $network")
                trySend(NetworkState.Disconnected)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                Timber.d("Tarmoq imkoniyatlari o'zgardi")
                trySend(getNetworkState(context))
            }
        }

        // Barcha tarmoq o'zgarishlarini kuzatish uchun so'rov
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        // Callback-ni ro'yxatdan o'tkazish
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Flow bekor qilinganda callback-ni o'chirish
        awaitClose {
            Timber.d("Tarmoq kuzatish to'xtatildi")
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged() // Faqat o'zgarishlarni yuborish (takroriy qiymatlarni filtrlash)

    /**
     * Faqat ulanish holatini (boolean) kuzatish
     *
     * @param context Android kontekst
     * @return Flow<Boolean> - true: ulangan, false: uzilgan
     */
    fun observeConnectivity(context: Context): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

        // Dastlabki holatni yuborish
        trySend(isConnected(context))

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()

    // =========================================================================
    // YORDAMCHI FUNKSIYALAR
    // =========================================================================

    /**
     * Tarmoq turini o'zbekcha matn sifatida olish
     *
     * @param type Tarmoq turi
     * @return O'zbekcha tarmoq turi nomi
     */
    fun getNetworkTypeName(type: NetworkType): String {
        return when (type) {
            NetworkType.WIFI -> "Wi-Fi"
            NetworkType.MOBILE -> "Mobil tarmoq"
            NetworkType.ETHERNET -> "Ethernet"
            NetworkType.OTHER -> "Boshqa"
            NetworkType.NONE -> "Ulanish yo'q"
        }
    }

    /**
     * Wi-Fi orqali ulanganligini tekshirish
     * Katta hajmdagi ma'lumotlarni yuborish uchun foydali
     */
    fun isWifiConnected(context: Context): Boolean {
        return getNetworkType(context) == NetworkType.WIFI
    }

    /**
     * Mobil tarmoq orqali ulanganligini tekshirish
     */
    fun isMobileConnected(context: Context): Boolean {
        return getNetworkType(context) == NetworkType.MOBILE
    }
}
