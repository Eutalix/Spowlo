plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // The compose plugin is applied here because this module contains Composable code.
    id("org.jetbrains.kotlin.plugin.compose")
}

// --- THE FIX ---
// Specifies that all Kotlin compilation tasks in this module must use JDK 17.
kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.bobbyesp.spowlo.color"
    compileSdk = 34 // Aligned to 34 for consistency with other modules.

    defaultConfig {
        minSdk = 24 // Aligned to 24 for consistency.
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    // Use the Compose BOM (Bill of Materials) to manage versions of Compose libraries.
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    api(composeBom)
    
    // Expose these core Compose libraries to any module that depends on :color.
    api("androidx.compose.ui:ui")
    api("androidx.compose.runtime:runtime")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.material3:material3")
    
    // Core KTX is a common dependency.
    api("androidx.core:core-ktx:1.13.1")
}