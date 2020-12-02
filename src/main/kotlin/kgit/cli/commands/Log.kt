package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kgit.base.Commit
import kgit.base.KGit
import kgit.data.ObjectDatabase
import kgit.data.Oid

class Log(private val objectDb: ObjectDatabase, private val kgit: KGit)
    : CliktCommand(help = "Walk the list of commits and print them") {

    private val start: String? by argument(help = "OID to start from").optional()

    override fun run() {
        val refs = reverseOidLookup()
        val oid: Oid? = kgit.getOid(start ?: "@")
        val logStart = oid?.let { listOf(it) } ?: emptyList()

        kgit.listCommitsAndParents(logStart).forEach {
            val commit = kgit.getCommit(it)
            commit.prettyPrint(it, refs)
        }
    }

    private fun reverseOidLookup(): MutableMap<String, MutableList<String>> {
        val refs = mutableMapOf<String, MutableList<String>>()
        objectDb.iterateRefs().forEach {
            refs.getOrPut(it.ref.value, ::mutableListOf).add(it.name)
        }
        return refs
    }

    private fun Commit.prettyPrint(oid: Oid, refs: Map<String, MutableList<String>>) {
        val refsOfThisCommit = refs.getValue(oid.value).joinToString(prefix = "(", separator = ", ", postfix = ")")
        println("commit $oid $refsOfThisCommit")
        println("\t$message")
        println("")
    }
}