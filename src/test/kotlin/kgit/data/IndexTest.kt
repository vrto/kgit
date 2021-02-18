package kgit.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import kgit.DYNAMIC_STRUCTURE
import kgit.DynamicStructureAware
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class IndexTest : DynamicStructureAware() {

    val INDEX_PATH = "$DYNAMIC_STRUCTURE/$KGIT_DIR/index"

    @Test
    fun `inflating flat index shouldn't do anything`() {
        val oid = nextOid()
        saveIndex("""
            {
                "flat": "$oid"
            }
        """.trimIndent())

        val inflated = Index(INDEX_PATH).inflate()
        assertThat(inflated).isEqualTo(mapOf("flat" to oid))
    }

    @Test
    fun `should inflate single-level index`() {
        val (oid1, oid2) = listOf(nextOid(), nextOid())
        saveIndex("""
            {
                "flat": "$oid1",
                "subdir/nested": "$oid2"
            }
        """.trimIndent())

        val inflated = Index(INDEX_PATH).inflate()
        assertThat(inflated).isEqualTo(mapOf(
            "flat" to oid1,
            "subdir" to mapOf("nested" to oid2)
        ))
    }

    @Test
    fun `should inflate multi-level index`() {
        val (oid1, oid2, oid3, oid4) = listOf(nextOid(), nextOid(), nextOid(), nextOid())
        saveIndex("""
            {
                "flat": "$oid1",
                "subdir/nested": "$oid2",
                "subdir2/nested2": "$oid3",
                "subdir2/subdir3/nested3": "$oid4"
            }
        """.trimIndent())

        val inflated = Index(INDEX_PATH).inflate()
        assertThat(inflated).isEqualTo(mapOf(
            "flat" to oid1,
            "subdir" to mapOf("nested" to oid2),
            "subdir2" to mapOf("nested2" to oid3,
                               "subdir3" to mapOf("nested3" to oid4))
        ))
    }

    private fun nextOid() = data.hashObject(UUID.randomUUID().toString().toByteArray(), TYPE_BLOB)

    private fun saveIndex(json: String) = File(INDEX_PATH).writeText(json)
}
