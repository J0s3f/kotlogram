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
    }
}

rootProject.name = "kotlogramme"
include(":kotlogramme-kotlin")
if (System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null) {
    include(":kotlogramme-android")
    include(":kotlogramme-android-test")
}