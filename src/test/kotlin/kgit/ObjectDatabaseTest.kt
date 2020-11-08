package kgit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kgit.data.InvalidTypeException
import kgit.data.KGIT_DIR
import kgit.data.ObjectDatabase
import kgit.data.TYPE_BLOB
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path

class ObjectDatabaseTest {

    @BeforeEach
    @AfterEach
    internal fun setUp() {
        Path.of(KGIT_DIR).toFile().deleteRecursively()
    }

    @Test
    fun `init creates a new directory`() {
        assertFileDoesNotExists(KGIT_DIR)

        ObjectDatabase.init()

        assertFileExists(KGIT_DIR)
    }

    @Test
    fun `hashObject stores an object and returns an OID`() {
        assertFileDoesNotExists("$KGIT_DIR/objects")
        ObjectDatabase.init()

        val oid = ObjectDatabase.hashObject("sample data".toByteArray(), TYPE_BLOB)
        assertThat(oid.length).isEqualTo(40)

        assertFileExists("$KGIT_DIR/objects/$oid")
    }

    @Test
    fun `getObject prints an object using the given OID`() {
        ObjectDatabase.init()
        val originalContent = "sample object"
        val oid = ObjectDatabase.hashObject(originalContent.toByteArray(), TYPE_BLOB)

        val content = ObjectDatabase.getObject(oid, expectedType = TYPE_BLOB)

        assertThat(content).isEqualTo(originalContent)
    }

    @Test
    fun `getObject throws an error if the given type doesn't match`() {
        ObjectDatabase.init()
        val originalContent = "sample object"
        val oid = ObjectDatabase.hashObject(originalContent.toByteArray(), type = "other")

        assertThrows<InvalidTypeException> {
            ObjectDatabase.getObject(oid = oid, expectedType = TYPE_BLOB)
        }
    }

    private fun assertFileDoesNotExists(path: String) {
        assertThat(Files.exists(Path.of(path))).isFalse()
    }

    private fun assertFileExists(path: String) {
        assertThat(Files.exists(Path.of(path))).isTrue()
    }
}