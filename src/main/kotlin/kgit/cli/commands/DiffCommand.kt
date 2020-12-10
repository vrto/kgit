package kgit.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kgit.base.KGit
import kgit.base.Tree
import kgit.cli.printDiffLines
import kgit.diff.Diff

class DiffCommand(private val kgit: KGit, private val diff: Diff)
    : CliktCommand(help = "Compare working tree to a commit") {

    private val oid: String? by argument(help = "Commit to diff").optional()

    override fun run() {
        val workingTree = kgit.getWorkingTree()
        val commitTree = getCommitTree()
        val diffLines = diff.diffTrees(commitTree, workingTree)
        echo(diffLines.printDiffLines())
    }

    private fun getCommitTree(): List<Tree.FileState> {
        val commit = kgit.getOid(oid ?: "@").let {
            kgit.getCommit(it)
        }
        return kgit.getComparableTree(commit.treeOid)
    }
}