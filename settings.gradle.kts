pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://repo.plasmoverse.com/snapshots")
        maven("https://jitpack.io")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
}

stonecutter {
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions("1.21.1")
        vcsVersion = "1.21.1"
    }
}

rootProject.name = "pv-addon-flashback"
