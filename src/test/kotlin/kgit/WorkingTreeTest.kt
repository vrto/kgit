package kgit

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.doesNotContain
import assertk.assertions.extracting
import assertk.assertions.hasSize
import kgit.base.Tree.FileState
import org.junit.jupiter.api.Test

class WorkingTreeTest : DynamicStructureAware() {

    @Test
    fun `should get working tree`() {
        val tree = kgit.getWorkingTree()

        assertTreeContainsExactly(tree, "$DYNAMIC_STRUCTURE/flat.txt", "$DYNAMIC_STRUCTURE/subdir/nested.txt")
    }

    @Test
    fun `current working tree should include new files too`() {
        modifyCurrentWorkingDirFiles()

        val tree = kgit.getWorkingTree()

        assertTreeContainsExactly(tree,
            "$DYNAMIC_STRUCTURE/flat.txt", "$DYNAMIC_STRUCTURE/subdir/nested.txt", "$DYNAMIC_STRUCTURE/new_file")
    }
}

private fun assertTreeContainsExactly(tree: List<FileState>, vararg paths: String) {
    assertThat(tree).hasSize(paths.size)
    assertThat(tree).extracting { it.path }.containsOnly(*paths)
    assertThat(tree).extracting { it.oid }.doesNotContain(null)
}