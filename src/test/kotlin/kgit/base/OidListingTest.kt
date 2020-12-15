package kgit.base

import assertk.assertThat
import assertk.assertions.containsExactly
import kgit.DynamicStructureAware
import org.junit.jupiter.api.Test

class OidListingTest : DynamicStructureAware() {

    @Test
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
        kgit.checkout(second.value)

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

        val originalPath = kgit.listCommitsAndParents(tagsToOids("refs/tags/final-idea"))
        assertThat(originalPath).containsExactly(final, third, second, first)

        val alternatePath = kgit.listCommitsAndParents(tagsToOids("refs/tags/alternate-idea"))
        assertThat(alternatePath).containsExactly(alternate2, alternate1, second, first)

        val everything = kgit.listCommitsAndParents(tagsToOids("refs/tags/final-idea", "refs/tags/alternate-idea"))
        assertThat(everything).containsExactly(final, third, second, first, alternate2, alternate1)
    }

    private fun tagsToOids(vararg tags: String) = tags.map(kgit::getOid)
}