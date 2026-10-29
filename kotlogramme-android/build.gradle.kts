plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.j0s3f.kotlogramme"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDir("src/main/jniLibs")
        }
    }
}

dependencies {
    implementation(project(":kotlogramme-kotlin"))
}

// Build the Rust native library for all Android ABIs when ANDROID_NDK_HOME is set.
val ndkHome = System.getenv("ANDROID_NDK_HOME")
    ?: System.getenv("ANDROID_NDK_ROOT")
val cargoTargets = listOf(
    "aarch64-linux-android" to "arm64-v8a",
    "armv7-linux-androideabi" to "armeabi-v7a",
    "i686-linux-android" to "x86",
    "x86_64-linux-android" to "x86_64",
)
if (ndkHome != null) {
    val hostTag = when {
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> "windows-x86_64"
        System.getProperty("os.name").startsWith("Mac", ignoreCase = true) -> "darwin-x86_64"
        else -> "linux-x86_64"
    }
    val clangBin = "$ndkHome/toolchains/llvm/prebuilt/$hostTag/bin"
    cargoTargets.forEach { (target, abi) ->
        val taskName = "buildNative" + abi.replace("-", "").replace("_", "").replaceFirstChar { it.uppercase() }
        tasks.register<Exec>(taskName) {
            workingDir = file("../native")
            environment("ANDROID_NDK_HOME", ndkHome)
            val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
            val exe = if (isWindows) ".cmd" else ""
            val linker = when (target) {
                "aarch64-linux-android" -> "aarch64-linux-android21-clang$exe"
                "armv7-linux-androideabi" -> "armv7a-linux-androideabi21-clang$exe"
                "i686-linux-android" -> "i686-linux-android21-clang$exe"
                "x86_64-linux-android" -> "x86_64-linux-android21-clang$exe"
                else -> error("unknown target")
            }
            environment("CC", "$clangBin/$linker")
            environment("AR", "$clangBin/llvm-ar" + if (isWindows) ".exe" else "")
            environment("CARGO_TARGET_${target.replace("-", "_").uppercase()}_LINKER", "$clangBin/$linker")
            commandLine("cargo", "build", "--profile", "release-shipped", "--target", target)
            doLast {
                copy {
                    from("../native/target/$target/release-shipped/libkotlogramme.so")
                    into("src/main/jniLibs/$abi")
                }
            }
        }
        tasks.named("preBuild") { dependsOn(taskName) }
    }
}