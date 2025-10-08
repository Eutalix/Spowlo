import java.util.Properties

// UPDATED: Replaced aliases with standard plugin IDs for compatibility.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
}

val versionName = rootProject.extra["versionName"] as String

val localProperties = Properties().apply {
    load(project.rootDir.resolve("local.properties").inputStream())
}

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
        all {
            // Note: Even though we removed the requirement from the :app module,
            // this module still tries to read them. It's safe to leave as long
            // as a placeholder local.properties exists during CI build.
            buildConfigField(
                "String", "CLIENT_ID", "\"${localProperties.getProperty("CLIENT_ID")}\""
            )
            buildConfigField(
                "String", "CLIENT_SECRET", "\"${localProperties.getProperty("CLIENT_SECRET")}\""
            )
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

afterEvaluate{
    publishing {
        publications {
            create<MavenPublication>("bundledRelease") {
                from(components["bundledRelease"])
                groupId = "com.github.BobbyESP.spotdl_android"
                artifactId = "library"
                version = project.version.toString()
            }

            create<MavenPublication>("nonbundledRelease") {
                from(components["nonbundledRelease"])
                groupId = "com.github.BobbyESP.spotdl_android"
                artifactId = "library-nonbundled"
                version = project.version.toString()
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    
    // IMPORTANT: This :common module might be the next missing piece.
    // If the build fails again, we may need to migrate a 'common' module as well.
    // For now, we will comment it out as it likely doesn't exist in the Spowlo project.
    // implementation(project(":common")) 
    
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    implementation("commons-io:commons-io:2.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}