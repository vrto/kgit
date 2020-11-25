package kgit

import assertk.assertThat
import assertk.assertions.*
import kgit.data.*
import org.junit.jupiter.api.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ObjectDatabaseTest {

    val objectDb = ObjectDatabase(STATIC_STRUCTURE)

    val KGIT_DIR = "$STATIC_STRUCTURE/.kgit"
    val HEAD_DIR = "$KGIT_DIR/HEAD"

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

    @Nested
    inner class Hashing {

        @BeforeEach
        fun initDb() {
            objectDb.init()
        }

        @Test
        fun `hashObject stores an object and returns an OID`() {
            val oid = objectDb.hashObject("sample data".toByteArray(), TYPE_BLOB)
            assertThat(oid.value.length).isEqualTo(40)

            assertFileExists("$KGIT_DIR/objects/$oid")
        }

        @Test
        fun `hashing the same object twice gracefully rewrites the contents`() {
            val oid1 = objectDb.hashObject("sample data".toByteArray(), TYPE_BLOB)
            val oid2 = objectDb.hashObject("sample data".toByteArray(), TYPE_BLOB)

            assertThat(oid1).isEqualTo(oid2)

            assertFileExists("$KGIT_DIR/objects/$oid1")
            assertFileExists("$KGIT_DIR/objects/$oid2")
        }

        @Test
        fun `getObject prints an object using the given OID`() {
            val originalContent = "sample object"
            val oid = objectDb.hashObject(originalContent.toByteArray(), TYPE_BLOB)

            val content = objectDb.getObject(oid, expectedType = TYPE_BLOB)

            assertThat(content).isEqualTo(originalContent)
        }

        @Test
        fun `getObject throws an error if the given type doesn't match`() {
            val originalContent = "sample object"
            val oid = objectDb.hashObject(originalContent.toByteArray(), type = "other")

            assertThrows<InvalidTypeException> {
                objectDb.getObject(oid = oid, expectedType = TYPE_BLOB)
            }
        }
    }

    @Nested
    inner class Heads {

        @BeforeEach
        fun initDb() {
            objectDb.init()
        }

        @Test
        fun `setHead stores the OID into HEAD`() {
            val oid = objectDb.hashObject("sample data".toByteArray(), TYPE_BLOB)

            objectDb.setHead(oid.toDirectRef())

            assertFileExists(HEAD_DIR)
            assertThat(File(HEAD_DIR).readText().toOid()).isEqualTo(oid)
        }

        @Test
        fun `getHead returns null when nothing is in HEAD`() {
            val headOid = objectDb.getHead()
            assertThat(headOid).isNull()
        }

        @Test
        fun `getHead grabs OID in HEAD`() {
            val oid = objectDb.hashObject("sample data".toByteArray(), TYPE_BLOB)

            objectDb.setHead(oid.toDirectRef())

            val headOid = objectDb.getHead()?.oid
            assertThat(headOid).isEqualTo(oid)
        }
    }

    @Nested
    inner class Refs {

        @BeforeEach
        fun initDb() {
            objectDb.init()
        }

        @Test
        fun `should iterate refs`() {
            File("$KGIT_DIR/refs/tags").mkdirs()

            val oid1 = objectDb.hashObject("sample data".toByteArray(), TYPE_BLOB)
            val oid2 = objectDb.hashObject("sample data".toByteArray(), TYPE_BLOB)
            File("$KGIT_DIR/refs/tags/tag1").writeText(oid1.value)
            File("$KGIT_DIR/refs/tags/tag2").writeText(oid2.value)
            objectDb.setHead(oid1.toDirectRef())

            val refs = objectDb.iterateRefs()

            assertThat(refs).containsExactly(
                NamedRef("HEAD", oid1),
                NamedRef("tags/tag2", oid2),
                NamedRef("tags/tag1", oid1))
        }

        @Test
        fun `should dereference symbolic ref`() {
            File("$KGIT_DIR/refs/tags").mkdirs()
            File("$KGIT_DIR/refs/heads").mkdirs()

            val oid = objectDb.hashObject("sample data".toByteArray(), TYPE_BLOB)
            File("$KGIT_DIR/refs/tags/tag1").writeText(oid.value)
            File("$KGIT_DIR/refs/heads/branch1").writeText("ref: ${oid.value}")

            assertThat(objectDb.getRef("refs/tags/tag1")?.oid).isEqualTo(oid)
            assertThat(objectDb.getRef("refs/heads/branch1")?.oid).isEqualTo(oid)
        }
    }
}

private fun assertFileDoesNotExists(path: String) {
    assertThat(Files.exists(Path.of(path))).isFalse()
}

private fun assertFileExists(path: String) {
    assertThat(Files.exists(Path.of(path))).isTrue()
}
