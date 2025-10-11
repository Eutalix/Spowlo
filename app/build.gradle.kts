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

// --- THE FIX ---
// Specifies that all Kotlin compilation tasks in this module should use JDK 17.
// This resolves the JVM target compatibility error with KSP.
kotlin {
    jvmToolchain(17)
}

dependencies {
    // Local Project Modules
    implementation(project(":library"))
    implementation(project(":ffmpeg"))
    implementation(project(":color"))
    implementation(project(":common"))

    // ... (the rest of the dependencies block remains the same) ...
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.iconsExtended)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.ext.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.paging.runtime.ktx)
    implementation(libs.paging.compose)

    implementation(libs.coil.compose)

    implementation(libs.bundles.ktor)
    implementation(libs.mmkv)

    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.navigation.animation)

    implementation(libs.spotify.api.android)

    implementation(libs.android.material)
    implementation(libs.markdown)
    implementation(libs.customtabs)
    debugImplementation(libs.crash.handler)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}