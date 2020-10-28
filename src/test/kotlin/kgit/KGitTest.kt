package kgit

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class KGitTest {

    @BeforeEach
    internal fun setUp() {
        Path.of(KGIT_DIR).toFile().deleteRecursively()
    }

    @Test
    fun `should write tree`() {
        Data.init()

        val dirToWrite = File("src/test/resources/test-structure")
        assertThat(dirToWrite.exists()).isTrue()
        val treeOid = Base.writeTree(directory = dirToWrite.absolutePath)

        val content = Data.getObject(treeOid, TYPE_TREE)
        val lines = content.split("\n")
        assertThat(lines).hasSize(3)

        assertThat(lines[0]).contains("blob")
        assertThat(lines[0]).contains("cats.txt")
        assertThat(lines[1]).contains("tree")
        assertThat(lines[1]).contains("other")
        assertThat(lines[2]).contains("blob")
        assertThat(lines[2]).contains("dogs.txt")
    }
}