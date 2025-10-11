import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.compose.compiler)
}

// Versioning class remains the same.
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
    compileSdk = 34 // Keep this consistent with :library module

    defaultConfig {
        applicationId = "com.bobbyesp.spowlo"
        minSdk = 26
        targetSdk = 34
        versionCode = currentVersion.toVersionCode()
        versionName = currentVersion.toVersionName().let { if (!splitApks) "$it-full" else it }

        // Resolves the 'bundling' flavor dimension from the library modules.
        missingDimensionStrategy("bundling", "bundled")

        // Resolves the manifest merger failure.
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
    implementation(project(":library")) // This will expose spotify-api-kotlin via `api` configuration
    implementation(project(":ffmpeg"))
    implementation(project(":color"))
    implementation(project(":common"))

    // AndroidX & Core KTX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Window Size Class - Fixes 'windowsizeclass' unresolved reference
    implementation(libs.androidx.compose.material3.windowSizeClass)
    
    // Lifecycle & ViewModel for Compose
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)

    // Navigation for Compose
    implementation(libs.androidx.navigation.compose)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room for Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Paging 3 for paginated lists
    implementation(libs.paging.runtime.ktx)
    implementation(libs.paging.compose)

    // Coil for Image Loading
    implementation(libs.coil.compose)

    // Network (Ktor)
    implementation(libs.bundles.ktor)

    // Key-Value Storage
    implementation(libs.mmkv)

    // Accompanist - Only keep the ones that are still needed and updated.
    // System UI Controller is still useful.
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    
    // Other Utilities
    implementation(libs.markdown)
    implementation(libs.customtabs)
    debugImplementation(libs.crash.handler)

    // Testing
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}