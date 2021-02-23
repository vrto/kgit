package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kgit.base.KGit
import kgit.base.Tree
import kgit.cli.printDiffLines
import kgit.diff.Diff

class DiffCommand(private val kgit: KGit, private val diff: Diff)
    : CliktCommand(help = "Compare working tree to a commit") {

    private val oid: String? by argument(help = "Commit to diff").optional()
    private val cached: Boolean by option("--cached").flag()

    override fun run() {
        val treeFrom = when {
            oid != null -> getCommitTree(oid!!)
            cached -> getCommitTree("@")
            else -> kgit.getIndexTree()
        }

        val treeTo = when {
            cached -> kgit.getIndexTree()
            else -> kgit.getWorkingTree()
        }

        val diffLines = diff.diffTrees(treeFrom, treeTo)
        echo(diffLines.printDiffLines())
    }

    private fun getCommitTree(commitCandidate: String): List<Tree.FileState> {
        val commit = kgit.getOid(commitCandidate ?: "@").let {
            kgit.getCommit(it)
        }
        return kgit.getComparableTree(commit.treeOid)
    }
}