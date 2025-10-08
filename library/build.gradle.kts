import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
}

// REMOVED: This line was causing the build to fail as 'versionName' is not defined in the root project.
// val versionName = rootProject.extra["versionName"] as String

// REMOVED: This logic is also not needed as it depends on local.properties which we are avoiding.
// val localProperties = Properties().apply {
//     load(project.rootDir.resolve("local.properties").inputStream())
// }

android {
    namespace = "com.bobbyesp.library"
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
        // REMOVED: The buildConfigFields are not needed and depend on local.properties.
        // all { ... }
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
        buildConfig = true // Kept, as some internal logic might use it.
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

tasks.register<Jar>("androidBundledSourcesJar") {
    archiveClassifier = "sources"
    from(android.sourceSets.getByName("main").java.srcDirs, android.sourceSets.getByName("bundled").java.srcDirs)
}

tasks.register<Jar>("androidNonbundledSourcesJar") {
    archiveClassifier = "sources"
    from(android.sourceSets.getByName("main").java.srcDirs, android.sourceSets.getByName("nonbundled").java.srcDirs)
}

// REMOVED: The entire publishing block is not relevant for building the Spowlo APK.
// It was for publishing the library as a standalone artifact.
// afterEvaluate { ... }

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    
    // As identified, :common does not exist in Spowlo, so this is removed for good.
    // implementation(project(":common")) 
    
    // Using explicit versions for clarity and stability.
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    implementation("commons-io:commons-io:2.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}