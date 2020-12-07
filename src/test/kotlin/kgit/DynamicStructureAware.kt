package kgit

import kgit.base.KGit
import kgit.data.ObjectDatabase
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Path

abstract class DynamicStructureAware {

    protected val objectDb = ObjectDatabase(DYNAMIC_STRUCTURE)
    protected val kgit = KGit(objectDb)

    @BeforeEach
    fun createKgitDir() {
        Path.of(DYNAMIC_STRUCTURE).toFile().deleteRecursively()
        createDynamicTestStructure()
        objectDb.init()
    }

}