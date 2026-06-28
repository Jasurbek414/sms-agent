// =============================================================================
// SMS Agent Platform - Root Build Configuration
// Barcha modullar uchun umumiy plugin sozlamalari
// =============================================================================

plugins {
    // Android Application plugin
    id("com.android.application") version "8.7.3" apply false

    // Kotlin Android plugin
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false

    // Kotlin Compose Compiler plugin (Kotlin 2.0+ uchun)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false

    // Hilt - Dependency Injection
    id("com.google.dagger.hilt.android") version "2.52" apply false

    // KSP - Kotlin Symbol Processing (Room, Hilt uchun)
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false

    // Kotlin Serialization (agar kerak bo'lsa)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}
