import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

val versionName = "1.0.0"

val localProperties = Properties().apply {
    val file = project.rootDir.resolve("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

android {
    namespace = "com.bobbyesp.library"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

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
        all {
            val clientId = localProperties.getProperty("CLIENT_ID") ?: "null"
            val clientSecret = localProperties.getProperty("CLIENT_SECRET") ?: "null"
            
            buildConfigField("String", "CLIENT_ID", "\"$clientId\"")
            buildConfigField("String", "CLIENT_SECRET", "\"$clientSecret\"")
        }
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
    packaging {
        jniLibs.useLegacyPackaging = true 
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android) 

    implementation(libs.commons.io)

    //Serialization
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}