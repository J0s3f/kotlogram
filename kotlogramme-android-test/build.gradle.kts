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
    implementation(project(":kotlogramme-android"))
    implementation(project(":kotlogramme-kotlin"))
}