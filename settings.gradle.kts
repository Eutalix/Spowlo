pluginManagement {
    repositories {
        maven {
            url = uri("app/libs/maven-repo")
        }
        gradlePluginPortal()
        google()
        mavenCentral()
        maven ("https://jitpack.io")
        // NEW: Add Sonatype snapshots repository for alpha/beta library versions
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
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
        maven ("https://jitpack.io")
        // NEW: Add Sonatype snapshots repository for alpha/beta library versions
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
rootProject.name = "Spowlo"

// Includes all migrated modules.
include (":app")
include(":color")
include(":library")
include(":ffmpeg")
include(":common")