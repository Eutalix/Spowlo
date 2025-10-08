plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // maven-publish can be removed if you don't plan to publish this module standalone.
    id("maven-publish")
}

// NEW: Aligns the Kotlin toolchain with the main app module.
kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.bobbyesp.ffmpeg"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    // UPDATED: Standardized Java and Kotlin target versions for compatibility.
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
    
    // This block is for publishing and can be removed if not needed.
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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.core:core-ktx:1.13.1")
    
    // The modules being migrated.
    implementation(project(":library"))

    // UPDATED: Correctly declares the dependency on the now-migrated :common module.
    implementation(project(":common"))
    
    implementation("commons-io:commons-io:2.11.0")
}