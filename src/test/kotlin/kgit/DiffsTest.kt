package kgit

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test

class DiffsTest : DynamicStructureAware() {

    @Test
    fun `no changes should result in an empty diff`() {
        val oid = kgit.writeTree()
        val from = kgit.getTree(oid)
        val to = kgit.getTree(oid)

        val changedPaths = kgit.diffTrees(from, to)
        assertThat(changedPaths).isEmpty()
    }

    @Test
    fun `should list a changed path`() {
        val orig = kgit.writeTree()
        modifyOneFile()
        val changed = kgit.writeTree()

        val from = kgit.getTree(orig)
        val to = kgit.getTree(changed)

        val changedPaths = kgit.diffTrees(from, to)

        assertThat(changedPaths).containsOnly("./flat.txt")
    }

    @Test
    fun `should list changed paths`() {
        val orig = kgit.writeTree()
        modifyCurrentWorkingDirFiles()
        val changed = kgit.writeTree()

        val from = kgit.getTree(orig)
        val to = kgit.getTree(changed)

        val changedPaths = kgit.diffTrees(from, to)

        assertThat(changedPaths).containsOnly(
            "./flat.txt",
            "./subdir/nested.txt",
            "./new_file")
    }
}