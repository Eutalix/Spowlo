// UPDATED: Replaced aliases with standard plugin IDs.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    // REMOVED: "maven-publish" is not needed for an internal module.
}

// NEW: Aligns the Kotlin toolchain with the main app module.
kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.bobbyesp.spotdl_common"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // UPDATED: Standardized Java and Kotlin target versions for library compatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Using explicit versions for clarity, matching what Spowlo likely uses.
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-compress:1.26.2")
    
    // Ktor bundle (client, content-negotiation, serialization)
    val ktorVersion = "2.3.12"
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

// REMOVED: The entire 'afterEvaluate' publishing block is not needed.