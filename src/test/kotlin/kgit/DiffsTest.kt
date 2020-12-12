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

class DiffsTest : DynamicStructureAware() {

    private val diff = Diff(objectDb)

    @Nested
    inner class Diffing {

        @Test
        fun `no changes should result in an empty diff`() {
            val oid = kgit.writeTree()
            val from = kgit.getComparableTree(oid)
            val to = kgit.getComparableTree(oid)

            val changedPaths = diff.diffTrees(from, to)
            assertThat(changedPaths).isEmpty()
        }

        @Test
        fun `should show diff for one changed file`() {
            val orig = kgit.writeTree()
            modifyOneFile()
            val changed = kgit.writeTree()

            val from = kgit.getComparableTree(orig)
            val to = kgit.getComparableTree(changed)

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
            val orig = kgit.writeTree()
            modifyCurrentWorkingDirFiles()
            val changed = kgit.writeTree()

            val from = kgit.getComparableTree(orig)
            val to = kgit.getComparableTree(changed)

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
            val oid = kgit.writeTree()
            val from = kgit.getComparableTree(oid)
            val to = kgit.getComparableTree(oid)

            val fileChanges = diff.listFileChanges(from, to)
            assertThat(fileChanges).isEmpty()
        }

        @Test
        fun `should list a single changed file`() {
            val orig = kgit.writeTree()
            modifyOneFile()
            val changed = kgit.writeTree()

            val from = kgit.getComparableTree(orig)
            val to = kgit.getComparableTree(changed)

            val fileChanges = diff.listFileChanges(from, to)
            assertThat(fileChanges).containsExactly(FileChange("./flat.txt", MODIFIED))
        }

        @Test
        fun `should identify all change types`() {
            val orig = kgit.writeTree()
            modifyAndDeleteCurrentWorkingDirFiles()
            val changed = kgit.writeTree()

            val from = kgit.getComparableTree(orig)
            val to = kgit.getComparableTree(changed)

            val fileChanges = diff.listFileChanges(from, to)
            assertThat(fileChanges).containsExactly(
                FileChange("./flat.txt", DELETED),
                FileChange("./subdir/nested.txt", MODIFIED),
                FileChange("./new_file", NEW)
            )
        }
    }

}