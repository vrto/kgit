package kgit

import kgit.base.KGit
import kgit.data.ObjectDatabase
import kgit.diff.Diff
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Path

abstract class DynamicStructureAware {

    protected val data = ObjectDatabase(DYNAMIC_STRUCTURE)
    protected val kgit = KGit(data, Diff(data))

    @BeforeEach
    fun createKgitDir() {
        Path.of(DYNAMIC_STRUCTURE).toFile().deleteRecursively()
        createDynamicTestStructure()
        data.init()
    }

    @BeforeEach
    fun clearIndex() {
        data.getIndex().clear()
    }

}