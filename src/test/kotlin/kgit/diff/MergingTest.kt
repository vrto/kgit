package kgit.diff

import assertk.assertThat
import assertk.assertions.containsExactly
import kgit.DYNAMIC_STRUCTURE
import kgit.modifyCurrentWorkingDirFiles
import kgit.modifyOneFile
import org.junit.jupiter.api.Test


class MergingTest : TreeAwareTest() {

    private val diff = Diff(data)

    @Test
    fun `merging two identical contents shouldn't change the content of files`() {
        val (from, to) = createTrees()

        val merged = diff.mergeTrees(from, to)
        assertThat(merged).containsExactly(
            FileMerge(path = "$DYNAMIC_STRUCTURE/flat.txt", content = "orig content\n"),
            FileMerge(path = "$DYNAMIC_STRUCTURE/subdir/nested.txt", content = "orig nested content\n")
        )
    }

    @Test
    fun `should merge two trees with one file change`() {
        val (from, to) = createTrees {
            modifyOneFile()
        }

        val merged = diff.mergeTrees(from, to)
        assertThat(merged).containsExactly(
            FileMerge(
                path = "$DYNAMIC_STRUCTURE/flat.txt", content = """
                #ifndef HEAD
                orig content
                #else /* HEAD */
                changed content
                #endif /* HEAD */
                
                """.trimIndent()
            ),
            FileMerge(path = "$DYNAMIC_STRUCTURE/subdir/nested.txt", content = "orig nested content\n")
        )
    }

    @Test
    fun `should merge two trees with several file changes`() {
        val (from, to) = createTrees {
            modifyCurrentWorkingDirFiles()
        }

        val merged = diff.mergeTrees(from, to)
        assertThat(merged).containsExactly(
            FileMerge(path = "$DYNAMIC_STRUCTURE/flat.txt", content = """
                #ifndef HEAD
                orig content
                #else /* HEAD */
                changed content
                #endif /* HEAD */
                
                """.trimIndent()
            ),
            FileMerge(path = "$DYNAMIC_STRUCTURE/subdir/nested.txt", content = """
                #ifndef HEAD
                orig nested content
                #else /* HEAD */
                changed nested content
                #endif /* HEAD */
                
                """.trimIndent()
            ),
            FileMerge(path = "$DYNAMIC_STRUCTURE/new_file", content = """
                #ifndef HEAD
                
                #else /* HEAD */
                new content
                #endif /* HEAD */
            
            """.trimIndent()
            )
        )
    }

}