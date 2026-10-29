pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "kotlogramme"
include(":kotlogramme-kotlin")
if (System.getenv("KOTLOGRAMME_INCLUDE_ANDROID") == "1") {
    include(":kotlogramme-android")
    include(":kotlogramme-android-test")
}