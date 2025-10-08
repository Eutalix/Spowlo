plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// REMOVED: The old java block. The jvmToolchain will handle this.
// java { ... }

// KEPT: This aligns the Kotlin compiler with the main app module.
kotlin {
    jvmToolchain(21)
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    namespace = "com.bobbyesp.spowlo.color"
    
    // UPDATED: This block now aligns the Java compiler with the Kotlin compiler.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Standard for libraries
        targetCompatibility = JavaVersion.VERSION_1_8 // Standard for libraries
    }

    kotlinOptions {
        jvmTarget = "1.8" // Ensure Kotlin output is also compatible
    }
    
    buildTypes {
        all {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            isMinifyEnabled = false
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // ... dependencies remain the same ...
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    api(composeBom)
    
    api("androidx.compose.ui:ui")
    api("androidx.compose.runtime:runtime")
    api("androidx.core:core-ktx:1.13.1")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.material3:material3")
}