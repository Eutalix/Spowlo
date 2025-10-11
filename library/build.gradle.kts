plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
}

// --- THE FIX ---
// Specifies that all Kotlin compilation tasks in this module should use JDK 17.
kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.bobbyesp.library"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"${findProperty("CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"${findProperty("CLIENT_SECRET") ?: ""}\"")
    }

    flavorDimensions.add("bundling")
    productFlavors {
        create("bundled") {
            dimension = "bundling"
        }
        create("nonbundled") {
            dimension = "bundling"
        }
    }

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
    
    buildFeatures {
        buildConfig = true
    }

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
    implementation(project(":common"))
    
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    implementation("commons-io:commons-io:2.11.0")

    api("com.adamratzman:spotify-api-kotlin-core:4.1.3")
}