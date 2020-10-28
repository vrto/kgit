package kgit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path

class LowLevelOpsTest {

    @BeforeEach
    internal fun setUp() {
        Path.of(KGIT_DIR).toFile().deleteRecursively()
    }

    @Test
    fun `init creates a new directory`() {
        assertFileDoesNotExists(KGIT_DIR)

        Data.init()

        assertFileExists(KGIT_DIR)
    }

    @Test
    fun `hashObject stores an object and returns an OID`() {
        assertFileDoesNotExists("$KGIT_DIR/objects")
        Data.init()

        val oid = Data.hashObject("sample data".toByteArray(), TYPE_BLOB)
        assertThat(oid.length).isEqualTo(40)

        assertFileExists("$KGIT_DIR/objects/$oid")
    }

    @Test
    fun `getObject prints an object using the given OID`() {
        Data.init()
        val originalContent = "sample object"
        val oid = Data.hashObject(originalContent.toByteArray(), TYPE_BLOB)

        val content = Data.getObject(oid, expectedType = TYPE_BLOB)

        assertThat(content).isEqualTo(originalContent)
    }

    @Test
    fun `getObject throws an error if the given type doesn't match`() {
        Data.init()
        val originalContent = "sample object"
        val oid = Data.hashObject(originalContent.toByteArray(), type = "other")

        assertThrows<InvalidTypeException> {
            Data.getObject(oid = oid, expectedType = TYPE_BLOB)
        }
    }

    private fun assertFileDoesNotExists(path: String) {
        assertThat(Files.exists(Path.of(path))).isFalse()
    }

    private fun assertFileExists(path: String) {
        assertThat(Files.exists(Path.of(path))).isTrue()
    }
}