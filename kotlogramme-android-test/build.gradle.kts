plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.j0s3f.kotlogramme.test"
    compileSdk = 35
    defaultConfig {
        applicationId = "io.github.j0s3f.kotlogramme.test"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Resolve the Android artifact from JitPack, exactly as a consumer would.
    // The AAR carries the four native libraries; the jar carries the Kotlin/JVM classes.
    implementation("com.github.J0s3f.kotlogram:kotlogram-android:v0.9.14") {
        exclude(group = "com.github.J0s3f.kotlogram", module = "kotlogram")
    }
    implementation("com.github.J0s3f.kotlogram:kotlogramme:v0.9.14")
}