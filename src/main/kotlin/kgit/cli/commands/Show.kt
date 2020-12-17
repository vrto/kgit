package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kgit.base.Commit
import kgit.base.KGit
import kgit.cli.prettyPrint
import kgit.cli.printDiffLines
import kgit.data.Oid
import kgit.diff.Diff

class Show(private val kgit: KGit, private val diff: Diff)
    : CliktCommand(help = "Compare current working tree with the specified commit") {

    private val oid: String? by argument(help = "OID to show").optional()

    override fun run() {
        kgit.getOid(oid ?: "@").let {
            val commit = kgit.getCommit(it)
            commit.prettyPrint(it)
            commit.parentOids.forEach { parent -> commit.showParent(parent) }
        }
    }

    private fun Commit.showParent(parent: Oid) {
        val parentTree = parent.let(kgit::getCommit).treeOid
        val diffLines = diff.diffTrees(
            orig = kgit.getComparableTree(parentTree),
            changed = kgit.getComparableTree(treeOid)
        )
        echo(diffLines.printDiffLines())
    }
}