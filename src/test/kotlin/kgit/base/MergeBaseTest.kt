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
        kgit.add(".")
        kgit.commit("first commit")
        val c = kgit.commit("commit C")

        kgit.createBranch("A branch", c)
        kgit.add(".")
        kgit.commit("A - 1")
        kgit.commit("A - 2")
        val a = kgit.commit("Commit A")

        kgit.checkout(c.value)
        kgit.createBranch("B branch", c)
        kgit.add(".")
        kgit.commit("B - 1")
        val b = kgit.commit("Commit B")

        val base = kgit.getMergeBase(a, b)
        assertThat(base).isEqualTo(c)
    }

}