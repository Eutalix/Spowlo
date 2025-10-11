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

// A sealed class to manage app versioning in a structured and type-safe way.
sealed class Version(
    open val versionMajor: Int,
    val versionMinor: Int,
    val versionPatch: Int,
    val versionBuild: Int = 0
) {
    abstract fun toVersionName(): String

    fun toVersionCode(): Int =
        versionMajor * 1000000 + versionMinor * 10000 + versionPatch * 100 + versionBuild

    class Stable(versionMajor: Int, versionMinor: Int, versionPatch: Int) :
        Version(versionMajor, versionMinor, versionPatch) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}"
    }
}

val currentVersion: Version = Version.Stable(
    versionMajor = 1,
    versionMinor = 5,
    versionPatch = 3,
)

val keystorePropertiesFile = rootProject.file("keystore.properties")
val splitApks = !project.hasProperty("noSplits")

android {
    // Configuration for release signing, loaded from keystore.properties if it exists.
    if (keystorePropertiesFile.exists()) {
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
        signingConfigs {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
                storeFile = file(keystoreProperties["storeFile"]!!)
                storePassword = keystoreProperties["storePassword"].toString()
            }
        }
    }

    namespace = "com.bobbyesp.spowlo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bobbyesp.spowlo"
        minSdk = 26
        targetSdk = 34
        versionCode = currentVersion.toVersionCode()
        versionName = currentVersion.toVersionName().run {
            if (!splitApks) "$this-full"
            else this
        }

        missingDimensionStrategy("bundling", "bundled")

        manifestPlaceholders += mapOf(
            "redirectSchemeName" to "spowlo-auth",
            "redirectHostName" to "callback"
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

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
    implementation(libs.androidx.compose.material) // For Material 2 components if needed
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
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
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // Coil for Image Loading
    implementation(libs.coil.kt.compose)

    // Network (Ktor)
    implementation(libs.bundles.ktor)

    // Key-Value Storage
    implementation(libs.mmkv)

    // Accompanist - Only keep the ones that are still needed and updated.
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.navigation.animation)

    // Spotify API - Added directly to app for components that use it, while library exposes it via `api`
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