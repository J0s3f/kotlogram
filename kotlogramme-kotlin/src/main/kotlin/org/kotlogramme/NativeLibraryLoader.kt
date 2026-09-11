package org.kotlogramme

import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale

internal object NativeLibraryLoader {
    private const val libraryBaseName = "kotlogramme"

    @Volatile
    private var loaded = false

    @Synchronized
    fun ensureLoaded() {
        if (loaded) return

        val explicitPath = System.getProperty("kotlogramme.native.path")
        if (!explicitPath.isNullOrBlank()) {
            System.load(Path.of(explicitPath).toAbsolutePath().toString())
            loaded = true
            return
        }

        val platform = nativePlatformFor(
            System.getProperty("os.name"),
            System.getProperty("os.arch"),
        )
        val resourceName = "native/${platform.resourceDirectory}/${platform.libraryFileName}"
        val resource = NativeLibraryLoader::class.java.classLoader.getResourceAsStream(resourceName)
            ?: run {
                // Useful for development builds where the native library is installed globally.
                System.loadLibrary(libraryBaseName)
                loaded = true
                return
            }

        resource.use { input ->
            val tempDirectory = Files.createTempDirectory("kotlogramme-native-")
            val extracted = tempDirectory.resolve(platform.libraryFileName)
            BufferedInputStream(input).use { buffered ->
                Files.copy(buffered, extracted, StandardCopyOption.REPLACE_EXISTING)
            }
            extracted.toFile().deleteOnExit()
            tempDirectory.toFile().deleteOnExit()
            System.load(extracted.toAbsolutePath().toString())
        }
        loaded = true
    }

}

internal data class NativePlatform(
    val resourceDirectory: String,
    val libraryFileName: String,
)

/** Maps JVM system properties to a bundled native-library resource. */
internal fun nativePlatformFor(osName: String, osArch: String): NativePlatform {
    val os = osName.lowercase(Locale.ROOT)
    val arch = osArch.lowercase(Locale.ROOT)
    val normalizedArch = when (arch) {
        "amd64", "x86_64" -> "x86_64"
        "aarch64", "arm64" -> "aarch64"
        else -> error("Unsupported CPU architecture: $arch")
    }

    return when {
        os.contains("win") -> NativePlatform("windows-$normalizedArch", "kotlogramme.dll")
        os.contains("mac") || os.contains("darwin") -> NativePlatform("macos-$normalizedArch", "libkotlogramme.dylib")
        os.contains("linux") -> NativePlatform("linux-$normalizedArch", "libkotlogramme.so")
        else -> error("Unsupported operating system/architecture: $os/$arch")
    }
}
