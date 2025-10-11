plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    // The 'maven-publish' plugin is used to publish this library as a standalone artifact.
    // It can be removed if you only intend to use this as a local module.
    id("maven-publish")
}

kotlin {
    // Aligns the Kotlin JVM toolchain version across all modules for consistency.
    jvmToolchain(21)
}

android {
    namespace = "com.bobbyesp.library"
    compileSdk = 34 // Use a recent SDK version for access to modern APIs.

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")

        // Securely pass Spotify credentials from the project's 'local.properties' file
        // into the BuildConfig class, making them accessible in Kotlin code.
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"${findProperty("CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"${findProperty("CLIENT_SECRET") ?: ""}\"")
    }

    // Define product flavors to handle different library distributions.
    flavorDimensions.add("bundling")
    productFlavors {
        create("bundled") {
            dimension = "bundling"
        }
        create("nonbundled") {
            dimension = "bundling"
        }
    }

    // Configure source sets for each flavor.
    sourceSets {
        getByName("nonbundled") {
            java.srcDir("src/nonbundled/java")
            jniLibs.srcDirs("src/nonbundled/jniLibs")
        }
        getByName("bundled") {
            java.srcDir("src/bundled/java")
            jniLibs.srcDirs("src/bundled/jniLibs")
        }
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

    // Standardize Java and Kotlin target versions for broad compatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    // Enable the BuildConfig feature.
    buildFeatures {
        buildConfig = true
    }

    // Configuration for publishing the library to a Maven repository.
    publishing {
        singleVariant("bundledRelease") {
            withSourcesJar()
            withJavadocJar()
        }
        singleVariant("nonbundledRelease") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    // Declare a dependency on the ':common' module for shared utility code.
    implementation(project(":common"))
    
    // Core Android & Kotlin libraries.
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Utility library for file operations.
    implementation("commons-io:commons-io:2.11.0")

    // --- THE FIX ---
    // Use `api` instead of `implementation`.
    // This exposes the spotify-api-kotlin library to any module that depends on this :library module
    // (like the :app module). This resolves all the 'Unresolved reference' errors for classes
    // like `Track`, `SpotifyAppApi`, etc., in the :app module.
    api("com.adamratzman:spotify-api-kotlin-core:4.1.3")
}