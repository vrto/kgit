package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kgit.base.Commit
import kgit.base.KGit
import kgit.data.Oid

class Log(private val kgit: KGit)
    : CliktCommand(help = "Walk the list of commits and print them") {

    private val start: String? by argument(help = "OID to start from").optional()

    override fun run() {
        val oid: Oid? = kgit.getOid(start ?: "@")
        val logStart = oid?.let { listOf(it) } ?: emptyList()
        kgit.listCommitsAndParents(logStart).forEach {
            val commit = kgit.getCommit(it)
            commit.prettyPrint(it)
        }
    }

    private fun Commit.prettyPrint(oid: Oid) {
        println("commit $oid")
        println("\t$message")
        println("")
    }
}