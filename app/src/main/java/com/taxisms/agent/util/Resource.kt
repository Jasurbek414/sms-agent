package com.taxisms.agent.util

/**
 * Resource - Ma'lumotlarni yuklash holatini ifodalovchi yordamchi sinf.
 * Clean Architecture uchun standart javob o'ramidir.
 */
sealed class Resource<out T> {
    
    /** Muvaffaqiyatli natija */
    data class Success<out T>(val data: T) : Resource<T>()
    
    /** Xatolik yuz bergandagi natija */
    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>()
    
    /** Yuklanish holati */
    object Loading : Resource<Nothing>()
}
