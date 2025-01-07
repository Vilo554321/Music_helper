pluginManagement {
    repositories {
        google() // Dôležité pre Android Gradle Plugin a iné Android závislosti
        mavenCentral() // Dôležité pre ďalšie knižnice
        gradlePluginPortal() // Pre Gradle pluginy
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Zmena na PREFER_SETTINGS pre flexibilitu
    repositories {
        maven { url = uri("https://jitpack.io") }
        google() // Pre Android závislosti
        mavenCentral() // Pre NanoHTTPD a iné knižnice
    }
}

rootProject.name = "Music helper"
include(":app")
