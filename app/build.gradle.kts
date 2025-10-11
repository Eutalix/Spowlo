import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    alias(libs.plugins.compose.compiler)
}
apply(plugin = "dagger.hilt.android.plugin")

// A sealed class to manage app versioning in a structured and type-safe way.
sealed class Version(
    open val versionMajor: Int,
    val versionMinor: Int,
    val versionPatch: Int,
    val versionBuild: Int = 0
) {
    abstract fun toVersionName(): String

    fun toVersionCode(): Int =
        versionMajor * 1000000 + versionMinor * 10000 + versionPatch * 100 + versionBuild

    class Stable(versionMajor: Int, versionMinor: Int, versionPatch: Int) :
        Version(versionMajor, versionMinor, versionPatch) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}"
    }
}

val currentVersion: Version = Version.Stable(
    versionMajor = 1,
    versionMinor = 5,
    versionPatch = 3,
)

val keystorePropertiesFile = rootProject.file("keystore.properties")
val splitApks = !project.hasProperty("noSplits")

android {
    // Configuration for release signing, loaded from keystore.properties if it exists.
    if (keystorePropertiesFile.exists()) {
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
        signingConfigs {
            getByName("debug")
            {
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
                storeFile = file(keystoreProperties["storeFile"]!!)
                storePassword = keystoreProperties["storePassword"].toString()
            }
        }
    }

    compileSdk = 35
    defaultConfig {
        applicationId = "com.bobbyesp.spowlo"
        minSdk = 26
        targetSdk = 35
        versionCode = currentVersion.toVersionCode()

        versionName = currentVersion.toVersionName().run {
            if (!splitApks) "$this-(F-Droid)"
            else this
        }

        // This tells Gradle how to resolve a missing dimension strategy. When a library
        // (like :library or :ffmpeg) has product flavors ('bundled', 'nonbundled'),
        // this tells the app to default to the 'bundled' version.
        missingDimensionStrategy("bundling", "bundled")

        // --- FIX ---
        // Provides values for placeholders required by a dependency's manifest during the
        // manifest merge process. This is typically needed for features like OAuth callbacks
        // which require a custom URL scheme to redirect back to the app.
        manifestPlaceholders += mapOf(
            "redirectSchemeName" to "spowlo-auth",
            "redirectHostName" to "callback"
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
        if (!splitApks)
            ndk {
                (properties["ABI_FILTERS"] as String).split(';').forEach {
                    abiFilters.add(it)
                }
            }
    }

    // Configuration for creating split APKs for different ABIs.
    if (splitApks)
        splits {
            abi {
                isEnable = !project.hasProperty("noSplits")
                reset()
                include("arm64-v8a", "armeabi-v7a")
                isUniversalApk = false
            }
        }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            packaging {
                resources.excludes.add("META-INF/*.kotlin_module")
            }
            if (keystorePropertiesFile.exists())
                signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            if (keystorePropertiesFile.exists())
                signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Spowlo (Debug)")
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        disable.addAll(listOf("MissingTranslation", "ExtraTranslation"))
    }

    // Customizes the output APK file name to include version and build type.
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "Spowlo-${defaultConfig.versionName}-${name}.apk"
        }
    }

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.kotlin_module"
        }
    }
    namespace = "com.bobbyesp.spowlo"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Local module dependencies
    implementation(project(":library"))
    implementation(project(":ffmpeg"))
    implementation(project(":color"))

    // AndroidX & Core libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Accompanist (for supplementary Compose utilities)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.navigation.animation)
    
    // Paging 3 for paginated lists
    implementation(libs.paging.compose)
    implementation(libs.paging.runtime)

    // Coil for image loading
    implementation(libs.coil.kt.compose)

    // Kotlinx Serialization for JSON parsing
    implementation(libs.kotlinx.serialization.json)

    // Hilt for Dependency Injection
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room for local database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Network
    implementation(libs.bundles.ktor)

    // Key-value storage
    implementation(libs.mmkv)

    // Other utilities
    implementation(libs.markdown)
    implementation(libs.customtabs)
    debugImplementation(libs.crash.handler)

    // Testing
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// Helper functions and providers for build logic.
fun String.capitalizeWord(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.getDefault()
        ) else it.toString()
    }
}

class RoomSchemaArgProvider(
    @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE) val schemaDir: File
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        return listOf("room.schemaLocation=${schemaDir.path}")
    }
}