package kgit.base

data class Commit(val treeOid: String, val message: String) {
    override fun toString() =
        """tree $treeOid
        |
        |$message
        """.trimMargin()
}