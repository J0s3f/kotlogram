plugins {
    kotlin("jvm") version "2.1.20" apply false
    kotlin("android") version "2.1.20" apply false
    id("com.android.library") version "9.4.1" apply false
    id("com.android.application") version "9.4.1" apply false
    kotlin("plugin.serialization") version "2.1.20" apply false
    `maven-publish`
}

group = "io.github.j0s3f"
version = providers.gradleProperty("version").orElse("0.1.0-SNAPSHOT").get()

subprojects {
    group = rootProject.group
    version = rootProject.version
}