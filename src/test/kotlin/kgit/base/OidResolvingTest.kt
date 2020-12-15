package kgit.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import kgit.DynamicStructureAware
import kgit.data.Oid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.properties.Delegates

class OidResolvingTest : DynamicStructureAware() {

    private var oid: Oid by Delegates.notNull()

    @BeforeEach
    fun prepareTestCommit() {
        oid = kgit.commit("Test commit")
    }

    @Test
    fun `should resolve OID into an OID`() {
        val resolved = kgit.getOid(oid.value)
        assertThat(resolved).isEqualTo(oid)
    }

    @Test
    fun `should resolve ref into an OID`() {
        kgit.tag("tag-to-resolve", oid)

        val resolvedViaRoot = kgit.getOid("refs/tags/tag-to-resolve")
        val resolvedViaRefs = kgit.getOid("tags/tag-to-resolve")
        val resolvedViaTags = kgit.getOid("tag-to-resolve")

        assertThat(resolvedViaRoot).isEqualTo(oid)
        assertThat(resolvedViaRefs).isEqualTo(oid)
        assertThat(resolvedViaTags).isEqualTo(oid)
    }

    @Test
    fun `should crash if name can't be resolved into any OID`() {
        assertThrows<IllegalArgumentException> {
            kgit.getOid("bogus")
        }
    }

    @Test
    fun `should translate '@' to HEAD`() {
        assertThat(kgit.getOid("@")).isEqualTo(objectDb.getHead().oid)
    }
}