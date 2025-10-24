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

// Versioning with build component support (Stable keeps X.Y.Z as versionName)
// versionCode = major*1_000_000 + minor*10_000 + patch*100 + build
sealed class Version(
    open val versionMajor: Int,
    val versionMinor: Int,
    val versionPatch: Int,
    val versionBuild: Int = 0
) {
    abstract fun toVersionName(): String

    fun toVersionCode(): Int =
        versionMajor * 1000000 + versionMinor * 10000 + versionPatch * 100 + versionBuild

    class Beta(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
        Version(versionMajor, versionMinor, versionPatch, versionBuild) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}-beta.$versionBuild"
    }

    // Stable accepts versionBuild but keeps plain X.Y.Z as versionName
    class Stable(
        versionMajor: Int,
        versionMinor: Int,
        versionPatch: Int,
        versionBuild: Int = 0
    ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}"
    }

    class ReleaseCandidate(
        versionMajor: Int,
        versionMinor: Int,
        versionPatch: Int,
        versionBuild: Int
    ) :
        Version(versionMajor, versionMinor, versionPatch, versionBuild) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}-rc.$versionBuild"
    }
}

// Keep 1.5.4 as versionName, bump only versionCode via versionBuild
val currentVersion: Version = Version.Stable(
    versionMajor = 1,
    versionMinor = 5,
    versionPatch = 4,
    versionBuild = 1 // increase when you publish a rebuild without changing X.Y.Z
)

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

val splitApks = !project.hasProperty("noSplits")

android {
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    val localProperties = Properties()
    if (rootProject.file("local.properties").exists()) {
        localProperties.load(FileInputStream(rootProject.file("local.properties")))
    }

    compileSdk = 35

    defaultConfig {
        applicationId = "com.bobbyesp.spowlo"
        minSdk = 26
        targetSdk = 35
        versionCode = currentVersion.toVersionCode()

        versionName = currentVersion.toVersionName().run {
            if (!splitApks) "$this-(F-Droid)" else this
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        // Keep correct dimension resolution when using the library module flavors
        missingDimensionStrategy("bundling", "bundled")

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")

            buildConfigField(
                "String",
                "CLIENT_ID",
                "\"${localProperties.getProperty("CLIENT_ID", "YOUR_CLIENT_ID_PLACEHOLDER")}\""
            )
            buildConfigField(
                "String",
                "CLIENT_SECRET",
                "\"${localProperties.getProperty("CLIENT_SECRET", "YOUR_CLIENT_SECRET_PLACEHOLDER")}\""
            )

            matchingFallbacks.add(0, "debug")
            matchingFallbacks.add(1, "release")
        }
        debug {
            if (keystorePropertiesFile.exists())
                signingConfig = signingConfigs.getByName("debug")

            buildConfigField(
                "String",
                "CLIENT_ID",
                "\"${localProperties.getProperty("CLIENT_ID", "YOUR_CLIENT_ID_PLACEHOLDER")}\""
            )
            buildConfigField(
                "String",
                "CLIENT_SECRET",
                "\"${localProperties.getProperty("CLIENT_SECRET", "YOUR_CLIENT_SECRET_PLACEHOLDER")}\""
            )

            System.setProperty("CLIENT_ID", "\"${localProperties.getProperty("CLIENT_ID")}\"")
            System.setProperty("CLIENT_SECRET", "\"${localProperties.getProperty("CLIENT_SECRET")}\"")

            matchingFallbacks.add(0, "debug")
            matchingFallbacks.add(1, "release")
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

    @Suppress("DEPRECATION")
    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "Spowlo-v${variant.versionName}-${variant.name}.apk"
        }
    }

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }

    // Consolidated packaging rules
    packaging {
        resources {
            // pickFirst to avoid conflicts under META-INF when merging dependencies
            pickFirsts += "META-INF/**"
            excludes += "META-INF/*.kotlin_module"
        }
        jniLibs.useLegacyPackaging = true
    }

    namespace = "com.bobbyesp.spowlo"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":color"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.accompanist.webview)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.material)
    implementation(libs.accompanist.pager.indicators)

    implementation(libs.paging.compose)
    implementation(libs.paging.runtime)
    implementation(libs.coil.kt.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.ext.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(project(":library"))
    implementation(project(":ffmpeg"))
    implementation(project(":common"))

    implementation(libs.spotify.api.android)
    implementation(libs.okhttp)
    implementation(libs.bundles.ktor)
    implementation(libs.mmkv)
    implementation(libs.markdown)
    implementation(libs.customtabs)

    debugImplementation(libs.crash.handler)
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

fun String.capitalizeWord(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

class RoomSchemaArgProvider(
    @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE) val schemaDir: File
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        return listOf("room.schemaLocation=${schemaDir.path}")
    }
}