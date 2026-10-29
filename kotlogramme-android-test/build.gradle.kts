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
    implementation("com.github.J0s3f.kotlogram:kotlogram-android:v0.9.15")
}