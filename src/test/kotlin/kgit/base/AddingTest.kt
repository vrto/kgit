package kgit.base

import assertk.assertThat
import assertk.assertions.isTrue
import kgit.DYNAMIC_STRUCTURE
import kgit.DynamicStructureAware
import kgit.data.containsExactKeys
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class AddingTest : DynamicStructureAware() {

    @Nested
    inner class FlatFiles {

        @Test
        fun `adding a non-existent file shouldn't do anything`() {
            kgit.add("bogus")
            assertThat(data.getIndex().isEmpty()).isTrue()
        }

        @Test
        fun `should add one file to the index`() {
            kgit.add("flat.txt")

            val index = data.getIndex()
            assertThat(index).containsExactKeys("flat.txt")
        }

        @Test
        fun `should add several files to the index`() {
            kgit.add("flat.txt")
            kgit.add("subdir/nested.txt")

            val index = data.getIndex()
            assertThat(index).containsExactKeys("flat.txt", "subdir/nested.txt")
        }
    }

    @Nested
    inner class Directories {

        @Test
        fun `adding an empty directory shouldn't do anything`() {
            File("$DYNAMIC_STRUCTURE/emptyDir").mkdir()
            kgit.add("emptyDir")

            val index = data.getIndex()
            assertThat(index.isEmpty()).isTrue()
        }

        @Test
        fun `adding an ignored directory shouldn't do anything`() {
            kgit.add(".kgit")

            val index = data.getIndex()
            assertThat(index.isEmpty()).isTrue()
        }

        @Test
        fun `should add a directory to the index`() {
            File("$DYNAMIC_STRUCTURE/newDir").mkdir()
            File("$DYNAMIC_STRUCTURE/newDir/foo").writeText("Nested file in newDir")
            File("$DYNAMIC_STRUCTURE/newDir/bar").writeText("Another nested file in newDir")

            kgit.add("newDir")

            val index = data.getIndex()
            assertThat(index).containsExactKeys("newDir/foo", "newDir/bar")
        }

        @Test
        fun `should add a directory with another nested directory to the index`() {
            File("$DYNAMIC_STRUCTURE/newDir").mkdir()
            File("$DYNAMIC_STRUCTURE/newDir/foo").writeText("Nested file in newDir")
            File("$DYNAMIC_STRUCTURE/newDir/bar").mkdir()
            File("$DYNAMIC_STRUCTURE/newDir/bar/baz").writeText("Nested file in newDir/bar")

            kgit.add("newDir")

            val index = data.getIndex()
            assertThat(index).containsExactKeys("newDir/foo", "newDir/bar/baz")
        }
    }

}
