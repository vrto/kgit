package kgit

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import kgit.data.TYPE_COMMIT
import kgit.data.toOid
import org.junit.jupiter.api.Test
import java.io.File

private val HEAD_DIR = "$DYNAMIC_STRUCTURE/.kgit/HEAD"

class CommitsTest : DynamicStructureAware() {

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