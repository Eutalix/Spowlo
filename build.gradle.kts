@file:Suppress("UnstableApiUsage")

buildscript {
    repositories {
        maven {
            url = uri("libs/maven-repo")
        }
        mavenCentral()
        google()
    }
}

plugins {
    // Standard plugins for an Android application, applied to sub-projects.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.gradlePlugin) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    
    // THE FIX: This correctly applies the Compose Compiler plugin at the root level,
    // making it available to all modules in the project.
    alias(libs.plugins.compose.compiler) apply false
}

// Defines a custom 'clean' task to delete the entire build directory.
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}