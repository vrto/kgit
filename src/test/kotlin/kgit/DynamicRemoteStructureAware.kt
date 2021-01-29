package kgit

import kgit.base.KGit
import kgit.data.ObjectDatabase
import kgit.diff.Diff
import kgit.remote.Remote
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Path

abstract class DynamicRemoteStructureAware : DynamicStructureAware() {

    protected val remoteData = ObjectDatabase(DYNAMIC_REMOTE_STRUCTURE)
    protected val remoteKgit = KGit(remoteData, Diff(remoteData))

    protected val remote = Remote(data, kgit)

    @BeforeEach
    fun createDynamicRemoteStructure() {
        Path.of(DYNAMIC_REMOTE_STRUCTURE).toFile().deleteRecursively()
        createDynamicRemoteTestStructure()
        remoteData.init()
    }
}