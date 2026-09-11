package org.kotlogramme

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeLibraryLoaderTest {
    @Test
    fun `selects bundled ARM64 resources for each supported operating system`() {
        assertEquals(
            NativePlatform("linux-aarch64", "libkotlogramme.so"),
            nativePlatformFor("Linux", "aarch64"),
        )
        assertEquals(
            NativePlatform("windows-aarch64", "kotlogramme.dll"),
            nativePlatformFor("Windows 11", "arm64"),
        )
        assertEquals(
            NativePlatform("macos-aarch64", "libkotlogramme.dylib"),
            nativePlatformFor("Mac OS X", "arm64"),
        )
    }

    @Test
    fun `retains x86 64 resource selection`() {
        assertEquals(
            NativePlatform("linux-x86_64", "libkotlogramme.so"),
            nativePlatformFor("Linux", "amd64"),
        )
        assertEquals(
            NativePlatform("windows-x86_64", "kotlogramme.dll"),
            nativePlatformFor("Windows", "x86_64"),
        )
    }
}
