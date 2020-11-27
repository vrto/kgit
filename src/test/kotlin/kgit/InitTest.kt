package kgit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kgit.base.KGit
import kgit.data.ObjectDatabase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path

class InitTest {

    private val objectDb = ObjectDatabase(DYNAMIC_STRUCTURE)
    private val kgit = KGit(objectDb)

    @BeforeEach
    fun setUpTestStructure() {
        Path.of(DYNAMIC_STRUCTURE).toFile().deleteRecursively()
        createDynamicTestStructure()
    }

    @Test
    fun `freshly initialized repository has an empty master branch`() {
        // detached head pointing nowhere
        assertThat(objectDb.getHead()?.oidOrNull).isNull()
        assertThat(objectDb.getHead(deref = false)?.value).isEqualTo("HEAD")

        kgit.init()

        // empty 'master' branch
        assertThat(objectDb.getHead()?.oidOrNull).isNull()
        assertThat(objectDb.getHead(deref = false)?.value).isEqualTo("ref: refs/heads/master")
    }
}
