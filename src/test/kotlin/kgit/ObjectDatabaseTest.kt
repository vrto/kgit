package kgit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kgit.data.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ObjectDatabaseTest {

    val objectDb = ObjectDatabase()

    @BeforeEach
    @AfterEach
    internal fun setUp() {
        Path.of(KGIT_DIR).toFile().deleteRecursively()
    }

    @Test
    fun `init creates a new directory`() {
        assertFileDoesNotExists(KGIT_DIR)

        objectDb.init()

        assertFileExists(KGIT_DIR)
    }

    @Test
    fun `hashObject stores an object and returns an OID`() {
        assertFileDoesNotExists("$KGIT_DIR/objects")
        objectDb.init()

        val oid = objectDb.hashObject("sample data".toByteArray(), TYPE_BLOB)
        assertThat(oid.value.length).isEqualTo(40)

        assertFileExists("$KGIT_DIR/objects/$oid")
    }

    @Test
    fun `getObject prints an object using the given OID`() {
        objectDb.init()
        val originalContent = "sample object"
        val oid = objectDb.hashObject(originalContent.toByteArray(), TYPE_BLOB)

        val content = objectDb.getObject(oid, expectedType = TYPE_BLOB)

        assertThat(content).isEqualTo(originalContent)
    }

    @Test
    fun `getObject throws an error if the given type doesn't match`() {
        objectDb.init()
        val originalContent = "sample object"
        val oid = objectDb.hashObject(originalContent.toByteArray(), type = "other")

        assertThrows<InvalidTypeException> {
            objectDb.getObject(oid = oid, expectedType = TYPE_BLOB)
        }
    }

    @Test
    fun `setHead stores the OID into HEAD`() {
        objectDb.init()
        val oid = objectDb.hashObject("sample data".toByteArray(), TYPE_BLOB)

        objectDb.setHead(oid)

        assertFileExists(HEAD_DIR)
        assertThat(File(HEAD_DIR).readText().toOid()).isEqualTo(oid)
    }

    private fun assertFileDoesNotExists(path: String) {
        assertThat(Files.exists(Path.of(path))).isFalse()
    }

    private fun assertFileExists(path: String) {
        assertThat(Files.exists(Path.of(path))).isTrue()
    }
}