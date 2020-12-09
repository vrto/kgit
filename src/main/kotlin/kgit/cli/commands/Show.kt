package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kgit.base.KGit
import kgit.cli.prettyPrint
import kgit.diff.Diff

class Show(private val diff: Diff, private val kgit: KGit) : CliktCommand(help = "Print commit message") {

    private val oid: String? by argument(help = "OID to show").optional()

    override fun run() {
        kgit.getOid(oid ?: "@").let {
            val commit = kgit.getCommit(it)
            commit.prettyPrint(it)

            if (commit.parentOid != null) {
                val parentTree = commit.parentOid.let(kgit::getCommit).treeOid
                val diffLines = diff.diffTrees(kgit.getTree(parentTree), kgit.getTree(commit.treeOid))
                echo(diffLines.joinToString(separator = "\n"))
            }
        }
    }
}