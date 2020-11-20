package kgit

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import kgit.base.KGit
import kgit.data.*
import org.junit.jupiter.api.*
import java.io.File
import java.nio.file.Path

class KGitTest {

    private val objectDb = ObjectDatabase()
    private val kgit = KGit(objectDb)

    @BeforeEach
    fun createKgitDir() {
        objectDb.init()
    }

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        Path.of(KGIT_DIR).toFile().deleteRecursively()
        Path.of(DYNAMIC_STRUCTURE).toFile().deleteRecursively()
    }

    @Nested
    inner class Trees {

        @Test
        fun `should write tree`() {
            val treeOid = ensureStaticTestStructure().let {
                kgit.writeTree(directory = it.absolutePath)
            }

            val content = objectDb.getObject(treeOid, TYPE_TREE)
            val lines = content.split("\n")
            assertThat(lines).hasSize(3)

            assertAll {
                assertThat(lines[0]).contains("blob")
                assertThat(lines[0]).contains("cats.txt")
                assertThat(lines[1]).contains("tree")
                assertThat(lines[1]).contains("other")
                assertThat(lines[2]).contains("blob")
                assertThat(lines[2]).contains("dogs.txt")
            }
        }

        @Test
        fun `should get an already saved tree`() {
            val treeOid = ensureStaticTestStructure().let {
                kgit.writeTree(directory = it.absolutePath)
            }

            val tree = kgit.getTree(treeOid).parseState(basePath = "./", kgit::getTree)

            assertThat(tree.size).isEqualTo(3)
            assertThat(tree).extracting { it.path }.containsOnly("./cats.txt", "./dogs.txt", "./other/shoes.txt")
            assertThat(tree).extracting { it.oid }.doesNotContain(null)
        }

        @Test
        fun `should write tree, modify files & read back the original tree`() {
            val treeOid = createDynamicTestStructure().let {
                kgit.writeTree(directory = it.absolutePath)
            }
            modifyCurrentWorkingDirFiles()

            assertFilesChanged()
            kgit.readTree(treeOid, basePath = "$DYNAMIC_STRUCTURE/")
            assertFilesRestored()
        }
    }

    @Nested
    inner class Commits {

        @Test
        fun `should create a first commit`() {
            val oid = kgit.commit(
                message = "Test commit",
                directory = STATIC_STRUCTURE
            )

            val content = objectDb.getObject(oid, expectedType = TYPE_COMMIT)
            val lines = content.split("\n")
            assertThat(lines).hasSize(3)

            assertAll {
                assertThat(lines[0]).contains("tree")
                assertThat(lines[1]).isEmpty()
                assertThat(lines[2]).isEqualTo("Test commit")
            }

            assertThat(File(HEAD_DIR).readText().toOid()).isEqualTo(oid)
        }

        @Test
        fun `should link commits together`() {
            val parent = kgit.commit(
                message = "Test commit 1",
                directory = STATIC_STRUCTURE
            )

            val head = kgit.commit(
                message = "Test commit 2",
                directory = STATIC_STRUCTURE
            )

            val content = objectDb.getObject(head, expectedType = TYPE_COMMIT)
            val lines = content.split("\n")
            assertThat(lines).hasSize(4)

            assertAll {
                assertThat(lines[0]).contains("tree")
                assertThat(lines[1]).isEqualTo("parent $parent")
                assertThat(lines[2]).isEmpty()
                assertThat(lines[3]).isEqualTo("Test commit 2")
            }

            assertThat(File(HEAD_DIR).readText().toOid()).isEqualTo(head)
        }

        @Test
        fun `should get the first commit`() {
            val oid = kgit.commit(
                message = "Test commit",
                directory = STATIC_STRUCTURE
            )

            val commit = kgit.getCommit(oid)
            with (commit) {
                assertThat(treeOid).isNotNull()
                assertThat(parentOid).isNull()
                assertThat(message).isEqualTo("Test commit")
            }
        }

        @Test
        fun `should get a commit with parent`() {
            val parentOid = kgit.commit(
                message = "Parent mesg",
                directory = STATIC_STRUCTURE
            )

            val headOid = kgit.commit(
                message = "Head msg",
                directory = STATIC_STRUCTURE
            )

            val commit = kgit.getCommit(headOid)
            with (commit) {
                assertThat(treeOid).isNotNull()
                assertThat(parentOid).isEqualTo(parentOid)
                assertThat(message).isEqualTo("Head msg")
            }
        }
    }

    @Nested
    inner class Checkout {

        @Test
        fun `should checkout a commit`() {
            val structure = createDynamicTestStructure()
            val orig = kgit.commit("First commit", structure.absolutePath)

            modifyCurrentWorkingDirFiles()
            val next = kgit.commit("Second commit", structure.absolutePath)
            assertThat(objectDb.getHead()).isEqualTo(next)

            kgit.checkout(orig, "${structure.absolutePath}/")
            assertFilesRestored()
            assertThat(objectDb.getHead()).isEqualTo(orig)
        }
    }

    @Nested
    inner class Tags {

        @Test
        fun `should create a tag`() {
            val oid = kgit.commit(
                    message = "Test commit",
                    directory = STATIC_STRUCTURE
            )

            kgit.tag("test-tag", oid)

            val ref = objectDb.getRef("refs/tags/test-tag")
            assertThat(ref).isEqualTo(oid)
        }

        @Test
        fun `should create a tag with slashes`() {
            val oid = kgit.commit(
                    message = "Test commit",
                    directory = STATIC_STRUCTURE
            )

            kgit.tag("nested/tag", oid)

            val ref = objectDb.getRef("refs/tags/nested/tag")
            assertThat(ref).isEqualTo(oid)
        }
    }

    @Nested
    inner class OidResolving {

        private var oid: Oid = Oid("N/A")

        @BeforeEach
        fun prepareTestCommit() {
            oid = kgit.commit(
                    message = "Test commit",
                    directory = STATIC_STRUCTURE
            )
        }

        @Test
        fun `should resolve OID into an OID`() {
            val resolved = kgit.getOid(oid.value)
            assertThat(resolved).isEqualTo(oid)
        }

        @Test
        fun `should resolve ref into an OID`() {
            kgit.tag("tag-to-resolve", oid)

            val resolvedViaRoot = kgit.getOid("refs/tags/tag-to-resolve")
            val resolvedViaRefs = kgit.getOid("tags/tag-to-resolve")
            val resolvedViaTags = kgit.getOid("tag-to-resolve")
//            val resolvedViaHeads = kgit.getOid("tag-to-resolve") TBD

            assertThat(resolvedViaRoot).isEqualTo(oid)
            assertThat(resolvedViaRefs).isEqualTo(oid)
            assertThat(resolvedViaTags).isEqualTo(oid)
        }

        @Test
        fun `should crash if name can't be resolved into any OID`() {
            assertThrows<IllegalArgumentException> {
                kgit.getOid("bogus")
            }
        }
    }
}