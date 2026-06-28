# =============================================================================
# SMS Agent Platform - ProGuard Qoidalari
# Professional obfuscation va optimization konfiguratsiyasi
# =============================================================================

# =============================================================================
# UMUMIY SOZLAMALAR
# =============================================================================

# Optimallashtirish darajasi
-optimizationpasses 5
-dontusefragmentidentifiers

# Source fayl va line raqamlarini saqlash (crash report uchun)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Annotation-larni saqlash
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# =============================================================================
# KOTLIN
# =============================================================================

# Kotlin metadata saqlash
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Kotlin Serialization (agar ishlatilsa)
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class com.taxisms.agent.**$$serializer { *; }
-keepclassmembers class com.taxisms.agent.** {
    *** Companion;
}

# =============================================================================
# RETROFIT
# =============================================================================

# Retrofit interfeys va callback-larni saqlash
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Retrofit optional dependencies
-dontwarn okio.**
-dontwarn retrofit2.Platform$Java8

# Retrofit response modellarini saqlash
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# =============================================================================
# OKHTTP
# =============================================================================

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# OkHttp platform va SSL
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn org.conscrypt.ConscryptHostnameVerifier

# WebSocket
-keep class okhttp3.internal.ws.** { *; }

# =============================================================================
# ROOM DATABASE
# =============================================================================

# Room entity va DAO-larni saqlash
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

-dontwarn androidx.room.paging.**

# =============================================================================
# HILT / DAGGER
# =============================================================================

# Hilt generated kodlarni saqlash
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Hilt annotation-larni saqlash
-keepclasseswithmembernames class * {
    @dagger.* <fields>;
}
-keepclasseswithmembernames class * {
    @javax.inject.* <fields>;
}
-keepclasseswithmembernames class * {
    @dagger.* <methods>;
}

# =============================================================================
# GSON
# =============================================================================

# Gson serialization/deserialization
-keepattributes Signature
-keepattributes *Annotation*

# Gson TypeAdapter va TypeAdapterFactory
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson bilan ishlatiladigan modellar
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# =============================================================================
# ILOVA MODELLARI VA ENTITY-LAR
# =============================================================================

# Data modellar - API response va request
-keep class com.taxisms.agent.data.model.** { *; }
-keepclassmembers class com.taxisms.agent.data.model.** { *; }

# Room Entity-lar
-keep class com.taxisms.agent.data.local.entity.** { *; }
-keepclassmembers class com.taxisms.agent.data.local.entity.** { *; }

# DTO (Data Transfer Objects)
-keep class com.taxisms.agent.data.remote.dto.** { *; }
-keepclassmembers class com.taxisms.agent.data.remote.dto.** { *; }

# Enum-larni saqlash
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# =============================================================================
# JETPACK COMPOSE
# =============================================================================

# Compose runtime
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# =============================================================================
# CAMERAX / ML KIT
# =============================================================================

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ML Kit Barcode Scanner
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# =============================================================================
# WORKMANAGER
# =============================================================================

# WorkManager Worker class-larni saqlash
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# Hilt Worker
-keep class * extends androidx.hilt.work.HiltWorker { *; }

# =============================================================================
# DATASTORE
# =============================================================================

-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# =============================================================================
# ANDROID KOMPONENTLAR
# =============================================================================

# BroadcastReceiver-larni saqlash
-keep class com.taxisms.agent.receiver.** { *; }

# Service-larni saqlash
-keep class com.taxisms.agent.service.** { *; }

# Application class
-keep class com.taxisms.agent.App { *; }

# =============================================================================
# OGOHLANTIRISHLARNI BOSTIRISH
# =============================================================================

-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
