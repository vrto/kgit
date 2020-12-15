package kgit.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import kgit.DYNAMIC_STRUCTURE
import kgit.DynamicStructureAware
import kgit.modifyCurrentWorkingDirFiles
import org.junit.jupiter.api.Test
import java.io.File

class MergingTest : DynamicStructureAware() {

    @Test
    fun `should merge HEAD into given commit`() {
        val orig = kgit.commit("first commit")
        modifyCurrentWorkingDirFiles()
        kgit.commit("set new HEAD")

        kgit.merge(orig)

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
}