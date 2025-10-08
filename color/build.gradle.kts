// UPDATED: Replaced aliases with standard plugin IDs for compatibility.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(21)
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    namespace = "com.bobbyesp.spowlo.color"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17 // Corrected duplicate line
    }
    buildTypes {
        all {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            isMinifyEnabled = false
        }
    }
    // The composeOptions block needs to be inside the android block
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // Specify the version explicitly
    }
}

dependencies {
    // Using explicit versions from a typical libs.versions.toml for stability
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    api(composeBom)
    
    api("androidx.compose.ui:ui")
    api("androidx.compose.runtime:runtime")
    api("androidx.core:core-ktx:1.13.1")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.material3:material3")
}