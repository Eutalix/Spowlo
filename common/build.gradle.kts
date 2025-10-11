plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// --- THE FIX ---
// Specifies that all Kotlin compilation tasks in this module must use JDK 17.
// This ensures consistency across the project and resolves JVM target compatibility errors.
kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.bobbyesp.spotdl_common"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
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
    
    // Standardized Java and Kotlin target versions for library compatibility.
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
    // Ktor for networking
    val ktorVersion = "2.3.8" // Using the version from your TOML for consistency
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    
    // Other utilities
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-compress:1.26.1") // Consistent version
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}