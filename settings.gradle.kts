// settings.gradle.kts
pluginManagement {
    repositories {
        // This is where Gradle looks for PLUGINS.
        // It's crucial that all necessary repositories are listed here.
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // This is where Gradle looks for LIBRARIES (dependencies).
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "Spowlo"

include (":app")
include(":color")
include(":library")
include(":ffmpeg")
include(":common")