package org.kotlogramme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Drift gate between the Kotlin bridge and the native operation inventory.
 *
 * `native/operations.txt` lists every operation the Rust crate answers. The Rust unit tests in
 * `native/src/ops/tests.rs` assert the same file equals `all_operations()` plus the three
 * JNI-implemented names, so together the two tests fail the build when either side adds, renames
 * or removes an operation without the other.
 */
class OperationParityTest {
    @Test
    fun `bridge annotations match the native operation inventory`() {
        val declared = OperationCatalog.names
        val native = inventoryLines().toSet()

        val missing = declared - native
        val extra = native - declared
        assertTrue(
            missing.isEmpty() && extra.isEmpty(),
            "native/operations.txt is out of date; missing from Kotlin: $missing, " +
                "missing from the native inventory: $extra",
        )
        assertEquals(declared, native, "native/operations.txt must contain exactly the annotated operations")
    }

    @Test
    fun `inventory has no duplicates and is sorted`() {
        val lines = inventoryLines()
        assertEquals(lines.sorted(), lines, "native/operations.txt must be sorted")
        assertEquals(
            lines.distinct(),
            lines,
            "native/operations.txt must not repeat an operation",
        )
    }

    private fun inventoryLines(): List<String> {
        val file = locateInventory()
        return file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** Finds `native/operations.txt` from the repository root or from the module directory. */
    private fun locateInventory(): File {
        var directory: File? = File(System.getProperty("user.dir")).absoluteFile
        while (directory != null) {
            for (candidate in listOf(
                File(directory, INVENTORY_PATH),
                File(directory, "../$INVENTORY_PATH"),
            )) {
                if (candidate.isFile) return candidate.canonicalFile
            }
            directory = directory.parentFile
        }
        throw AssertionError(
            "Could not locate $INVENTORY_PATH starting from ${System.getProperty("user.dir")}",
        )
    }

    private companion object {
        const val INVENTORY_PATH = "native/operations.txt"
    }
}
