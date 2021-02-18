package kgit.remote

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import kgit.DYNAMIC_REMOTE_STRUCTURE
import kgit.DynamicRemoteStructureAware
import kgit.addFileToRemoteStructure
import kgit.base.KGit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FetchingTest : DynamicRemoteStructureAware() {

    @Test
    fun `should fetch the single remote master branch with no exta objects`() {
        remoteKgit.add(".")
        val first = remoteKgit.commit("first")
        remoteKgit.createBranch("master", first)

        val refs = remote.fetch(DYNAMIC_REMOTE_STRUCTURE)
        assertThat(refs).containsExactly("heads/master")
    }

    @Test
    fun `should fetch the single remote master branch with an extra object`() {
        remoteKgit.add(".")
        val first = remoteKgit.commit("first")
        remoteKgit.createBranch("master", first)

        remoteKgit.checkout("master")
        addFileToRemoteStructure("new_stuff")
        remoteKgit.add("new_stuff")
        val remoteCommit = remoteKgit.commit("new stuff added")

        kgit.add(".")
        val firstLocal = kgit.commit("first local")
        kgit.createBranch("master", firstLocal)

        // local kgit does not 'see' remote commit
        assertThrows<Exception> {
            kgit.getCommit(remoteCommit)
        }

        val refs = remote.fetch(DYNAMIC_REMOTE_STRUCTURE)
        assertThat(refs).containsExactly("heads/master")

        // local kgit should 'see' remote commit
        val fetchedCommit = kgit.getCommit(remoteCommit)
        assertThat(fetchedCommit.message).isEqualTo("new stuff added")
    }

    @Test
    fun `should fetch multiple remote branches and their objects, and combine with local refs and objects`() {
        remoteKgit.add(".")
        val first = remoteKgit.commit("first")
        remoteKgit.createBranch("master", first)

        // add content to one branch
        remoteKgit.createBranch("feature", first)
        remoteKgit.checkout("feature")
        addFileToRemoteStructure("feature_idea")
        remoteKgit.add("feature_idea")
        remoteKgit.commit("feature idea")

        // add content to another branch
        remoteKgit.createBranch("testing-something", first)
        remoteKgit.checkout("testing-something")
        addFileToRemoteStructure("testing_something")
        remoteKgit.add("testing_something")
        remoteKgit.commit("testing something")

        // non-branch refs are ignored
        remoteKgit.tag("ignored-tag", first)

        kgit.add(".")
        val firstLocal = kgit.commit("first local")
        kgit.createBranch("master", firstLocal)

        val refs = remote.fetch(DYNAMIC_REMOTE_STRUCTURE)
        assertThat(refs).containsOnly("heads/master", "heads/feature", "heads/testing-something")

        val localRefs = data.iterateRefs().map { it.name }
        assertThat(localRefs).containsAll(
            "HEAD", "heads/master", "remote/master", "remote/feature", "remote/testing-something")

        val allLocalObjects = kgit.parseAllBlobs(localRefs)
        assertThat(allLocalObjects).containsAll("feature_idea", "testing_something")
    }

    private fun KGit.parseAllBlobs(refs: List<String>): List<String> {
        val oids = refs.map(this::getOid)
        return listObjectsInCommits(oids).mapNotNull(data::tryParseBlob)
    }
}