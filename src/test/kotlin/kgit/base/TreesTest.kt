package kgit.base

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import kgit.*
import kgit.data.TYPE_TREE
import org.junit.jupiter.api.Test

class TreesTest : DynamicStructureAware() {

    @Test
    fun `should write tree`() {
        val treeOid = kgit.writeTree()

        val content = objectDb.getObject(treeOid, TYPE_TREE)
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
        val treeOid = kgit.writeTree()

        val tree = kgit.getTree(treeOid).parseState(basePath = "./", kgit::getTree)

        assertThat(tree.size).isEqualTo(2)
        assertThat(tree).extracting { it.path }.containsOnly("./flat.txt", "./subdir/nested.txt")
        assertThat(tree).extracting { it.oid }.doesNotContain(null)
    }

    @Test
    fun `should write tree, modify files & read back the original tree`() {
        val treeOid = kgit.writeTree()
        modifyCurrentWorkingDirFiles()

        assertFilesChanged()
        kgit.readTree(treeOid, basePath = "$DYNAMIC_STRUCTURE/")
        assertFilesRestored()
    }
}