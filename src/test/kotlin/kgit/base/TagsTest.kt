package kgit.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import kgit.DynamicStructureAware
import org.junit.jupiter.api.Test

class TagsTest : DynamicStructureAware() {

    @Test
    fun `should create a tag`() {
        val oid = kgit.commit("Test commit")

        kgit.tag("test-tag", oid)

        val ref = data.getRef("refs/tags/test-tag")
        assertThat(ref.oid).isEqualTo(oid)
    }

    @Test
    fun `should create a tag with slashes`() {
        val oid = kgit.commit("Test commit")

        kgit.tag("nested/tag", oid)

        val ref = data.getRef("refs/tags/nested/tag")
        assertThat(ref.oid).isEqualTo(oid)
    }
}