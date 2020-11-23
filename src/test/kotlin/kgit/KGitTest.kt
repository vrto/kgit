package kgit

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import kgit.base.KGit
import kgit.data.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Path

class KGitTest {

    private val objectDb = ObjectDatabase(DYNAMIC_STRUCTURE)
    private val kgit = KGit(objectDb)

    @BeforeEach
    fun createKgitDir() {
        Path.of(DYNAMIC_STRUCTURE).toFile().deleteRecursively()
        createDynamicTestStructure()
        objectDb.init()
    }

    @Nested
    inner class Trees {

        @Test
        fun `should write tree`() {
            val treeOid = kgit.writeTree()

            val content = objectDb.getObject(treeOid, TYPE_TREE)
            val lines = content.split("\n")
            assertThat(lines).hasSize(2)

            assertAll {
                assertThat(lines[0]).contains("blob")
                assertThat(lines[0]).contains("flat.txt")
                assertThat(lines[1]).contains("tree")
                assertThat(lines[1]).contains("subdir")
            }
        }

        @Test
        fun `should get an already saved tree`() {
            val treeOid = kgit.writeTree()

            val tree = kgit.getTree(treeOid).parseState(basePath = "./", kgit::getTree)

            assertThat(tree.size).isEqualTo(2)
            assertThat(tree).extracting { it.path }.containsOnly("./flat.txt", "./subdir/nested.txt")
            assertThat(tree).extracting { it.oid }.doesNotContain(null)
        }

        @Test
        fun `should write tree, modify files & read back the original tree`() {
            val treeOid = kgit.writeTree()
            modifyCurrentWorkingDirFiles()

            assertFilesChanged()
            kgit.readTree(treeOid, basePath = "$DYNAMIC_STRUCTURE/")
            assertFilesRestored()
        }
    }

    @Nested
    inner class Commits {

        private val HEAD_DIR = "$DYNAMIC_STRUCTURE/.kgit/HEAD"

        @Test
        fun `should create a first commit`() {
            val oid = kgit.commit("Test commit")

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
            val parent = kgit.commit("Test commit 1")

            val head = kgit.commit("Test commit 2")

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
            val oid = kgit.commit("Test commit")

            val commit = kgit.getCommit(oid)
            with (commit) {
                assertThat(treeOid).isNotNull()
                assertThat(parentOid).isNull()
                assertThat(message).isEqualTo("Test commit")
            }
        }

        @Test
        fun `should get a commit with parent`() {
            val parentOid = kgit.commit("Parent msg")
            val headOid = kgit.commit("Head msg")
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
            val orig = kgit.commit("First commit")

            modifyCurrentWorkingDirFiles()
            val next = kgit.commit("Second commit")
            assertThat(objectDb.getHead()).isEqualTo(next)

            kgit.checkout(orig, "TODO/")
            assertFilesRestored()
            assertThat(objectDb.getHead()).isEqualTo(orig)
        }
    }

    @Nested
    inner class Tags {

        @Test
        fun `should create a tag`() {
            val oid = kgit.commit(
                message = "Test commit"
            )

            kgit.tag("test-tag", oid)

            val ref = objectDb.getRef("refs/tags/test-tag")
            assertThat(ref).isEqualTo(oid)
        }

        @Test
        fun `should create a tag with slashes`() {
            val oid = kgit.commit(
                message = "Test commit"
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
                message = "Test commit"
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

        @Test
        fun `should translate '@' to HEAD`() {
            assertThat(kgit.getOid("@")).isEqualTo(objectDb.getHead())
        }
    }

    @Nested
    inner class OidListing {

        //        @Test
        fun `should list all OIDs reachable for ref(s)`() {
            // o
            val first = kgit.commit("First commit")

            // o<----o
            val second = kgit.commit("Second commit")

            // o<----o----o
            val third = kgit.commit("Third commit")

            // o<----o----o----o
            val final = kgit.commit("Final idea")

            // o<----o----o----@
            //                 ^
            //                 refs/tags/final
            kgit.tag("final-idea", final)

            // o<----@----o----o
            //                 ^
            //                 refs/tags/final
            kgit.checkout(second)

            // o<----o----o----o
            //       \         ^
            //        <--$     refs/tags/final
            val alternate1 = kgit.commit("Idea 1")

            // o<----o----o----o
            //       \         ^
            //        <--$---$ refs/tags/final
            val alternate2 = kgit.commit("Idea 1 some more")

            // o<----o----o----o
            //       \         ^
            //        <--$---$ refs/tags/final
            //               ^
            //               refs/tags/alternate
            kgit.tag("alternate-idea", alternate2)

            val originalPath = kgit.listCommitsAndParents(tagsToOids("refs/tags/final"))
            assertThat(originalPath).containsExactly(final, third, second, first)

            val alternatePath = kgit.listCommitsAndParents(tagsToOids("refs/tags/alternate"))
            assertThat(alternatePath).containsExactly(alternate2, alternate1, second, first)

            val everything = kgit.listCommitsAndParents(tagsToOids("refs/tags/final", "refs/tags/alternate"))
            assertThat(everything).containsExactly(final, third, second, first, alternate1, alternate2)
        }

        private fun tagsToOids(vararg tags: String) = tags.map(kgit::getOid)
    }
}