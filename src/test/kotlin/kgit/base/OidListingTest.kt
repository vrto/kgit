package kgit.base

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.hasSize
import kgit.DYNAMIC_STRUCTURE
import kgit.DynamicStructureAware
import kgit.data.Oid
import kgit.data.TYPE_BLOB
import kgit.data.TYPE_COMMIT
import kgit.data.TYPE_TREE
import org.junit.jupiter.api.Test
import java.io.File

class OidListingTest : DynamicStructureAware() {

    @Test
    fun `should list all OIDs reachable for ref(s)`() {
        // o
        val first = kgit.addAllAndCommit("First commit")

        // o<----o
        val second = kgit.addAllAndCommit("Second commit")

        // o<----o----o
        val third = kgit.addAllAndCommit("Third commit")

        // o<----o----o----o
        val final = kgit.addAllAndCommit("Final idea")

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
        val alternate1 = kgit.addAllAndCommit("Idea 1")

        // o<----o----o----o
        //       \         ^
        //        <--$---$ refs/tags/final
        val alternate2 = kgit.addAllAndCommit("Idea 1 some more")

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

    @Test
    fun `should iterate objects in commits`() {
        val first = kgit.addAllAndCommit("First commit")

        generateNewFile("second")
        kgit.add("second")
        val second = kgit.commit("Second commit")

        generateNewFile("third")
        kgit.add("third")
        val third = kgit.commit("Third commit")

        val objects = kgit.listObjectsInCommits(listOf(first, second, third))
        assertThat(objects).hasSize(
            3 // commits
            + 4 // trees
            + 4 // files
        )

        assertThat(objects.unwrap(TYPE_COMMIT)).hasSize(3)
        assertThat(objects.unwrap(TYPE_TREE)).hasSize(4)
        assertThat(objects.unwrap(TYPE_BLOB)).containsOnly("orig content", "orig nested content", "second", "third")
    }

    private fun generateNewFile(name: String) {
        File("$DYNAMIC_STRUCTURE/$name").apply {
            require(createNewFile())
            writeText(name)
        }
    }

    private fun List<Oid>.unwrap(type: String): List<String> = mapNotNull { oid ->
        data.tryParseObject(oid, type)
    }
}