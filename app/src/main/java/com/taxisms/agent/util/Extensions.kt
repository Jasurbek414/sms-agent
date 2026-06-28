@file:Suppress("unused")

package com.taxisms.agent.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * SMS Agent Platform - Kengaytma Funksiyalar (Extensions)
 *
 * Kotlin extension function-lar orqali tez-tez ishlatiluvchi
 * operatsiyalarni soddalashtirish.
 */

// =============================================================================
// STRING KENGAYTMALARI - Telefon raqam formatlash
// =============================================================================

/**
 * Telefon raqamini standart formatga keltirish
 * Faqat raqamlar va '+' belgisini qoldiradi
 *
 * Misollar:
 * - "+998 (90) 123-45-67" → "+998901234567"
 * - "998901234567" → "+998901234567"
 * - "90 123 45 67" → "+99890123456" (O'zbekiston kodi qo'shiladi)
 *
 * @return Formatlangan telefon raqam yoki null (agar noto'g'ri bo'lsa)
 */
fun String.toPhoneFormat(): String? {
    // Faqat raqamlar va '+' belgisini ajratib olish
    val cleaned = this.filter { it.isDigit() || it == '+' }

    return when {
        // Bo'sh yoki juda qisqa
        cleaned.length < 9 -> null

        // '+' bilan boshlanadi - xalqaro format
        cleaned.startsWith("+") -> {
            if (cleaned.length >= 12) cleaned else null
        }

        // '998' bilan boshlanadi - O'zbekiston kodi mavjud
        cleaned.startsWith("998") && cleaned.length == 12 -> "+$cleaned"

        // 9 raqamli (mahalliy raqam) - O'zbekiston kodini qo'shish
        cleaned.length == 9 -> "+998$cleaned"

        // Boshqa formatlar
        cleaned.length >= 10 -> "+$cleaned"

        else -> null
    }
}

/**
 * Telefon raqamini ko'rsatish uchun formatlash
 * Foydalanuvchiga chiroyli ko'rsatish uchun
 *
 * Misol: "+998901234567" → "+998 (90) 123-45-67"
 */
fun String.toDisplayPhoneFormat(): String {
    val cleaned = this.filter { it.isDigit() }
    return when {
        cleaned.length == 12 && cleaned.startsWith("998") -> {
            "+998 (${cleaned.substring(3, 5)}) ${cleaned.substring(5, 8)}-${cleaned.substring(8, 10)}-${cleaned.substring(10, 12)}"
        }
        else -> this
    }
}

/**
 * Telefon raqamini maxfiy qilish (maskalash)
 * Xavfsizlik uchun raqamning bir qismini yashirish
 *
 * Misol: "+998901234567" → "+998 90 ***-**-67"
 */
fun String.maskPhoneNumber(): String {
    val cleaned = this.filter { it.isDigit() }
    return when {
        cleaned.length >= 12 -> {
            "+${cleaned.substring(0, 5)} ***-**-${cleaned.takeLast(2)}"
        }
        cleaned.length >= 7 -> {
            "${cleaned.take(3)}****${cleaned.takeLast(2)}"
        }
        else -> "***"
    }
}

/**
 * String bo'sh yoki faqat bo'shliqlardan iboratligini tekshirish
 */
fun String?.isNotNullOrBlank(): Boolean {
    return !this.isNullOrBlank()
}

// =============================================================================
// CONTEXT KENGAYTMALARI - Android kontekst uchun
// =============================================================================

/**
 * Internetga ulanish mavjudligini tekshirish
 *
 * @return true - internet mavjud, false - internet yo'q
 */
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false

    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/**
 * Qisqa Toast xabar ko'rsatish
 *
 * @param message Ko'rsatiladigan xabar matni
 * @param isLong true bo'lsa, uzun vaqt ko'rsatiladi
 */
fun Context.showToast(message: String, isLong: Boolean = false) {
    Toast.makeText(
        this,
        message,
        if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    ).show()
}

/**
 * Toast xabarni string resurs ID orqali ko'rsatish
 */
fun Context.showToast(messageResId: Int, isLong: Boolean = false) {
    showToast(getString(messageResId), isLong)
}

// =============================================================================
// DATE KENGAYTMALARI - Sana va vaqt formatlash
// =============================================================================

/**
 * Long timestamp-ni formatlangan sana stringiga aylantirish
 *
 * @param pattern Sana formati (default: "dd.MM.yyyy HH:mm")
 * @return Formatlangan sana string
 */
fun Long.toFormattedDate(pattern: String = "dd.MM.yyyy HH:mm"): String {
    return try {
        val sdf = SimpleDateFormat(pattern, Locale("uz", "UZ"))
        sdf.format(Date(this))
    } catch (e: Exception) {
        Timber.e(e, "Sana formatlashda xatolik: timestamp=$this")
        ""
    }
}

/**
 * Long timestamp-ni nisbiy vaqt stringiga aylantirish
 * Foydalanuvchiga tushunarli format: "5 daqiqa oldin", "2 soat oldin", va h.k.
 *
 * @return Nisbiy vaqt string
 */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        // Kelajakdagi vaqt
        diff < 0 -> "hozirgina"

        // 1 daqiqadan kam
        diff < TimeUnit.MINUTES.toMillis(1) -> "hozirgina"

        // 1 soatdan kam
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$minutes daqiqa oldin"
        }

        // 1 kundan kam
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "$hours soat oldin"
        }

        // 7 kundan kam
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "$days kun oldin"
        }

        // 30 kundan kam
        diff < TimeUnit.DAYS.toMillis(30) -> {
            val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
            "$weeks hafta oldin"
        }

        // 365 kundan kam
        diff < TimeUnit.DAYS.toMillis(365) -> {
            val months = TimeUnit.MILLISECONDS.toDays(diff) / 30
            "$months oy oldin"
        }

        // 1 yildan ko'p
        else -> {
            val years = TimeUnit.MILLISECONDS.toDays(diff) / 365
            "$years yil oldin"
        }
    }
}

/**
 * Date obyektini formatlangan stringga aylantirish
 */
fun Date.toFormattedString(pattern: String = "dd.MM.yyyy HH:mm:ss"): String {
    return try {
        val sdf = SimpleDateFormat(pattern, Locale("uz", "UZ"))
        sdf.format(this)
    } catch (e: Exception) {
        Timber.e(e, "Sana formatlashda xatolik")
        ""
    }
}

// =============================================================================
// FLOW KENGAYTMALARI - Kotlin Coroutines Flow uchun
// =============================================================================

fun <T> Flow<T>.asResource(): Flow<Resource<T>> {
    return this
        .map<T, Resource<T>> { data ->
            Resource.Success(data)
        }
        .onStart {
            emit(Resource.Loading)
        }
        .catch { exception ->
            Timber.e(exception, "Flow xatoligi")
            emit(Resource.Error(exception.message ?: "Noma'lum xatolik"))
        }
}
