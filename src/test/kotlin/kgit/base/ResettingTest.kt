package kgit.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import kgit.DynamicStructureAware
import org.junit.jupiter.api.Test

class ResettingTest : DynamicStructureAware() {

    @Test
    fun `reset should move HEAD to an OID`() {
        val oid1 = kgit.commit("First commit")
        val oid2 = kgit.commit("Second commit")
        assertThat(objectDb.getHead().oidValue).isEqualTo(oid2.value)

        kgit.reset(oid1)

        assertThat(objectDb.getHead().oidValue).isEqualTo(oid1.value)
    }

}