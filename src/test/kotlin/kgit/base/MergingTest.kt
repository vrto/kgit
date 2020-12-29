package kgit.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kgit.DYNAMIC_STRUCTURE
import kgit.DynamicStructureAware
import kgit.modifyCurrentWorkingDirFiles
import kgit.modifyOneSourceFile
import org.junit.jupiter.api.Test
import java.io.File

class MergingTest : DynamicStructureAware() {

    @Test
    fun `should merge HEAD into given commit and set MERGE_HEAD`() {
        mergeCommitWithHead()

        assertThat(File("$DYNAMIC_STRUCTURE/flat.txt").readText()).isEqualTo("""
            changed content

        """.trimIndent())

        assertThat(File("$DYNAMIC_STRUCTURE/subdir/nested.txt").readText()).isEqualTo("""
            changed nested content

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

    @Test
    fun `should auto-merge two versions of the same file`() {
        //     Version A
        //     v
        // o---o
        //  \
        //   --o
        //     ^
        //     Version B

        modifyOneSourceFile("""
            fun be_a_cat(): 
                "Meow" 
                return true
            
            fun be_a_dog(): 
                "Bark" 
                return false
        """.trimIndent())
        val orig = kgit.commit("Original file")
        kgit.createBranch("a", orig)
        kgit.checkout("a")

        modifyOneSourceFile("""
            fun be_a_cat(): 
                "Sleep" 
                return true
            
            fun be_a_dog(): 
                "Bark" 
                return false
        """.trimIndent())
        val versionA = kgit.commit("Version A")

        kgit.createBranch("b", orig)
        kgit.checkout("b")

        modifyOneSourceFile("""
            fun be_a_cat(): 
                "Meow" 
                return true
            
            fun be_a_dog(): 
                "Eat homework" 
                return false
        """.trimIndent())
        kgit.commit("Version B")

        kgit.merge(versionA)

        assertThat(File("$DYNAMIC_STRUCTURE/source.py").readText()).isEqualTo("""
            fun be_a_cat(): 
                "Sleep" 
                return true
            
            fun be_a_dog(): 
                "Eat homework" 
                return false

        """.trimIndent())

        assertThat(data.getMergeHead()!!.value).isEqualTo(versionA.value)
    }
}