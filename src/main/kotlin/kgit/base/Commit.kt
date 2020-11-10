package kgit.base

import kgit.data.Oid

data class Commit(val treeOid: Oid,
                  val parentOid: Oid? = null, // first commit has no parent
                  val message: String) {
    override fun toString(): String {
        val parentLine = parentOid?.let { "\nparent $parentOid" } ?: ""
        return """tree $treeOid$parentLine
            |
            |$message
            """.trimMargin()
    }
}