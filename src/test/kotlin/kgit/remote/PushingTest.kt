package kgit.remote

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import kgit.DYNAMIC_REMOTE_STRUCTURE
import kgit.DynamicRemoteStructureAware
import kgit.addFileToLocalStructure
import kgit.base.KGit
import kgit.data.ObjectDatabase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PushingTest : DynamicRemoteStructureAware() {

    @Test
    fun `should throw an error on pushing a non-existent branch`() {
        assertThrows<IllegalArgumentException> {
            remote.push(DYNAMIC_REMOTE_STRUCTURE, "bogus")
        }
    }

    @Test
    fun `should push the single local master branch with no extra objects`() {
        val first = kgit.commit("first")
        kgit.createBranch("master", first)

        assertThat(remoteData.listRefs()).doesNotContain("master")
        remote.push(DYNAMIC_REMOTE_STRUCTURE, "refs/heads/master")
        assertThat(remoteData.listRefs()).contains("heads/master")
    }

    @Test
    fun `should push the single local master branch with an extra object`() {
        val first = kgit.commit("first")
        kgit.createBranch("master", first)

        kgit.checkout("master")
        addFileToLocalStructure("new_stuff")
        val localCommit = kgit.commit("new stuff added")

        val firstRemote = remoteKgit.commit("first remote")
        remoteKgit.createBranch("master", firstRemote)

        // remote kgit does not 'see' local commit
        assertThrows<Exception> {
            remoteKgit.getCommit(localCommit)
        }

        remote.push(DYNAMIC_REMOTE_STRUCTURE, "refs/heads/master")

        // remote kgit should 'see' local commit
        val fetchedCommit = remoteKgit.getCommit(localCommit)
        assertThat(fetchedCommit.message).isEqualTo("new stuff added")
    }

    @Test
    fun `should push changes to the remote branch`() {
        val first = kgit.commit("first")
        kgit.createBranch("master", first)

        // add content to one branch
        kgit.createBranch("feature", first)
        kgit.checkout("feature")
        addFileToLocalStructure("feature_idea")
        kgit.commit("feature idea")

        val firstRemote = remoteKgit.commit("first remote")
        remoteKgit.createBranch("master", firstRemote)

        // sync master
        remote.push(DYNAMIC_REMOTE_STRUCTURE, "refs/heads/master")
        assertThat(remoteData.listRefs()).containsAll("HEAD", "heads/master")

        // push the feature branch
        remote.push(DYNAMIC_REMOTE_STRUCTURE, "refs/heads/feature")

        val remoteRefs = remoteData.listRefs()
        assertThat(remoteData.listRefs()).containsAll("HEAD", "heads/master", "heads/feature")

        val allRemoteObjects = remoteKgit.parseAllBlobs(remoteRefs)
        assertThat(allRemoteObjects).containsAll("feature_idea")
    }

    private fun ObjectDatabase.listRefs() = iterateRefs().map { it.name }

    private fun KGit.parseAllBlobs(refs: List<String>): List<String> {
        val oids = refs.map(this::getOid)
        return listObjectsInCommits(oids).mapNotNull(remoteData::tryParseBlob)
    }
}