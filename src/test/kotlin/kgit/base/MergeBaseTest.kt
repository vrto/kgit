package kgit.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import kgit.DynamicStructureAware
import org.junit.jupiter.api.Test

class MergeBaseTest : DynamicStructureAware() {

    @Test
    fun `the only commit is its own closest ancestor`() {
        val first = kgit.commit("First commit")
        val base = kgit.getMergeBase(first, first)
        assertThat(base).isEqualTo(first)
    }

    @Test
    fun `should compute common ancestor of two commits`() {
        //       commit C    commit A
        //      v           v
        //  o---o---o---o---o
        //       \
        //        --o---o
        //              ^
        //              commit B
        kgit.addAllAndCommit("first commit")

        val c = kgit.addAllAndCommit("commit C")

        kgit.createBranch("A branch", c)
        kgit.addAllAndCommit("A - 1")
        kgit.addAllAndCommit("A - 2")

        val a = kgit.addAllAndCommit("Commit A")

        kgit.checkout(c.value)
        kgit.createBranch("B branch", c)
        kgit.addAllAndCommit("B - 1")

        val b = kgit.addAllAndCommit("Commit B")

        val base = kgit.getMergeBase(a, b)
        assertThat(base).isEqualTo(c)
    }

}