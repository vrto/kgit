package kgit.base

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kgit.DynamicStructureAware
import org.junit.jupiter.api.Test

class AncestryTest : DynamicStructureAware() {

    @Test
    fun `should recognize ancestors`() {
        kgit.add(".")
        val first = kgit.commit("first")
        kgit.createBranch("master", first)
        kgit.checkout("master")

        val second = kgit.commit("second on master")

        kgit.createBranch("feature", first)
        kgit.checkout("feature")
        kgit.add(".")
        val feature = kgit.commit("feature commit")

        assertThat(kgit.isAncestor(commit = second, maybeAncestor = first)).isTrue()
        assertThat(kgit.isAncestor(commit = feature, maybeAncestor = first)).isTrue()
        assertThat(kgit.isAncestor(commit = feature, maybeAncestor = second)).isFalse()
    }
}