package kgit.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kgit.*
import kgit.base.MergeResult.FAST_FORWARDED
import kgit.base.MergeResult.MERGED
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
        val orig = kgit.addAllAndCommit("first commit")

        modifyCurrentWorkingDirFiles()
        kgit.addAllAndCommit("set new HEAD")

        val result = kgit.merge(orig)
        assertThat(result).isEqualTo(MERGED)

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
        val orig = kgit.addAllAndCommit("Original file")
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
        val versionA = kgit.addAllAndCommit("Version A")

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
        kgit.addAllAndCommit("Version B")

        val result = kgit.merge(versionA)
        assertThat(result).isEqualTo(MERGED)

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

    @Test
    fun `should do a fast-forward merge`() {
        // HEAD is the common ancestor of HEAD and some-branch

        //     HEAD
        //     v
        // o---o
        //     \
        //      --o---o
        //            ^
        //            some-branch
        kgit.addAllAndCommit("start")
        val head = kgit.addAllAndCommit("HEAD")

        kgit.createBranch("some-branch", head)
        kgit.checkout("some-branch")

        modifyCurrentWorkingDirFiles()

        kgit.addAllAndCommit("new branch commit 1")
        val someBranch = kgit.addAllAndCommit("some-branch ref")

        // move HEAD back
        kgit.checkout(head.value)
        assertThat(data.getHead().oidValue).isEqualTo(head.value)

        // ffwd merge
        val result = kgit.merge(someBranch)
        assertThat(result).isEqualTo(FAST_FORWARDED)
        assertFilesChanged()

        // HEAD moved to the latest commit
        assertThat(data.getHead().oidValue).isEqualTo(someBranch.value)
        assertThat(data.getMergeHead()).isNull()
    }
}