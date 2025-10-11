import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("kotlin-android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

// Versioning class and other top-level properties remain the same
sealed class Version(
    open val versionMajor: Int, val versionMinor: Int, val versionPatch: Int, val versionBuild: Int = 0
) {
    abstract fun toVersionName(): String
    fun toVersionCode(): Int = versionMajor * 1000000 + versionMinor * 10000 + versionPatch * 100 + versionBuild
    class Stable(versionMajor: Int, versionMinor: Int, versionPatch: Int) : Version(versionMajor, versionMinor, versionPatch) {
        override fun toVersionName(): String = "${versionMajor}.${versionMinor}.${versionPatch}"
    }
}
val currentVersion: Version = Version.Stable(versionMajor = 1, versionMinor = 5, versionPatch = 3)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val splitApks = !project.hasProperty("noSplits")

android {
    // ... (signingConfigs block remains the same)

    namespace = "com.bobbyesp.spowlo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bobbyesp.spowlo"
        minSdk = 26
        targetSdk = 34
        versionCode = currentVersion.toVersionCode()
        versionName = currentVersion.toVersionName().run { if (!splitApks) "$this-full" else this }
        
        missingDimensionStrategy("bundling", "bundled")
        
        manifestPlaceholders += mapOf(
            "redirectSchemeName" to "spowlo-auth",
            "redirectHostName" to "callback"
        )
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }
    
    // ... (buildTypes and other android blocks remain the same) ...

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Local Project Modules
    implementation(project(":library"))
    implementation(project(":ffmpeg"))
    implementation(project(":color"))
    implementation(project(":common"))

    // AndroidX & Core KTX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.iconsExtended)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.ext.compiler)

// Room for Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Paging 3
    implementation(libs.paging.runtime.ktx)
    implementation(libs.paging.compose)

    // Coil for Image Loading
    implementation(libs.coil.compose)

    // Network (Ktor)
    implementation(libs.bundles.ktor)

    // Key-Value Storage
    implementation(libs.mmkv)

    // Accompanist
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.navigation.animation)

    // Spotify API
    implementation(libs.spotify.api.android)

    // Other Utilities
    implementation(libs.android.material)
    implementation(libs.markdown)
    implementation(libs.customtabs)
    debugImplementation(libs.crash.handler)

    // Testing
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}