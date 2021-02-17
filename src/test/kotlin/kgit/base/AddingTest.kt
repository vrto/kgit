package kgit.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kgit.DynamicStructureAware
import org.junit.jupiter.api.Test

class AddingTest : DynamicStructureAware() {

    @Test
    fun `adding a non-existent file shouldn't do anything`() {
        kgit.add("bogus")
        assertThat(data.getIndex().isEmpty()).isTrue()
    }

    @Test
    fun `should add one file to the index`() {
        kgit.add("flat.txt")

        val index = data.getIndex()
        assertThat(index.getSize()).isEqualTo(1)
        assertThat(index["flat.txt"]).isNotNull()
    }

    @Test
    fun `should add several files to the index`() {
        kgit.add("flat.txt")
        kgit.add("subdir/nested.txt")

        val index = data.getIndex()
        assertThat(index.getSize()).isEqualTo(2)
        assertThat(index["flat.txt"]).isNotNull()
        assertThat(index["subdir/nested.txt"]).isNotNull()
    }
}