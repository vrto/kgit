package kgit

import assertk.assertThat
import assertk.assertions.containsExactly
import kgit.diff.Diff
import kgit.diff.FileMerge
import org.junit.jupiter.api.Test


class MergingTest : TreeAwareTest() {

    private val diff = Diff(objectDb)

    @Test
    fun `merging two identical contents shouldn't change the content of files`() {
        val (from, to) = createTrees()

        val merged = diff.mergeTrees(from, to)
        assertThat(merged).containsExactly(
            FileMerge(path = "./flat.txt", content = "orig content\n"),
            FileMerge(path = "./subdir/nested.txt", content = "orig nested content\n")
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
                path = "./flat.txt", content = """
                #ifndef HEAD
                orig content
                #else /* HEAD */
                changed content
                #endif /* HEAD */
                
                """.trimIndent()
            ),
            FileMerge(path = "./subdir/nested.txt", content = "orig nested content\n")
        )
    }

    @Test
    fun `should merge two trees with several file changes`() {
        val (from, to) = createTrees {
            modifyCurrentWorkingDirFiles()
        }

        val merged = diff.mergeTrees(from, to)
        assertThat(merged).containsExactly(
            FileMerge(path = "./flat.txt", content = """
                #ifndef HEAD
                orig content
                #else /* HEAD */
                changed content
                #endif /* HEAD */
                
                """.trimIndent()
            ),
            FileMerge(path = "./subdir/nested.txt", content = """
                #ifndef HEAD
                orig nested content
                #else /* HEAD */
                changed nested content
                #endif /* HEAD */
                
                """.trimIndent()
            ),
            FileMerge(path = "./new_file", content = """
                #ifndef HEAD
                
                #else /* HEAD */
                new content
                #endif /* HEAD */
            
            """.trimIndent()
            )
        )
    }

}