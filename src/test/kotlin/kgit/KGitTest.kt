package kgit

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class KGitTest {

    @BeforeEach
    internal fun setUp() {
        Files.deleteIfExists(Path.of(KGIT_DIR))
    }

    @Test
    fun `init creates a new directory`() {
        assertThat(Files.exists(Path.of(KGIT_DIR))).isFalse()

        Data.init()

        assertThat(Files.exists(Path.of(KGIT_DIR))).isTrue()
    }
}