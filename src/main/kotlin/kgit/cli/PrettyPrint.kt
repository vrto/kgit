package kgit.cli

import kgit.base.Commit
import kgit.data.Oid

fun Commit.prettyPrint(oid: Oid, refs: Map<String, MutableList<String>> = emptyMap()) {
    val refsOfThisCommit = refs[oid.value]?.joinToString(prefix = "(", separator = ", ", postfix = ")") ?: ""
    println("commit $oid $refsOfThisCommit")
    println("\t$message")
    println("")
}

fun List<String>.printDiffLines() = this.joinToString(separator = "\n")
