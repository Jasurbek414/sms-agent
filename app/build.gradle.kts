// =============================================================================
// SMS Agent Platform - App Module Build Configuration
// Asosiy ilova moduli: Compose UI, Hilt DI, Room DB, Retrofit, CameraX
// =============================================================================

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.taxisms.agent"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.taxisms.agent"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema eksport qilish
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.generateKotlin", "true")
        }

        // BuildConfig maydonlari
        buildConfigField("String", "API_BASE_URL", "\"https://api.smsagent.local/\"")
        buildConfigField("long", "SMS_RETRY_DELAY_MS", "5000L")
        buildConfigField("int", "MAX_SMS_RETRY_COUNT", "3")
    }

    // -------------------------------------------------------------------------
    // Build Variants: debug va release
    // -------------------------------------------------------------------------
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("String", "LOG_LEVEL", "\"VERBOSE\"")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            buildConfigField("String", "LOG_LEVEL", "\"ERROR\"")
        }
    }

    // -------------------------------------------------------------------------
    // Product Flavors: universal va whiteLabel
    // -------------------------------------------------------------------------
    flavorDimensions += "mode"

    productFlavors {
        create("universal") {
            dimension = "mode"
            // Universal rejim - istalgan taksi park bilan ishlaydi
            buildConfigField("boolean", "IS_WHITE_LABEL", "false")
            buildConfigField("String", "DEFAULT_PARK_NAME", "\"Universal SMS Agent\"")
        }

        create("whiteLabel") {
            dimension = "mode"
            // White Label rejim - bitta taksi park uchun maxsus brending
            applicationIdSuffix = ".wl"
            buildConfigField("boolean", "IS_WHITE_LABEL", "true")
            buildConfigField("String", "DEFAULT_PARK_NAME", "\"Partner Taxi\"")
        }
    }

    // -------------------------------------------------------------------------
    // Build Features
    // -------------------------------------------------------------------------
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // -------------------------------------------------------------------------
    // Compile Options
    // -------------------------------------------------------------------------
    compileOptions {
        // Java 17 (Compose uchun talab qilinadi)
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    // -------------------------------------------------------------------------
    // Packaging Options
    // -------------------------------------------------------------------------
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}

// =============================================================================
// Dependencies - Barcha kutubxonalar
// =============================================================================
dependencies {

    // -------------------------------------------------------------------------
    // Jetpack Compose BOM - Barcha Compose versiyalarini boshqaradi
    // -------------------------------------------------------------------------
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime-livedata")

    // Compose Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // -------------------------------------------------------------------------
    // AndroidX Core
    // -------------------------------------------------------------------------
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // -------------------------------------------------------------------------
    // Navigation - Compose Navigation
    // -------------------------------------------------------------------------
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // -------------------------------------------------------------------------
    // Hilt - Dependency Injection
    // -------------------------------------------------------------------------
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // -------------------------------------------------------------------------
    // Room - Local Database
    // -------------------------------------------------------------------------
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")

    // -------------------------------------------------------------------------
    // Retrofit + OkHttp - Network
    // -------------------------------------------------------------------------
    val retrofitVersion = "2.11.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-scalars:$retrofitVersion")

    val okHttpVersion = "4.12.0"
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")

    // -------------------------------------------------------------------------
    // Gson - JSON Serialization
    // -------------------------------------------------------------------------
    implementation("com.google.code.gson:gson:2.11.0")

    // -------------------------------------------------------------------------
    // DataStore - Settings Persistence
    // -------------------------------------------------------------------------
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // -------------------------------------------------------------------------
    // WorkManager - Background Tasks
    // -------------------------------------------------------------------------
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // -------------------------------------------------------------------------
    // CameraX - QR Code Scanner
    // -------------------------------------------------------------------------
    val cameraXVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")

    // ML Kit Barcode Scanner (QR kod o'qish uchun)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // -------------------------------------------------------------------------
    // Timber - Logging
    // -------------------------------------------------------------------------
    implementation("com.jakewharton.timber:timber:5.0.1")

    // -------------------------------------------------------------------------
    // Coroutines
    // -------------------------------------------------------------------------
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // -------------------------------------------------------------------------
    // Paging 3
    // -------------------------------------------------------------------------
    implementation("androidx.paging:paging-runtime-ktx:3.3.5")
    implementation("androidx.paging:paging-compose:3.3.5")

    // -------------------------------------------------------------------------
    // Splash Screen API
    // -------------------------------------------------------------------------
    implementation("androidx.core:core-splashscreen:1.0.1")

    // -------------------------------------------------------------------------
    // Test Dependencies
    // -------------------------------------------------------------------------
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.13")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
