// UPDATED: Replaced aliases with standard plugin IDs for compatibility.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

val versionName = rootProject.extra["versionName"] as String

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.core:core-ktx:1.13.1")
    
    // This module also has dependencies we are migrating.
    implementation(project(":library"))

    // Commented out the :common dependency for now.
    // implementation(project(":common"))
    
    implementation("commons-io:commons-io:2.11.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("bundledRelease") {
                from(components["bundledRelease"])
                groupId = "com.github.BobbyESP.spotdl_android"
                artifactId = "ffmpeg"
                version = project.version.toString()
            }

            create<MavenPublication>("nonbundledRelease") {
                from(components["nonbundledRelease"])
                groupId = "com.github.BobbyESP.spotdl_android"
                artifactId = "ffmpeg-nonbundled"
                version = project.version.toString()
            }
        }
    }
}