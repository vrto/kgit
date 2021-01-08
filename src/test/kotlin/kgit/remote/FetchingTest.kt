package kgit.remote

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import kgit.DYNAMIC_REMOTE_STRUCTURE
import kgit.DynamicStructureAware
import kgit.base.KGit
import kgit.createDynamicRemoteTestStructure
import kgit.data.ObjectDatabase
import kgit.diff.Diff
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path

class FetchingTest : DynamicStructureAware() {

    val remoteData = ObjectDatabase(DYNAMIC_REMOTE_STRUCTURE)
    val remoteKgit = KGit(remoteData, Diff(remoteData))

    val remote = Remote(data)

    @BeforeEach
    fun createDynamicRemoteStructure() {
        Path.of(DYNAMIC_REMOTE_STRUCTURE).toFile().deleteRecursively()
        createDynamicRemoteTestStructure()
        remoteData.init()
    }

    @Test
    fun `should fetch the single remote master branch`() {
        val first = remoteKgit.commit("first")
        remoteKgit.createBranch("master", first)

        val refs = remote.fetch(DYNAMIC_REMOTE_STRUCTURE)
        assertThat(refs).containsExactly("heads/master")
    }

    @Test
    fun `should fetch multiple remote branches and combine with local refs`() {
        val first = remoteKgit.commit("first")
        remoteKgit.createBranch("master", first)
        remoteKgit.createBranch("feature", first)
        remoteKgit.createBranch("testing-something", first)
        // non-branch refs are ignored
        remoteKgit.tag("ignored-tag", first)

        val firstLocal = kgit.commit("first local")
        kgit.createBranch("master", firstLocal)

        val refs = remote.fetch(DYNAMIC_REMOTE_STRUCTURE)
        assertThat(refs).containsOnly("heads/master", "heads/feature", "heads/testing-something")

        val localRefs = data.iterateRefs().map { it.name }
        assertThat(localRefs).containsAll(
            "HEAD", "heads/master", "remote/master", "remote/feature", "remote/testing-something")
    }
}