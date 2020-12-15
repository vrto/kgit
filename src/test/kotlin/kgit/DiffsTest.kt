package kgit

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kgit.diff.ChangeAction.*
import kgit.diff.Diff
import kgit.diff.FileChange
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DiffsTest : TreeAwareTest() {

    private val diff = Diff(objectDb)

    @Nested
    inner class Diffing {

        @Test
        fun `no changes should result in an empty diff`() {
            val (from, to) = createTrees()

            val changedPaths = diff.diffTrees(from, to)
            assertThat(changedPaths).isEmpty()
        }

        @Test
        fun `should show diff for one changed file`() {
            val (from, to) = createTrees {
                modifyOneFile()
            }

            val diffLines = diff.diffTrees(from, to)

            assertThat(diffLines.joinToString(separator = "\n")).isEqualTo(
                """
            --- ./flat.txt
            +++ ./flat.txt
            @@ -1 +1 @@
            -orig content
            +changed content
            
            """.trimIndent()
            )
        }

        @Test
        fun `should diff a bunch of changed files`() {
            val (from, to) = createTrees {
                modifyCurrentWorkingDirFiles()
            }

            val diffLines = diff.diffTrees(from, to)

            assertThat(diffLines.joinToString(separator = "\n")).isEqualTo(
                """
            --- ./flat.txt
            +++ ./flat.txt
            @@ -1 +1 @@
            -orig content
            +changed content
            
            --- ./subdir/nested.txt
            +++ ./subdir/nested.txt
            @@ -1 +1 @@
            -orig nested content
            +changed nested content
            
            --- ./new_file
            +++ ./new_file
            @@ -1 +1 @@
            -
            +new content

            """.trimIndent()
            )
        }
    }

    @Nested
    inner class ListingChanges {

        @Test
        fun `no changes should be listed if nothing has changed`() {
            val (from, to) = createTrees()

            val fileChanges = diff.listFileChanges(from, to)
            assertThat(fileChanges).isEmpty()
        }

        @Test
        fun `should list a single changed file`() {
            val (from, to) = createTrees {
                modifyOneFile()
            }

            val fileChanges = diff.listFileChanges(from, to)
            assertThat(fileChanges).containsExactly(FileChange("./flat.txt", MODIFIED))
        }

        @Test
        fun `should identify all change types`() {
            val (from, to) = createTrees {
                modifyAndDeleteCurrentWorkingDirFiles()
            }

            val fileChanges = diff.listFileChanges(from, to)
            assertThat(fileChanges).containsExactly(
                FileChange("./flat.txt", DELETED),
                FileChange("./subdir/nested.txt", MODIFIED),
                FileChange("./new_file", NEW)
            )
        }
    }

}