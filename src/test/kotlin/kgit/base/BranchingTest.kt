package kgit.base

import assertk.assertThat
import assertk.assertions.*
import kgit.DynamicStructureAware
import kgit.data.Oid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates

class BranchingTest : DynamicStructureAware() {

    private var oid: Oid by Delegates.notNull()

    @BeforeEach
    fun createFirstCommit() {
        kgit.add(".")
        oid = kgit.commit("First commit")
    }

    @Test
    fun `should create a new branch`() {
        assertThat(data.getRef("refs/heads/test-branch").oidOrNull).isNull()
        kgit.createBranch("test-branch", oid)
        assertThat(data.getRef("refs/heads/test-branch").oid).isEqualTo(oid)
    }

    @Test
    fun `should return branch name`() {
        assertThat(kgit.getBranchName()).isNull()

        kgit.createBranch("test-branch", oid)
        kgit.checkout("test-branch")

        assertThat(kgit.getBranchName()).isEqualTo("test-branch")
    }

    @Test
    fun `should list branches`() {
        assertThat(kgit.listBranches()).isEmpty()

        kgit.createBranch("test-branch", oid)
        assertThat(kgit.listBranches()).containsExactly("test-branch")

        kgit.createBranch("test-branch2", oid)
        assertThat(kgit.listBranches()).containsOnly("test-branch", "test-branch2")
    }
}