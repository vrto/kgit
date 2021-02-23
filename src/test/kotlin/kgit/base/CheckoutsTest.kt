package kgit.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import kgit.DynamicStructureAware
import kgit.assertFilesRestored
import kgit.modifyCurrentWorkingDirFiles
import org.junit.jupiter.api.Test

class CheckoutsTest : DynamicStructureAware() {

    @Test
    fun `should checkout a commit`() {
        val orig = kgit.addAllAndCommit("First commit")

        modifyCurrentWorkingDirFiles()
        val next = kgit.addAllAndCommit("Second commit")
        assertThat(data.getHead().oid).isEqualTo(next)

        kgit.checkout(orig.value)
        assertFilesRestored()
        assertThat(data.getHead().oid).isEqualTo(orig)
    }

    @Test
    fun `should checkout a branch`() {
        val orig = kgit.addAllAndCommit("First commit")
        kgit.createBranch("test-branch", orig)

        modifyCurrentWorkingDirFiles()

        kgit.checkout("test-branch")

        assertFilesRestored()
        assertThat(data.getHead(deref = false).value).isEqualTo("ref: refs/heads/test-branch")
    }
}