pluginManagement {
    repositories {
        maven {
            url = uri("app/libs/maven-repo")
        }
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("app/libs/maven-repo")
        }
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "Spowlo"
include(":app")
// Included local modules for mono-repo structure
include(":color")
include(":common")
include(":ffmpeg")
include(":library")