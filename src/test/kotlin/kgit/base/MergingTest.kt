package kgit.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kgit.DYNAMIC_STRUCTURE
import kgit.DynamicStructureAware
import kgit.modifyCurrentWorkingDirFiles
import org.junit.jupiter.api.Test
import java.io.File

class MergingTest : DynamicStructureAware() {

    @Test
    fun `should merge HEAD into given commit and set MERGE_HEAD`() {
        mergeCommitWithHead()

        assertThat(File("$DYNAMIC_STRUCTURE/flat.txt").readText()).isEqualTo("""
            #ifndef HEAD
            changed content
            #else /* HEAD */
            orig content
            #endif /* HEAD */

        """.trimIndent())

        assertThat(File("$DYNAMIC_STRUCTURE/subdir/nested.txt").readText()).isEqualTo("""
            #ifndef HEAD
            changed nested content
            #else /* HEAD */
            orig nested content
            #endif /* HEAD */

        """.trimIndent())
    }

    @Test
    fun `merge commit should wipe MERGE_HEAD`() {
        mergeCommitWithHead()

        assertThat(data.getMergeHead()).isNotNull()
        kgit.commit("Commit merge changes")
        assertThat(data.getMergeHead()).isNull()
    }

    private fun mergeCommitWithHead() {
        val orig = kgit.commit("first commit")
        modifyCurrentWorkingDirFiles()
        kgit.commit("set new HEAD")

        kgit.merge(orig)

        assertThat(data.getMergeHead()!!.value).isEqualTo(orig.value)
    }
}