package kgit.data

import assertk.assertThat
import assertk.assertions.*
import kgit.DYNAMIC_REMOTE_STRUCTURE
import kgit.STATIC_STRUCTURE
import kgit.createDynamicRemoteTestStructure
import org.junit.jupiter.api.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ObjectDatabaseTest {

    val data = ObjectDatabase(STATIC_STRUCTURE)

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

        data.init()

        assertFileExists(KGIT_DIR)
    }

    @Nested
    inner class Hashing {

        @BeforeEach
        fun initDb() {
            data.init()
        }

        @Test
        fun `hashObject stores an object and returns an OID`() {
            val oid = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            assertThat(oid.value.length).isEqualTo(40)

            assertFileExists("$KGIT_DIR/objects/$oid")
        }

        @Test
        fun `hashing the same object twice gracefully rewrites the contents`() {
            val oid1 = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            val oid2 = data.hashObject("sample data".toByteArray(), TYPE_BLOB)

            assertThat(oid1).isEqualTo(oid2)

            assertFileExists("$KGIT_DIR/objects/$oid1")
            assertFileExists("$KGIT_DIR/objects/$oid2")
        }

        @Test
        fun `getObject prints an object using the given OID`() {
            val originalContent = "sample object"
            val oid = data.hashObject(originalContent.toByteArray(), TYPE_BLOB)

            val content = data.getObject(oid, expectedType = TYPE_BLOB)

            assertThat(content).isEqualTo(originalContent)
        }

        @Test
        fun `getObject throws an error if the given type doesn't match`() {
            val originalContent = "sample object"
            val oid = data.hashObject(originalContent.toByteArray(), type = "other")

            assertThrows<InvalidTypeException> {
                data.getObject(oid = oid, expectedType = TYPE_BLOB)
            }
        }
    }

    @Nested
    inner class Heads {

        @BeforeEach
        fun initDb() {
            data.init()
        }

        @Test
        fun `setHead stores the OID into HEAD`() {
            val oid = data.hashObject("sample data".toByteArray(), TYPE_BLOB)

            data.setHead(oid.toDirectRef())

            assertFileExists(HEAD_DIR)
            assertThat(File(HEAD_DIR).readText().toOid()).isEqualTo(oid)
        }

        @Test
        fun `getHead returns null when nothing is in HEAD`() {
            val headOid = data.getHead().oidOrNull
            assertThat(headOid).isNull()
        }

        @Test
        fun `getHead grabs OID in HEAD`() {
            val oid = data.hashObject("sample data".toByteArray(), TYPE_BLOB)

            data.setHead(oid.toDirectRef())

            val headOid = data.getHead().oid
            assertThat(headOid).isEqualTo(oid)
        }
    }

    @Nested
    inner class Refs {

        @BeforeEach
        fun initDb() {
            data.init()
            File("$KGIT_DIR/refs/tags").mkdirs()
            File("$KGIT_DIR/refs/heads").mkdirs()
        }

        @Test
        fun `should iterate refs`() {
            val oid1 = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            val oid2 = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            File("$KGIT_DIR/refs/tags/tag1").writeText(oid1.value)
            File("$KGIT_DIR/refs/tags/tag2").writeText(oid2.value)
            data.setHead(oid1.toDirectRef())

            val refs = data.iterateRefs()

            assertThat(refs).containsExactly(
                NamedRefValue("HEAD", oid1.toDirectRef()),
                NamedRefValue("tags/tag2", oid2.toDirectRef()),
                NamedRefValue("tags/tag1", oid1.toDirectRef()))
        }

        @Test
        fun `should include MERGE_HEAD in iterate refs result`() {
            val oid1 = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            File("$KGIT_DIR/refs/tags/tag1").writeText(oid1.value)
            data.setHead(oid1.toDirectRef())
            data.updateRef("MERGE_HEAD", oid1.toDirectRef())

            val refs = data.iterateRefs()

            assertThat(refs).containsExactly(
                NamedRefValue("HEAD", oid1.toDirectRef()),
                NamedRefValue("MERGE_HEAD", oid1.toDirectRef()),
                NamedRefValue("tags/tag1", oid1.toDirectRef()))
        }

        @Test
        fun `should dereference refs`() {
            val oid = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            File("$KGIT_DIR/refs/tags/tag1").writeText(oid.value)
            File("$KGIT_DIR/refs/heads/branch1").writeText("ref: ${oid.value}")
            File("$KGIT_DIR/refs/heads/branch2").writeText("ref: refs/heads/branch1")

            // direct ref
            assertThat(data.getRef("refs/tags/tag1").oid).isEqualTo(oid)

            // symbolic ref
            assertThat(data.getRef("refs/heads/branch1").oid).isEqualTo(oid)

            // recursive symbolic ref
            assertThat(data.getRef("refs/heads/branch2").oid).isEqualTo(oid)
        }

        @Test
        fun `should opt out dereferencing`() {
            val oid = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            File("$KGIT_DIR/refs/tags/tag1").writeText(oid.value)
            File("$KGIT_DIR/refs/heads/branch1").writeText("ref: ${oid.value}")

            assertThat(data.getRef("refs/tags/tag1", deref = false))
                .isEqualTo(RefValue(symbolic = false, oid.value))

            assertThat(data.getRef("refs/heads/branch1", deref = false))
                .isEqualTo(RefValue(symbolic = true, "ref: ${oid.value}"))
        }

        @Test
        fun `should write ref`() {
            val oid = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            data.updateRef("refs/tags/tag1", RefValue(value = oid.value))
            assertThat(File("$KGIT_DIR/refs/tags/tag1").readText()).isEqualTo(oid.value)
        }

        @Test
        fun `should write symbolic ref`() {
            val oid = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            data.updateRef("refs/tags/tag1", RefValue(value = oid.value))
            data.updateRef("refs/heads/branch1", RefValue(symbolic = true, value = "refs/tags/tag1"))
            assertThat(File("$KGIT_DIR/refs/heads/branch1").readText()).isEqualTo("ref: refs/tags/tag1")
        }

        @Test
        fun `should delete direct ref`() {
            val oid = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            data.updateRef("refs/tags/tag1", RefValue(value = oid.value))
            assertThat(File("$KGIT_DIR/refs/tags/tag1").readText()).isEqualTo(oid.value)

            data.deleteRef("refs/tags/tag1")

            assertThat(File("$KGIT_DIR/refs/tags/tag1").exists()).isFalse()
        }

        @Test
        fun `should delete symbolic ref`() {
            val oid = data.hashObject("sample data".toByteArray(), TYPE_BLOB)
            data.updateRef("refs/tags/tag1", RefValue(value = oid.value))
            data.updateRef("refs/heads/branch1", RefValue(symbolic = true, value = "refs/tags/tag1"))
            assertThat(File("$KGIT_DIR/refs/heads/branch1").readText()).isEqualTo("ref: refs/tags/tag1")

            data.deleteRef("refs/heads/branch1")

            assertThat(File("$KGIT_DIR/refs/heads/branch1").exists()).isTrue()
            assertThat(File("$KGIT_DIR/refs/tags/tag1").exists()).isFalse()
        }
    }

    @Nested
    inner class RemoteOps {

        val remoteData = ObjectDatabase(DYNAMIC_REMOTE_STRUCTURE)

        @BeforeEach
        fun setUpRemoteObjectDb() {
            Path.of(DYNAMIC_REMOTE_STRUCTURE).toFile().deleteRecursively()
            createDynamicRemoteTestStructure()
            remoteData.init()
        }

        @BeforeEach
        fun initLocalDb() {
            data.init()
        }

        @Test
        fun `fetching an already existing remote object should do nothing`() {
            val oid = data.hashObject("test object".toByteArray(), TYPE_BLOB)
            data.fetchObjectIfMissing(oid, DYNAMIC_REMOTE_STRUCTURE)
            assertThat(data.getObject(oid, TYPE_BLOB)).isEqualTo("test object")
        }

        @Test
        fun `should fetch a remote object into the local object db`() {
            val remoteOid = remoteData.hashObject("remote test object".toByteArray(), TYPE_BLOB)
            data.fetchObjectIfMissing(remoteOid, DYNAMIC_REMOTE_STRUCTURE)
            assertThat(data.getObject(remoteOid, TYPE_BLOB)).isEqualTo("remote test object")
        }

        @Test
        fun `pushing an already existing object to remote should no nothing`() {
            val remoteOid = remoteData.hashObject("remote test object".toByteArray(), TYPE_BLOB)
            data.pushObject(remoteOid, DYNAMIC_REMOTE_STRUCTURE)
            assertThat(remoteData.getObject(remoteOid, TYPE_BLOB)).isEqualTo("remote test object")
        }

        @Test
        fun `should push a local object to the remote object db`() {
            val oid = data.hashObject("test object".toByteArray(), TYPE_BLOB)
            data.pushObject(oid, DYNAMIC_REMOTE_STRUCTURE)
            assertThat(remoteData.getObject(oid, TYPE_BLOB)).isEqualTo("test object")
        }
    }

    @Nested
    inner class IndexOps {

        @BeforeEach
        fun initDb() {
            data.init()
        }

        @Test
        fun `should get index from the filesystem`() {
            val oid = data.hashObject("this goes to index".toByteArray(), TYPE_BLOB)
            File("$KGIT_DIR/index").writeText("""{"foo": "$oid"}""")
            val index = data.getIndex()
            assertThat(index["foo"]).isEqualTo(oid)
        }

        @Test
        fun `should save index to the filesystem`() {
            data.addToIndex(File("$STATIC_STRUCTURE/cats.txt"))

            val indexContent = File("$KGIT_DIR/index").readText()
            assertThat(indexContent).contains("""{"cats.txt": """")
        }
    }
}

private fun assertFileDoesNotExists(path: String) {
    assertThat(Files.exists(Path.of(path))).isFalse()
}

private fun assertFileExists(path: String) {
    assertThat(Files.exists(Path.of(path))).isTrue()
}
