package kgit.base

import kgit.data.Oid

data class Commit(val treeOid: Oid,
                  val parentOids: List<Oid> = emptyList(), // first commit has no parent
                  val message: String) {
    override fun toString(): String {
        val parentLine = parentOids
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n", prefix = "\n") { "parent $it" }
            ?: ""
        return """tree $treeOid$parentLine
            |
            |$message
            """.trimMargin()
    }
}