plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// REMOVED: This block is legacy. We will use kotlinOptions.jvmTarget instead for consistency.
/*
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
*/

// REMOVED: jvmToolchain sets the JDK version for the entire Gradle task,
// while jvmTarget sets the bytecode version. We need to align the bytecode.
/*
kotlin {
    jvmToolchain(21)
}
*/

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    namespace = "com.bobbyesp.spowlo.color"

    // MODIFIED: Ensure both Java and Kotlin compile to the same bytecode version.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // MODIFIED: Added kotlinOptions to explicitly set the Kotlin bytecode target to 17.
    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        all {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            isMinifyEnabled = false
        }
    }
}
dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.core.ktx)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
}