package kgit

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class KGitTest {

    private val DYNAMIC_STRUCTURE = "src/test/resources/dynamic-structure"

    @BeforeEach
    fun createKgitDir() {
        Data.init()
    }

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        Path.of(KGIT_DIR).toFile().deleteRecursively()
        Path.of(DYNAMIC_STRUCTURE).toFile().deleteRecursively()
    }

    @Test
    fun `should write tree`() {
        val treeOid = writeStaticTestStructure()

        val content = Data.getObject(treeOid, TYPE_TREE)
        val lines = content.split("\n")
        assertThat(lines).hasSize(3)

        assertAll {
            assertThat(lines[0]).contains("blob")
            assertThat(lines[0]).contains("cats.txt")
            assertThat(lines[1]).contains("tree")
            assertThat(lines[1]).contains("other")
            assertThat(lines[2]).contains("blob")
            assertThat(lines[2]).contains("dogs.txt")
        }
    }

    @Test
    fun `should get an already saved tree`() {
        val treeOid = writeStaticTestStructure()

        val tree = Base.getTree(treeOid).parseState(basePath = "./")

        assertThat(tree.size).isEqualTo(3)
        assertThat(tree).extracting { it.path }.containsOnly("./cats.txt", "./dogs.txt", "./other/shoes.txt")
        assertThat(tree).extracting { it.oid }.doesNotContain(null)
    }

    @Test
    fun `should write tree, modify files & read back the original tree`() {
        val treeOid = writeDynamicTestStructure()
        modifyCurrentWorkingDirFiles()

        assertFilesChanged()
        Base.readTree(treeOid, basePath = "$DYNAMIC_STRUCTURE/")
        assertFilesRestored()
    }

    //TODO restructure tests to use nested classes
    @Test
    fun `should create a commit`() {
        val oid = Base.commit(
            message = "Test commit",
            directory = "src/test/resources/test-structure") //TODO promote to constant

        val content = Data.getObject(oid, expectedType = TYPE_COMMIT)
        val lines = content.split("\n")
        assertThat(lines).hasSize(3)

        assertAll {
            assertThat(lines[0]).contains("tree")
            assertThat(lines[1]).isEmpty()
            assertThat(lines[2]).isEqualTo("Test commit")
        }
    }

    private fun writeStaticTestStructure(): String {
        val dirToWrite = File("src/test/resources/test-structure")
        assertThat(dirToWrite.exists()).isTrue()
        return Base.writeTree(directory = dirToWrite.absolutePath)
    }

    private fun writeDynamicTestStructure(): String {
        // desired state:
        // ./dynamic-structure
        // ./dynamic-structure/flat.txt
        // ./dynamic-structure/subdir/nested.txt

        val dirToWrite = File(DYNAMIC_STRUCTURE)
        assertThat(dirToWrite.exists()).isFalse()
        require(dirToWrite.mkdir())

        File("$DYNAMIC_STRUCTURE/flat.txt").apply {
            require(createNewFile())
            writeText("orig content")
        }

        require(File("$DYNAMIC_STRUCTURE/subdir").mkdir())

        File("$DYNAMIC_STRUCTURE/subdir/nested.txt").apply {
            require(createNewFile())
            writeText("orig nested content")
        }

        return Base.writeTree(directory = dirToWrite.absolutePath)
    }

    private fun modifyCurrentWorkingDirFiles() {
        File("$DYNAMIC_STRUCTURE/flat.txt").writeText("changed content")
        File("$DYNAMIC_STRUCTURE/subdir/nested.txt").writeText("changed nested content")
        File("$DYNAMIC_STRUCTURE/new_file").apply {
            require(createNewFile())
            writeText("new content")
        }
    }

    private fun assertFilesChanged() {
        assertThat(File("$DYNAMIC_STRUCTURE/flat.txt").readText()).isEqualTo("changed content")
        assertThat(File("$DYNAMIC_STRUCTURE/subdir/nested.txt").readText()).isEqualTo("changed nested content")
    }

    private fun assertFilesRestored() {
        assertThat(File("$DYNAMIC_STRUCTURE/flat.txt").readText()).isEqualTo("orig content")
        assertThat(File("$DYNAMIC_STRUCTURE/subdir/nested.txt").readText()).isEqualTo("orig nested content")
        assertThat(File("$DYNAMIC_STRUCTURE/new_file").exists()).isFalse()
    }
}