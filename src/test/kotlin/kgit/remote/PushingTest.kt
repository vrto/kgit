package kgit.remote

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import io.mockk.spyk
import io.mockk.verify
import kgit.DYNAMIC_REMOTE_STRUCTURE
import kgit.DynamicRemoteStructureAware
import kgit.addFileToLocalStructure
import kgit.addFileToRemoteStructure
import kgit.base.KGit
import kgit.data.ObjectDatabase
import kgit.remote.PushResult.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PushingTest : DynamicRemoteStructureAware() {

    val observableData = spyk(data)
    override fun setData() = observableData

    @Test
    fun `should reject a push on non-existing ref`() {
        val result = remote.push(DYNAMIC_REMOTE_STRUCTURE, "bogus")
        assertThat(result).isEqualTo(UNKNOWN_REF)
    }

    @Test
    fun `should push the single local master branch with no extra objects`() {
        kgit.add(".")
        val first = kgit.commit("first")
        kgit.createBranch("master", first)

        assertThat(remoteData.listRefs()).doesNotContain("heads/master")

        val result = remote.push(DYNAMIC_REMOTE_STRUCTURE, "refs/heads/master")

        assertThat(result).isEqualTo(OK)
        assertThat(remoteData.listRefs()).contains("heads/master")
    }

    @Test
    fun `should push the single local master branch with an extra object`() {
        kgit.add(".")
        val first = kgit.commit("first")
        kgit.createBranch("master", first)

        kgit.checkout("master")
        addFileToLocalStructure("new_stuff")
        kgit.add("new_stuff")
        val localCommit = kgit.commit("new stuff added")

        // copy the same initial commit
        remoteKgit.add(".")
        val firstRemote = remoteKgit.commit("first")
        remoteKgit.createBranch("master", firstRemote)

        // remote kgit does not 'see' local commit
        assertThrows<Exception> {
            remoteKgit.getCommit(localCommit)
        }

        val result = remote.push(DYNAMIC_REMOTE_STRUCTURE, "refs/heads/master")
        assertThat(result).isEqualTo(OK)

        // remote kgit should 'see' local commit
        val fetchedCommit = remoteKgit.getCommit(localCommit)
        assertThat(fetchedCommit.message).isEqualTo("new stuff added")

        val allRemoteObjects = remoteKgit.parseAllBlobs(remoteData.listRefs())
        assertThat(allRemoteObjects).containsAll("new_stuff")
    }

    @Test
    fun `should push changes to the remote branch`() {
        kgit.add(".")
        val first = kgit.commit("first")
        kgit.createBranch("master", first)

        // add content to one branch
        kgit.createBranch("feature", first)
        kgit.checkout("feature")
        addFileToLocalStructure("feature_idea")
        kgit.add("feature_idea")
        kgit.commit("feature idea")

        remoteKgit.add(".")
        val firstRemote = remoteKgit.commit("first")
        remoteKgit.createBranch("master", firstRemote)

        // sync master
        val result = remote.push(DYNAMIC_REMOTE_STRUCTURE, "refs/heads/master")
        assertThat(result).isEqualTo(OK)
        assertThat(remoteData.listRefs()).containsAll("HEAD", "heads/master")

        // push the feature branch
        remote.push(DYNAMIC_REMOTE_STRUCTURE, "refs/heads/feature")

        val remoteRefs = remoteData.listRefs()
        assertThat(remoteData.listRefs()).containsAll("HEAD", "heads/master", "heads/feature")

        val allRemoteObjects = remoteKgit.parseAllBlobs(remoteRefs)
        assertThat(allRemoteObjects).containsAll("feature_idea")
    }

    @Test
    fun `should push changes to the remote efficiently`() {
        kgit.add(".")
        val first = kgit.commit("first")
        kgit.createBranch("master", first)

        kgit.checkout("master")
        addFileToLocalStructure("new_file")
        kgit.add("new_file")
        kgit.commit("add new_file")

        remoteKgit.add(".")
        val firstRemote = remoteKgit.commit("first")
        remoteKgit.createBranch("master", firstRemote)

        val result = remote.push(DYNAMIC_REMOTE_STRUCTURE, "refs/heads/master")
        assertThat(result).isEqualTo(OK)
        verify(exactly = 3) { // new_file commit, tree for the commit, new_file content
            observableData.pushObject(any(), any())
        }

        val allRemoteObjects = remoteKgit.parseAllBlobs(remoteData.listRefs())
        assertThat(allRemoteObjects).containsAll("new_file")
    }

    @Test
    fun `should reject force push`() {
        kgit.add(".")
        val first = kgit.commit("first")
        kgit.createBranch("master", first)

        kgit.checkout("master")
        addFileToLocalStructure("new_file")
        kgit.add("new_file")
        kgit.commit("add new_file")

        remoteKgit.add(".")
        val firstRemote = remoteKgit.commit("first")
        remoteKgit.createBranch("master", firstRemote)

        remoteKgit.checkout("master")
        addFileToRemoteStructure("another_file")
        remoteKgit.add("another_file")
        remoteKgit.commit("add another_file")

        val result = remote.push(DYNAMIC_REMOTE_STRUCTURE, "refs/heads/master")
        assertThat(result).isEqualTo(FORCE_PUSH_REJECTED)
    }

    private fun ObjectDatabase.listRefs() = iterateRefs().map { it.name }

    private fun KGit.parseAllBlobs(refs: List<String>): List<String> {
        val oids = refs.map(this::getOid)
        return listObjectsInCommits(oids).mapNotNull(remoteData::tryParseBlob)
    }

}