package kgit.base

import kgit.data.Oid

data class Commit(val treeOid: Oid, val message: String) {
    override fun toString() =
        """tree $treeOid
        |
        |$message
        """.trimMargin()
}