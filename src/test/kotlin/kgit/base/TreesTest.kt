package kgit.base

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import kgit.DynamicStructureAware
import kgit.assertFilesChanged
import kgit.assertFilesRestored
import kgit.data.TYPE_TREE
import kgit.modifyCurrentWorkingDirFiles
import org.junit.jupiter.api.Test

class TreesTest : DynamicStructureAware() {

    @Test
    fun `should write tree`() {
        kgit.add(".")
        val treeOid = kgit.writeTree()

        val content = data.getObject(treeOid, TYPE_TREE)
        val lines = content.split("\n")
        assertThat(lines).hasSize(2)

        assertAll {
            assertThat(lines[0]).contains("blob")
            assertThat(lines[0]).contains("flat.txt")
            assertThat(lines[1]).contains("tree")
            assertThat(lines[1]).contains("subdir")
        }
    }

    @Test
    fun `should get an already saved tree`() {
        kgit.add(".")
        val treeOid = kgit.writeTree()

        val tree = kgit.getTree(treeOid).parseState(basePath = "./", kgit::getTree)

        assertThat(tree.size).isEqualTo(2)
        assertThat(tree).extracting { it.path }.containsOnly("./flat.txt", "./subdir/nested.txt")
        assertThat(tree).extracting { it.oid }.doesNotContain(null)
    }

    @Test
    fun `should write tree, modify files & read back the original tree`() {
        // fs -> index (tracked)
        kgit.add(".")
        // index -> fs
        val treeOid = kgit.writeTree()

        modifyCurrentWorkingDirFiles()

        assertFilesChanged()
        kgit.readTree(treeOid, updateWorking = true)
        assertFilesRestored()
    }
}