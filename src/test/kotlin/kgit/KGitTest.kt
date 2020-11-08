package kgit

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import kgit.base.KGit
import kgit.data.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class KGitTest {

    private val DYNAMIC_STRUCTURE = "src/test/resources/dynamic-structure"
    private val STATIC_STRUCTURE = "src/test/resources/test-structure"

    private val objectDb = ObjectDatabase()
    private val kgit = KGit(objectDb)

    @BeforeEach
    fun createKgitDir() {
        objectDb.init()
    }

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        Path.of(KGIT_DIR).toFile().deleteRecursively()
        Path.of(DYNAMIC_STRUCTURE).toFile().deleteRecursively()
    }

    @Nested
    inner class Trees {

        @Test
        fun `should write tree`() {
            val treeOid = writeStaticTestStructure()

            val content = objectDb.getObject(treeOid, TYPE_TREE)
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

            val tree = kgit.getTree(treeOid).parseState(basePath = "./", kgit::getTree)

            assertThat(tree.size).isEqualTo(3)
            assertThat(tree).extracting { it.path }.containsOnly("./cats.txt", "./dogs.txt", "./other/shoes.txt")
            assertThat(tree).extracting { it.oid }.doesNotContain(null)
        }

        @Test
        fun `should write tree, modify files & read back the original tree`() {
            val treeOid = writeDynamicTestStructure()
            modifyCurrentWorkingDirFiles()

            assertFilesChanged()
            kgit.readTree(treeOid, basePath = "$DYNAMIC_STRUCTURE/")
            assertFilesRestored()
        }

        private fun writeStaticTestStructure(): Oid {
            val dirToWrite = File(STATIC_STRUCTURE)
            assertThat(dirToWrite.exists()).isTrue()
            return kgit.writeTree(directory = dirToWrite.absolutePath)
        }

        private fun writeDynamicTestStructure(): Oid {
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

            return kgit.writeTree(directory = dirToWrite.absolutePath)
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

    @Nested
    inner class Commits {

        @Test
        fun `should create a commit`() {
            val oid = kgit.commit(
                message = "Test commit",
                directory = STATIC_STRUCTURE
            )

            val content = objectDb.getObject(oid, expectedType = TYPE_COMMIT)
            val lines = content.split("\n")
            assertThat(lines).hasSize(3)

            assertAll {
                assertThat(lines[0]).contains("tree")
                assertThat(lines[1]).isEmpty()
                assertThat(lines[2]).isEqualTo("Test commit")
            }
        }
    }

}